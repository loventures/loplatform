/*
 * LO Platform copyright (C) 2007–2025 LO Ventures LLC.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package loi.cp.reverseproxy;

import com.google.common.base.Charsets;
import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.component.annotation.PostLoad;
import com.learningobjects.cpxp.component.annotation.PreUnload;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.service.site.SiteFacade;
import com.learningobjects.cpxp.util.ClientConfig;
import com.learningobjects.cpxp.util.HttpUtils;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.cpxp.util.ThreadFactoryBuilder;
import com.typesafe.config.Config;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.util.EntityUtils;
import scala.collection.Iterator;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@Service(unique = true)
public class ReverseProxyPool {
    private static final Logger logger = Logger.getLogger(ReverseProxyPool.class.getName());
    private static final int MAX_REVERSE_PROXY_THREADS = 8; // Number of threads to process reverse proxy requests
    private static final int MAX_REVERSE_PROXY_QUEUE_SIZE = 32; // Max number of reverse proxy requests to queue (on top of running requests)

    private static LinkedBlockingQueue<Runnable> __queue;
    private static ThreadPoolExecutor __executor;

    @Inject
    private Config config;

    @PostLoad
    private static void initReverseProxyPool() {
        ThreadFactory threads = new ThreadFactoryBuilder()
            .threadGroup(new ThreadGroup("ReverseProxy"))
            .finishConfig();
        __queue = new LinkedBlockingQueue<>();
        __executor = new ThreadPoolExecutor(0, MAX_REVERSE_PROXY_THREADS, 60L, TimeUnit.SECONDS, __queue, threads);
    }

    @PreUnload
    private static void shutdownReverseProxyPool() {
        __executor.shutdownNow();
    }

    public void doProxy(SiteFacade site, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.reset();

        // TODO: Mo betta HEAD

        if (Boolean.TRUE.equals(site.getDisabled())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } else if (__queue.size() >= MAX_REVERSE_PROXY_QUEUE_SIZE) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }

        final ReverseProxyConfiguration configuration = site.getJson(ReverseProxyConfiguration.class);

        final String cookiePrefix = "proxy" + site.getId().toString();

        final String requestUri = (request.isSecure() ? "https://" : "http://") + request.getServerName() + request.getRequestURI();
        String queryString = request.getQueryString();
        final String url = configuration.remoteUrl() +
            request.getRequestURI().substring(site.getUrl().length()) + (queryString != null ? "?" + queryString : "");
        final AsyncContext context = request.startAsync();

        final URI siteURI = new URI(site.getUrl());

        // attach listener to respond to lifecycle events of this AsyncContext
        /* context.setListener(new AsyncListener() {
                 @Override public void onComplete(AsyncEvent event) throws IOException {
                 }
                 @Override public void onTimeout(AsyncEvent event) throws IOException {
                 }
                 @Override public void onError(AsyncEvent event) throws IOException {
                 }
                 @Override public void onStartAsync(AsyncEvent event) throws IOException {
                 }
             }); */

        // TODO: I /should/ add a listener so that if a timeout occurs,
        // then when my Runnable is actually run it just gives up. As it
        // stands if an async request is timed out then when it runs the
        // getRequest fails with IllegalStateException.
        context.setTimeout(TimeUnit.SECONDS.toMillis(60));
        __executor.submit(() -> {
            try {
                proxy(requestUri, url, cookiePrefix, siteURI, configuration,
                        (HttpServletRequest) context.getRequest(), (HttpServletResponse) context.getResponse());
            } catch (IllegalStateException ignored) {
                logger.log(Level.INFO, "Proxy request probably timed out");
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Proxy error", ex);
            } finally {
                context.complete();
            }
        });
    }

    private static final String[] REQUEST_HEADERS = {
        "Accept", "Accept-Charset", "Accept-Language", "If-None-Match", "If-Modified-Since", "Referer", "User-Agent"
    }; // "Accept-Encoding"

    private static final String[] RESPONSE_HEADERS = {
        "Content-Type", "Content-Language", "Content-Length", "Vary", "Expires", "ETag", "Date", "Last-Modified"
    }; // httpclient decodes the "Content-Encoding" and "Transfer-Encoding"

    // Mostly copied from CWO servlet...
    private void proxy(String requestUri, String url, String cookiePrefix, URI siteURI, ReverseProxyConfiguration configuration,
                       HttpServletRequest request, HttpServletResponse response) throws Exception {
        long then = System.currentTimeMillis();
        AbstractHttpClient client = getHttpClient();

        jakarta.servlet.http.Cookie[] requestCookies = request.getCookies();
        CookieStore cookieStore = deproxyCookies(
          (requestCookies == null)
            ? Collections.emptyList()
            : Arrays.asList(requestCookies),cookiePrefix,siteURI.getPath());

        logger.log(Level.WARNING, "URL {0}", url);
        HttpGet get = new HttpGet(url);
        for (String name : REQUEST_HEADERS) {
            for (String header : Collections.list(request.getHeaders(name))) {
                get.addHeader(name, header);
            }
        }

        // HttpClient doesn't seem to be using the cookie store for some reason, manually set cookie headers for now.
        if (!cookieStore.getCookies().isEmpty()) {
            for (Header cookieHeader : new BrowserCompatSpec().formatCookies(cookieStore.getCookies())) {
                get.addHeader(cookieHeader);
            }
        }
        get.addHeader(HttpUtils.HTTP_HEADER_X_FORWARDED_FOR, HttpUtils.getRemoteAddr(request, BaseServiceMeta.getServiceMeta()));

        HttpResponse rsp = client.execute(get);

        List<RewriteRule> rules = new ArrayList<>();
        for (Iterator<RewriteRule> i = configuration.rewriteRules().iterator(); i.hasNext();) {
            RewriteRule rule = i.next();
            if (Pattern.compile(rule.pathPattern()).matcher(url).matches()) {
                rules.add(rule);
            }
        }

        for (String name : RESPONSE_HEADERS) {
            // As long as we're a non-caching proxy, if rewrites occur then we have to block the content-length header
            if (!"Content-Length".equals(name) || rules.isEmpty()) {
                for (Header header : rsp.getHeaders(name)) {
                    response.addHeader(name, header.getValue());
                }
            }
        }

        List<String> cookieHeaders = new ArrayList<>();
        for(Header cookieHeader : rsp.getHeaders("Set-Cookie")) {
            cookieHeaders.add(cookieHeader.getValue());
        }
        List<Cookie> responseCookies = HttpUtils.parseCookies(get.getURI(),cookieHeaders,new BrowserCompatSpec());
        List<Cookie> proxiedCookies = flatMapPrefix(partitionCookies(responseCookies, configuration.cookieNames()), cookiePrefix);

        for(Cookie proxiedCookie : proxiedCookies) {
            response.addCookie(toJavaxCookie(proxiedCookie));
        }

        response.addHeader("Via", "0.9 Difference Engine");
        HttpEntity entity = rsp.getEntity();
        int statusCode = rsp.getStatusLine().getStatusCode();
        logger.log(Level.WARNING, "Status " + rsp.getStatusLine());
        try (OutputStream oos = response.getOutputStream()) {
            response.setStatus(statusCode);
            switch (statusCode) {
              case HttpServletResponse.SC_OK:
              case HttpServletResponse.SC_NOT_MODIFIED:
              case HttpServletResponse.SC_NOT_FOUND:
                  break;
              case HttpServletResponse.SC_MOVED_PERMANENTLY:
              case HttpServletResponse.SC_MOVED_TEMPORARILY:
                  Header location = rsp.getFirstHeader(HttpUtils.HTTP_HEADER_LOCATION);
                  response.addHeader(HttpUtils.HTTP_HEADER_LOCATION, adjustRedirect(siteURI.toString(), configuration.remoteUrl(), location.getValue()));
                  break;
              default:
                  logger.log(Level.WARNING, "Proxy error" + statusCode);
                  break;
            }
            response.setStatus(statusCode);
            if (entity == null) {
                // nada
            } else if (rules.isEmpty()) {
                IOUtils.copy(entity.getContent(), oos);
            } else {
                List<CompiledRewrite> rewrites = new ArrayList<>();
                for (RewriteRule rule : rules) {
                    rewrites.add(CompiledRewrite.apply(rule));
                }
                Header contentTypeHeader = entity.getContentType();
                ContentType contentType = (contentTypeHeader == null) ? null : ContentType.parse(contentTypeHeader.getValue());
                Charset charset = (contentType == null) ? null : contentType.getCharset();
                charset = (charset == null) ? Charsets.UTF_8 : charset;
                // InputStreamReader does buffer the input stream, but buffering the ISR appears to provide
                // substantial gains for by-character reading operations.
                BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent(), charset));
                OutputStreamWriter out = new OutputStreamWriter(oos, charset);
                String line;
                while ((line = in.readLine()) != null) { // TODO: I should preserve linefeed
                    for (CompiledRewrite rewrite : rewrites) {
                        line = rewrite.bodyRegex().replaceAllIn(line, rewrite.replacementText());
                    }
                    out.write(line.concat("\n"));
                }
                out.flush();
            }
        } finally {
            EntityUtils.consumeQuietly(entity);
        }
        long now = System.currentTimeMillis();
        logger.log(Level.INFO, "Proxied " + requestUri + " in " + (now - then) + " ms, status code " + statusCode);
    }

    private CookiesTuple partitionCookies(List<Cookie> cookies, scala.collection.immutable.List<String> cookieNames) {
        List<Cookie> proxiedCookies = new ArrayList<>();
        List<Cookie> otherCookies = new ArrayList<>();
        for(Cookie cookie : cookies) {
            if(cookieNames.contains(cookie.getName())) {
                otherCookies.add(cookie);
            } else {
                proxiedCookies.add(cookie);
            }
        }
        return new CookiesTuple(proxiedCookies,otherCookies);
    }

    private List<Cookie> flatMapPrefix(CookiesTuple cookies, String prefix) {
        List<Cookie> resultList = new ArrayList<>();
        for(Cookie cookie : cookies.getProxiedCookies()) {
            Cookie prefixedCookie = new BasicClientCookie(prefix + cookie.getName(),cookie.getValue());
            resultList.add(prefixedCookie);
        }
        for(Cookie cookie : cookies.getCookies()){
            resultList.add(cookie);
        }
        return resultList;
    }

    private jakarta.servlet.http.Cookie toJavaxCookie(Cookie clientCookie) {
        String name = clientCookie.getName();
        String value = clientCookie.getValue();

        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(name, value);

        value = clientCookie.getDomain();
        if(value != null) {
            cookie.setDomain(value);
        }

        value = clientCookie.getPath();
        if(value != null) {
            cookie.setPath(value);
        }

        cookie.setHttpOnly(true);
        return cookie;
    }

    private Cookie toApacheCookie(jakarta.servlet.http.Cookie cookie, Optional<String> cookiePrefix, Optional<String> path) {
        //TODO: mapping cookie prefix and path should be a separate concern
        String newCookieName = cookie.getName();
        if(cookiePrefix.isPresent()) {
            newCookieName = cookie.getName().replace(cookiePrefix.get(),"");
        }
        BasicClientCookie clientCookie = new BasicClientCookie(newCookieName,cookie.getValue());
        String value = cookie.getDomain();
        if(value != null) {
            clientCookie.setDomain(value);
        }

        path.ifPresent(clientCookie::setPath);

        value = cookie.getPath();
        if(value != null && !path.isPresent()) {
            clientCookie.setPath(value);
        }

        return clientCookie;
    }

    private CookieStore deproxyCookies(List<jakarta.servlet.http.Cookie> cookies, String cookiePrefix, String path) {
        CookieStore cookieStore = new BasicCookieStore();
        for(jakarta.servlet.http.Cookie servletCookie : cookies) {
                if (StringUtils.startsWith(servletCookie.getName(), cookiePrefix)) {
                    Cookie deproxiedCookie = toApacheCookie(servletCookie, Optional.of(cookiePrefix), Optional.of(path));
                    cookieStore.addCookie(deproxiedCookie);
                } else {
                    Cookie apacheCookie = toApacheCookie(servletCookie, Optional.empty(), Optional.of(path));
                    cookieStore.addCookie(apacheCookie);
                }
        }
        return cookieStore;
    }

    private static class CookiesTuple {

        private final List<Cookie> _proxiedCookies;
        private final List<Cookie> _cookies;

        public CookiesTuple(List<Cookie> proxiedCookies, List<Cookie> cookies) {
            _proxiedCookies = proxiedCookies;
            _cookies = cookies;
        }

        public List<Cookie> getProxiedCookies() {
            return _proxiedCookies;
        }

        public List<Cookie> getCookies() {
            return _cookies;
        }
    }

    private String adjustRedirect(String siteUrl, String remoteUrl, String to) {
        // TODO: Handle a relative redirect too..
        if (!to.startsWith(remoteUrl)) {
            return to;
        }
        return siteUrl + to.substring(remoteUrl.length());
    }

    private static DefaultHttpClient __httpClient;

    private synchronized AbstractHttpClient getHttpClient() {

        if (__httpClient == null) {
            __httpClient = HttpUtils.HttpClientBuilder
              .newMonitoredClient("ReverseProxy")
              .setLimit(MAX_REVERSE_PROXY_THREADS)
              .build(ClientConfig.fromConfig(config));
        }
        return __httpClient;
    }
}

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

package com.learningobjects.cpxp.util;

import com.learningobjects.cpxp.ServiceMeta;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.NullWriter;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpMessage;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SM;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.pool.PoolStats;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * HTTP utils.
 */
public class HttpUtils {
    public static final String HTTP_REQUEST_METHOD_POST = "POST";
    public static final String HTTP_REQUEST_METHOD_PUT = "PUT";
    public static final String HTTP_REQUEST_METHOD_HEAD = "HEAD";
    public static final String HTTP_REQUEST_METHOD_GET = "GET";
    public static final String HTTP_REQUEST_METHOD_DELETE = "DELETE";
    public static final String HTTP_HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";
    public static final String HTTP_HEADER_LAST_MODIFIED = "Last-Modified";
    public static final String HTTP_HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String HTTP_HEADER_CONTENT_ENCODING = "Content-Encoding";
    public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HTTP_HEADER_CONTENT_LENGTH = "Content-Length";
    public static final String HTTP_HEADER_EXPIRES = "Expires";
    public static final String HTTP_HEADER_ALLOW = "Allow";
    public static final String HTTP_HEADER_ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
    public static final String HTTP_HEADER_CONNECTION = "Connection";
    public static final String HTTP_HEADER_PRAGMA = "Pragma";
    public static final String HTTP_HEADER_REFERER = "Referer";
    public static final String HTTP_HEADER_HOST = "Host";
    public static final String HTTP_HEADER_CACHE_CONTROL = "Cache-Control";
    public static final String HTTP_HEADER_USER_AGENT = "User-Agent";
    public static final String HTTP_HEADER_VARY = "Vary";
    public static final String HTTP_HEADER_LOCATION = "Location";
    public static final String HTTP_HEADER_SET_COOKIE = "Set-Cookie";
    public static final String HTTP_HEADER_ACCEPT = "Accept";
    public static final String HTTP_HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    public static final String HTTP_HEADER_ACCEPT_LANGUAGE = "Accept-Language";
    public static final String HTTP_HEADER_WWW_AUTH = "WWW-Authenticate";
    public static final String HTTP_HEADER_AUTHORIZATION = "Authorization";
    public static final String HTTP_HEADER_X_REQUESTED_WITH = "X-Requested-With";
    public static final String HTTP_HEADER_X_REMOTE_ADDRESS = "X-Remote-Addr"; // Internally used in tests to "spoof" an IP
    public static final String HTTP_HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String HTTP_BASIC_AUTH_HEADER_NAME = "Basic";
    public static final String HTTP_DIGEST_AUTH_HEADER_NAME = "Digest";
    public static final String HTTP_AUTH_HEADER_ATTR_REALM = "realm";
    public static final String HTTP_DIGEST_AUTH_HEADER_ATTR_RESPONSE = "response";
    public static final String HTTP_DIGEST_AUTH_HEADER_ATTR_NONCE = "nonce";
    public static final String HTTP_DIGEST_AUTH_HEADER_ATTR_OPAQUE = "opaque";
    public static final String HTTP_DIGEST_AUTH_HEADER_ATTR_URI = "uri";
    public static final String HTTP_DIGEST_AUTH_HEADER_ATTR_USERNAME = "username";
    public static final String AUTHORIZATION_BEARER = "Bearer";
    public static final String PRAGMA_NO_CACHE = "no-cache";
    public static final String CACHE_CONTROL_REALLY_DONT_CACHE = "no-store, no-cache, must-revalidate";
    public static final String CONNECTION_CLOSE = "close";
    public static final String ENCODING_GZIP = "gzip";
    public static final String ENCODING_X_GZIP = "x-gzip";
    public static final String WITH_XML_HTTP_REQUEST = "XMLHttpRequest";
    public static final int HTTP_PORT = 80;
    public static final int HTTPS_PORT = 443;
    public static final String DISPOSITION_INLINE = "inline";
    public static final String DISPOSITION_ATTACHMENT = "attachment";

    private static final Pattern URL_HTTP_PREFIX = Pattern.compile("^https?://.*");
    private static final Pattern IP_SUFFIX = Pattern.compile("[.0-9]*$");

    private static final String HTTP_DIGEST_AUTH_OPAQUE_KEY = "Campus_Pack_Digest_Opaque";
    private static final String HTTP_DIGEST_AUTH_NONCE_KEY = "Campus_Pack_Digest_Nonce";

    public static void setExpires(HttpServletResponse response, long now, long ms) {
        response.setDateHeader(HTTP_HEADER_EXPIRES, now + ms);
        response.setHeader(HTTP_HEADER_PRAGMA, "cache"); // TODO: kill me
        response.setHeader(HTTP_HEADER_CACHE_CONTROL, "max-age=" + (ms / 1000L));
    }

    public static void setExpired(HttpServletResponse response) {
        response.setHeader(HTTP_HEADER_CACHE_CONTROL, CACHE_CONTROL_REALLY_DONT_CACHE);
        response.setHeader(HTTP_HEADER_PRAGMA, PRAGMA_NO_CACHE);
        response.setHeader(HTTP_HEADER_EXPIRES, "0");
    }

    public static void sendRedirect(HttpServletResponse response, String location) {
        response.setHeader(HTTP_HEADER_LOCATION, location);
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    }

    public static void sendAutopost(HttpServletResponse response, String location) throws IOException {
        setExpired(response);
        response.setContentType(MimeUtils.MIME_TYPE_TEXT_HTML + MimeUtils.CHARSET_SUFFIX_UTF_8);
        response.getWriter().write("<html><body onload=\"document.forms[0].action+=document.location.hash;document.forms[0].submit()\"><form method=\"POST\" action=\"" + location + "\"></form></body></html>");
    }

    public static String getCookieValue(HttpServletRequest request, String name) {
        try {
            Cookie cookie = getCookie(request, name);
            return (cookie == null) ? null : URLDecoder.decode(cookie.getValue(), CharEncoding.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Cookie getCookie(HttpServletRequest request, String name) {
        Cookie match = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie: cookies) {
                if (name.equals(cookie.getName())) {
                    match = cookie;
                    break;
                }
            }
        }
        return match;
    }

    // http://stackoverflow.com/questions/93551/how-to-encode-the-filename-parameter-of-content-disposition-header-in-http
    public static String getDisposition(String dis, String fileName) {
        if (StringUtils.isAsciiPrintable(fileName)) {
            return dis + "; filename=\"" + fileName + "\"";
        } else {
            StringBuilder sb = new StringBuilder(dis);
            sb.append("; filename*=UTF-8''");
            return noSpacePlusURLEncode(fileName, sb);
        }
    }

    public static String noSpacePlusURLEncode(String toEncode) {
        return noSpacePlusURLEncode(toEncode, new StringBuilder());
    }

    private static String noSpacePlusURLEncode(String toEncode, StringBuilder sb) {
        try {
            byte[] buffer = new byte[1];
            for (byte b : toEncode.getBytes("UTF-8")) {
                if (((b >= '0') && (b <= '9')) || ((b >= 'a') && (b <= 'z')) ||
                  ((b >= 'A') && (b <= 'Z')) || (b == '-') || (b == '.') ||
                  (b == '_') || (b == '~')) {
                    sb.append((char) b);
                } else {
                    buffer[0] = b;
                    sb.append('%').append(DigestUtils.toHexString(buffer));
                }
            }
            return sb.toString();
        } catch (IOException ioe) { // this just can't happen, but checked exceptions...
            throw new RuntimeException(ioe);
        }
    }

    public static String getUrl(HttpServletRequest request, String path) {
        return getDomainUrl(null, request, path, request.isSecure());
    }

    public static String getDomainUrl(String domain, HttpServletRequest request, String path) {
        return getDomainUrl(domain, request, path, request.isSecure());
    }

    public static String getUrl(HttpServletRequest request, String path, boolean secure) {
        return getDomainUrl(null, request, path, secure);
    }

    public static String getDomainUrl(String domain, HttpServletRequest request, String path, boolean secure) {
        if (URL_HTTP_PREFIX.matcher(path).matches()) {
            return path;
        }
        return secure ? getDomainHttpsUrl(domain, request, path) : getDomainHttpUrl(domain, request, path);
    }

    public static String urlEncode(String toEncode) {
        try {
            return URLEncoder.encode(toEncode, "UTF-8");
        } catch (UnsupportedEncodingException wat) {
            throw new RuntimeException(wat);
        }
    }

    public static String getHttpUrl(HttpServletRequest request, String path) {
        return getDomainHttpUrl(null, request, path);
    }

    private static final Set<String> SAFE_METHODS =
      Set.of("GET", "HEAD", "OPTIONS");
    public static boolean isSafe(HttpServletRequest request) {
        return SAFE_METHODS.contains(request.getMethod().toUpperCase());
    }

    public static String getDomainHttpUrl(String domain, HttpServletRequest request, String path) {
        StringBuilder result = new StringBuilder();
        result.append("http://").append(StringUtils.isBlank(domain) ? request.getServerName() : domain);
        int serverPort = request.getServerPort();
        if (request.isSecure()) {
            serverPort = getInsecurePort(serverPort);
        }
        if (serverPort != HTTP_PORT) {
            result.append(":").append(serverPort);
        }
        result.append(path);
        return result.toString();
    }

    public static String getHttpsUrl(HttpServletRequest request, String path) {
        return getDomainHttpsUrl(null, request, path);
    }

    public static String getDomainHttpsUrl(String domain, HttpServletRequest request, String path) {
        StringBuilder result = new StringBuilder();
        result.append("https://").append(StringUtils.isBlank(domain) ? request.getServerName() : domain);
        int serverPort = request.getServerPort();
        if (!request.isSecure()) {
            serverPort = getSecurePort(serverPort);
        }
        if (serverPort != HTTPS_PORT) {
            result.append(":").append(serverPort);
        }
        result.append(path);
        return result.toString();
    }

    public static int getSecurePort(int insecurePort) {
        if (insecurePort == HTTP_PORT) {
            return HTTPS_PORT;
        } else if (insecurePort % 1000 == 80) {
            return insecurePort + 101; // 8080 -> 8181
        } else {
            throw new RuntimeException("Unknown insecure port: " + insecurePort);
        }
    }

    public static int getInsecurePort(int securePort) {
        if (securePort == HTTPS_PORT) {
            return HTTP_PORT;
        } else if (securePort % 1000 == 181) {
            return securePort - 101; // 8181 -> 8080
        } else {
            throw new RuntimeException("Unknown secure port: " + securePort);
        }
    }

    public static String generateOpaqueForDigestAuth() {
        return DigestUtils.toHexString(NumberUtils.getNonce());
    }

    public static String generateNonceForDigestAuth() {
        return DigestUtils.toHexString(NumberUtils.getNonce());
    }

    public static void setDigestAuthHeaders(HttpServletRequest request, HttpServletResponse response, String realm)
            throws IOException {
        // TODO: This is totally vulnerable to replay because we don't store the
        // nonce to revidate it..
        String header = getDigestAuthHeader(realm, generateNonceForDigestAuth());
        response.setHeader(HttpUtils.HTTP_HEADER_WWW_AUTH, header);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    public static String getBasicAuthHeader(String realm) {
        String equals = "=";
        String quote = "\"";
        String separator = ", ";
        String space = " ";

        String opaque = HttpUtils.generateOpaqueForDigestAuth();

        StringBuffer header = new StringBuffer();
        header.append(HttpUtils.HTTP_BASIC_AUTH_HEADER_NAME);
        header.append(space);
        header.append(HttpUtils.HTTP_AUTH_HEADER_ATTR_REALM);
        header.append(equals);
        header.append(quote);
        header.append(realm);
        header.append(quote);

        return header.toString();
    }

    public static String getDigestAuthHeader(String realm, String nonce) {
        String equals = "=";
        String quote = "\"";
        String separator = ", ";
        String space = " ";

        String opaque = HttpUtils.generateOpaqueForDigestAuth();

        StringBuffer header = new StringBuffer();
        header.append(HttpUtils.HTTP_DIGEST_AUTH_HEADER_NAME);
        header.append(space);
        header.append(HttpUtils.HTTP_AUTH_HEADER_ATTR_REALM);
        header.append(equals);
        header.append(quote);
        header.append(realm);
        header.append(quote);
        header.append(separator);
        header.append(HttpUtils.HTTP_DIGEST_AUTH_HEADER_ATTR_NONCE);
        header.append(equals);
        header.append(quote);
        header.append(nonce);
        header.append(quote);
        header.append(separator);
        header.append(HttpUtils.HTTP_DIGEST_AUTH_HEADER_ATTR_OPAQUE);
        header.append(equals);
        header.append(quote);
        header.append(opaque);
        header.append(quote);

        return header.toString();
    }

    public static Map<String, String> getDigestAuthHeaderAttributes(HttpServletRequest request) {

        Map<String,String> digestHeaderMap = new HashMap<String,String>();

        String authHeader = request.getHeader(HttpUtils.HTTP_HEADER_AUTHORIZATION);
        if (!StringUtils.isEmpty(authHeader) && authHeader.startsWith(HttpUtils.HTTP_DIGEST_AUTH_HEADER_NAME)) {

                String digestHeaderAttrs = authHeader.substring(HttpUtils.HTTP_DIGEST_AUTH_HEADER_NAME.length());

                if (!StringUtils.isEmpty(digestHeaderAttrs)) {

                    String[] fields = digestHeaderAttrs.trim().split(",");

                    for (String field : fields) {

                        String[] pair = field.trim().split("=",2);
                        if (null != pair) {
                            if (pair.length > 1) {
                                String value = pair[1].trim();
                                if (!StringUtils.isEmpty(value)) {
                                    if (value.startsWith("\"")) {
                                        value = value.substring(1);
                                    }
                                    if (value.endsWith("\"")) {
                                        value = value.substring(0,value.length()-1);
                                    }
                                }
                                digestHeaderMap.put(pair[0].trim().toLowerCase(), value);
                            } else if (pair.length == 1) {
                                digestHeaderMap.put(pair[0].trim().toLowerCase(), "");
                            }
                        }
                    }
                }
            }

        return digestHeaderMap;
    }

    public static Map<String, List<String>> getParameters(String url) throws Exception {
        Map<String, List<String>> params = new HashMap<String, List<String>>();
        String[] urlParts = url.split("\\?");
        if (urlParts.length > 1) {
            String query = urlParts[1];
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                String key = URLDecoder.decode(pair[0], CharEncoding.UTF_8);
                String value = URLDecoder.decode((pair.length < 2) ? "" : pair[1], CharEncoding.UTF_8);
                List<String> values = params.get(key);
                if (values == null) {
                    values = new ArrayList<String>();
                    params.put(key, values);
                }
                values.add(value);
            }
        }
        return params;
    }

    public static boolean supportsCompression(HttpServletRequest request) {
        String acceptEncoding = (request == null) ? null : request.getHeader(HttpUtils.HTTP_HEADER_ACCEPT_ENCODING);
        return StringUtils.containsIgnoreCase(acceptEncoding, HttpUtils.ENCODING_GZIP);
    }

    private static Boolean __gzipResponse;

    private static boolean gzipResponse() {
        return __gzipResponse;
    }

    public static void configure(Config config) {
        __gzipResponse = config.getBoolean("com.learningobjects.cpxp.web.gzip");
    }

    public static Writer getWriter(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if ("HEAD".equals(request.getMethod())) {
            return new NullWriter();
        }
        return new BufferedWriter(new OutputStreamWriter(getOutputStream(request, response), CharEncoding.UTF_8));
    }

    public static OutputStream getOutputStream(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if ("HEAD".equals(request.getMethod())) {
            return new NullOutputStream();
        }
        OutputStream out = response.getOutputStream();
        if (gzipResponse()) {
            String acceptEncoding = request.getHeader(HTTP_HEADER_ACCEPT_ENCODING);
            if (StringUtils.containsIgnoreCase(acceptEncoding, ENCODING_GZIP)) {
                String encoding = StringUtils.containsIgnoreCase(acceptEncoding, ENCODING_X_GZIP) ? ENCODING_X_GZIP : ENCODING_GZIP;
                response.setHeader(HTTP_HEADER_CONTENT_ENCODING, encoding);
                // response.addHeader(HTTP_HEADER_VARY, HTTP_HEADER_ACCEPT_ENCODING); // to be pedantic but we don't currently cache these responses
                out = new GZIPOutputStream(out);
            }
        }
        return out;
    }

    public static String getRemoteAddr(HttpServletRequest request, ServiceMeta serviceMeta) {
        String remoteAddr = null;
        // We allow integration tests to "spoof" an IP by specifying the IP in an HTTP Header.
        // This is not allowed on production systems.
        if (!serviceMeta.isProdLike()) {
            remoteAddr = request.getHeader(HTTP_HEADER_X_REMOTE_ADDRESS);
        }

        if (StringUtils.isEmpty(remoteAddr)) {
            // Amazon load balancer will put the real IP in the x-forwarded-for field.
            String header = request.getHeader(HTTP_HEADER_X_FORWARDED_FOR);
            if (header != null) {
                // If the original request had a x-forwarded-for already defined, load balancer will
                // append the ip after a comma.  So try to pull the IP off the end.
                int commaIndex = header.lastIndexOf(',');
                remoteAddr = (commaIndex < 0 ? header.trim() : header.substring(commaIndex+1).trim());
            }
        }

        if (StringUtils.isEmpty(remoteAddr)) {
            remoteAddr = request.getRemoteAddr();
        }

        return remoteAddr;
    }

    /**
     * Set the HTTP headers such that it appears this request is coming from a different
     * IP address than it actually is.  This is useful in integration tests but it ignored
     * in production systems.
     */
    public static void spoofAddress(HttpMessage request, String ip) {
        request.setHeader(HttpUtils.HTTP_HEADER_X_REMOTE_ADDRESS, ip);
    }

    public static boolean isReload(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String cc = request.getHeader(HTTP_HEADER_CACHE_CONTROL), pragma = request.getHeader(HTTP_HEADER_PRAGMA);
        return "no-cache".equalsIgnoreCase(cc) ||
            "max-age=0".equalsIgnoreCase(cc) ||
            "no-cache".equalsIgnoreCase(pragma);
    }
    private static DefaultHttpClient __httpClient;

    public static synchronized CloseableHttpClient getHttpClient() {
        if (__httpClient == null) {
            __httpClient = HttpClientBuilder.newMonitoredClient("Default").build(ClientConfig.fromConfig(ConfigFactory.load())); //TODO: Accept config a parameter.
        }
        return __httpClient;
    }

    public static void enableRedirects(HttpMessage message) {
        final BasicHttpParams params = new BasicHttpParams();
        params.setParameter(ClientPNames.HANDLE_REDIRECTS, true);
        message.setParams(params);
    }

    public static boolean isMaxedOut(HttpClient client) {
        PoolingClientConnectionManager mgr = (PoolingClientConnectionManager) client.getConnectionManager();
        PoolStats stats = mgr.getTotalStats();
        return stats.getLeased() >= stats.getMax();
    }

    static class TrustAllStrategy implements TrustStrategy {
        @Override
        public boolean isTrusted(X509Certificate[] chain, String authType) {
            return true;
        }
    }

    static class ProxySocketFactory extends PlainSocketFactory {
        private final Proxy _proxy;

        public ProxySocketFactory(final Proxy proxy) {
            _proxy = proxy;
        }

        public Proxy getProxy() {
            return _proxy;
        }

        @Override
        public Socket createSocket(final HttpParams params) {
            return new Socket(_proxy);
        }
    }

    static class ProxySSLSocketFactory extends SSLSocketFactory {
        private final Proxy _proxy;

        public ProxySSLSocketFactory(final Proxy proxy) throws Exception {
            super(new TrustAllStrategy(), ALLOW_ALL_HOSTNAME_VERIFIER);
            _proxy = proxy;
        }

        @Override
        public Socket createSocket(final HttpParams params) {
            return new Socket(_proxy);
        }
    }

    public static boolean isAjax(HttpServletRequest request) {
        return WITH_XML_HTTP_REQUEST.equals(request.getHeader(HTTP_HEADER_X_REQUESTED_WITH)) || StringUtils.startsWith(request.getContentType(), MimeUtils.MIME_TYPE_APPLICATION_JSON) || StringUtils.contains(request.getHeader(HTTP_HEADER_ACCEPT), MimeUtils.MIME_TYPE_APPLICATION_JSON);
    }

    public static boolean isFirefox(HttpServletRequest request) {
        return StringUtils.containsIgnoreCase(request.getHeader(HTTP_HEADER_USER_AGENT), "Firefox");
    }

    public static List<org.apache.http.cookie.Cookie> parseCookies(URI uri, List<String> cookieHeaders, CookieSpec cookieSpec) {
        ArrayList<org.apache.http.cookie.Cookie> cookies = new ArrayList<org.apache.http.cookie.Cookie>();
        int port = (uri.getPort() < 0) ? 80 : uri.getPort();
        boolean secure = "https".equals(uri.getScheme());
        CookieOrigin origin = new CookieOrigin(uri.getHost(), port,
                uri.getPath(), secure);
        for (String cookieHeader : cookieHeaders) {
            BasicHeader header = new BasicHeader(SM.SET_COOKIE, cookieHeader);
            try {
                cookies.addAll(cookieSpec.parse(header, origin));
            } catch (MalformedCookieException e) {
                throw new RuntimeException(e);
            }
        }
        return cookies;
    }

    /**
     * Builder for new HttpClient objects.
     */
    public static class HttpClientBuilder {

        public static final boolean DEFAULT_PROXY = true;
        public static final int DEFAULT_LIMIT = 16;
        public static final int DEFAULT_CONNECTION_TIMEOUT = 10000;
        public static final int DEFAULT_SOCKET_TIMEOUT = 15000;

        private final String name;

        private boolean useProxy = DEFAULT_PROXY;
        private int limit = -1;
        private int connectionTimeout = -1;
        private int socketTimeout = -1;

        private HttpClientBuilder(final String name) {
            this.name = name;
        }

        public static HttpClientBuilder newClient() {
            return new HttpClientBuilder(null);
        }

        public static HttpClientBuilder newMonitoredClient(final String name) {
            return new HttpClientBuilder(name);
        }

        /**
         * Set maximum number of connections.
         * @see HttpClientBuilder#DEFAULT_LIMIT
         */
        public HttpClientBuilder setLimit(int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Set whether or not using proxy.
         * Note that a proxy will only be used if configuration contains proxy's host and port.
         * @see HttpClientBuilder#DEFAULT_PROXY
         */
        public HttpClientBuilder setUseProxy(boolean useProxy) {
            this.useProxy = useProxy;
            return this;
        }

        /**
         * Set connection timeout, in millis.
         * @see HttpClientBuilder#DEFAULT_CONNECTION_TIMEOUT
         */
        public HttpClientBuilder setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        /**
         * Set socket timeout, in millis.
         * @see HttpClientBuilder#DEFAULT_SOCKET_TIMEOUT
         */
        public HttpClientBuilder setSocketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        /**
         * Build the client.
         */
        public DefaultHttpClient build(ClientConfig clientConfig) {
            BasicHttpParams params = new BasicHttpParams();
            HttpProtocolParams.setUserAgent(params, "CampusPack/0.0");
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpClientParams.setAuthenticating(params, false);
            HttpClientParams.setRedirecting(params, false);
            HttpClientParams.setCookiePolicy(params, CookiePolicy.BROWSER_COMPATIBILITY);

            HttpConnectionParams.setConnectionTimeout(params, clientConfig.connectionTimeout);
            HttpConnectionParams.setSoTimeout(params, clientConfig.soTimeout);

            Proxy proxy;
            if (this.useProxy && !clientConfig.socksHost.isEmpty() && (clientConfig.socksPort > 0)) {
                proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(clientConfig.socksHost, clientConfig.socksPort));
            } else {
                proxy = Proxy.NO_PROXY;
            }

            SchemeRegistry schemes = new SchemeRegistry();
            try {
                schemes.register(new Scheme("http", 80, new ProxySocketFactory(proxy)));
                schemes.register(new Scheme("https", 443, new ProxySSLSocketFactory(proxy)));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            PoolingClientConnectionManager connections = new PoolingClientConnectionManager(schemes);
            connections.setMaxTotal(clientConfig.maxConnections);
            connections.setDefaultMaxPerRoute(clientConfig.maxConnections);

            return (this.name == null) ?
              new DefaultHttpClient(connections, params) :
              new MonitoredHttpClient(this.name, connections, params);
        }
    }
}

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

package loi.cp.generic;

import com.learningobjects.cpxp.component.ComponentDescriptor;
import com.learningobjects.cpxp.component.ComponentEnvironment;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.template.HtmlTemplateUtils;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.component.web.*;
import com.learningobjects.cpxp.service.mime.MimeWebService;
import com.learningobjects.cpxp.util.HttpUtils;
import com.learningobjects.cpxp.util.MimeUtils;
import com.learningobjects.cpxp.util.StringUtils;
import com.typesafe.config.Config;
import loi.apm.Apm;
import loi.cp.domain.GenericDomain;
import loi.cp.localProxy.LocalProxy;
import loi.cp.session.SessionComponent;
import org.apache.commons.io.IOUtils;

import javax.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;

@Component(enabled = false)
public class GenericServlet extends AbstractComponentServlet {
    @Inject
    private ComponentEnvironment _environment;

    @Inject
    private Config config;

    @Inject
    private MimeWebService _mimeWebService;

    @Inject
    private LocalProxy _localProxy;

    @Override
    public void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final ServletBinding binding = getDelegateDescriptor().getBinding(ServletComponent.class);
        String pi = StringUtils.removeStart(request.getPathInfo(), binding.path());
        if ("".equals(pi)) {
            String queryString = request.getQueryString();
            HttpUtils.sendRedirect(response, binding.path() + "/" + (queryString != null ? "?" + queryString : ""));
        } else {
            /* I would like to asComponent here but that only works if I make this a delegate and... yeah. no. */
            var context = new GenericDomain(binding, getComponentInstance(), _environment, request, config);
            /* Single-page app always renders index.html. */
            String doc = ("/".equals(pi) || binding.spa()) ? "index.html" : pi.substring(1);

            var proxied = _localProxy.proxyRequest(this, binding.path(), request, scala.Option.empty());
            if (proxied.isDefined()) {
                WebResponseOps.send(proxied.get(), request, response);
                return;
            }

            ComponentDescriptor component = getDelegateDescriptor().getComponent();
            URL url = component.getResource(doc);
            if (url == null) {
                throw new FileNotFoundException("Missing template: " + pi);
            }

            Path path = Paths.get(url.toURI());
            if ("file".equals(url.getProtocol()) && Files.isDirectory(path)) {
                Path indexTemplate = path.resolve("index.html");
                if (Files.exists(indexTemplate)) {
                    url = indexTemplate.toUri().toURL();
                } else {
                    throw new FileNotFoundException("Missing template: " + pi);
                }
            }

            String mimeType = _mimeWebService.getMimeType(doc);
            // TODO: KILLME AND FIX MIME TYPES UPGRADE
            if (doc.endsWith(".manifest")) {
                mimeType = MimeUtils.MIME_TYPE_TEXT_CACHE_MANIFEST;
            } else if (doc.endsWith(".json")) {
                mimeType = MimeUtils.MIME_TYPE_APPLICATION_JSON;
            } else if (doc.endsWith(".js") || doc.endsWith(".css") || doc.endsWith(".txt")) {
                mimeType += MimeUtils.CHARSET_SUFFIX_UTF_8;
            }


            response.setContentType(StringUtils.defaultIfEmpty(mimeType, MimeUtils.MIME_TYPE_APPLICATION_UNKNOWN));
            HttpUtils.setExpired(response);
            if (doc.endsWith("html")) {
                /* this barely works. I can't believe it. */
                WebResponseOps.send(HtmlResponse.apply(context, doc), request, response);
            } else if (doc.endsWith(".manifest") || doc.endsWith(".json")) {
                if (doc.endsWith(".manifest")) {
                    response.setHeader(HttpUtils.HTTP_HEADER_EXPIRES, "1");
                }
                boolean json = doc.endsWith(".json");
                String str; // TODO: Read a line at a time?
                try (InputStream in = url.openStream()) {
                    str = IOUtils.toString(in, "UTF-8");
                }
                try (Writer w = response.getWriter()) {
                    int index = 0;
                    Matcher m = HtmlTemplateUtils.expressionMatcher(str);
                    while (m.find()) {
                        w.write(str.substring(index, m.start()));
                        Object o = HtmlTemplateUtils.expandExpression(component, context, m);
                        w.write(json ? ComponentUtils.toJson(o) : String.valueOf(o));
                        index = m.end();
                    }
                    w.write(str.substring(index));
                }
            } else {
                try (InputStream in = url.openStream()) {
                    try (OutputStream out = response.getOutputStream()) {
                        IOUtils.copy(in, out);
                    }
                }
            }
        }
    }

    public String getTrackingHeader() {
        return apmEnabled() ? Apm.getBrowserTimingHeader() : null;
    }

    public String getTrackingFooter() {
        return apmEnabled() ? Apm.getBrowserTimingFooter() : null;
    }

    public boolean getSudoed() {
        return ComponentSupport.get(SessionComponent.class).isSudoed();
    }

    private boolean apmEnabled() {
        return config.getBoolean("apm.enabled");
    }
}

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

package loi.cp.domain;

import com.google.common.base.Throwables;
import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.component.*;
import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.site.ItemSiteBinding;
import com.learningobjects.cpxp.component.site.ItemSiteComponent;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.component.util.HtmlTemplate;
import com.learningobjects.cpxp.component.web.HtmlResponse;
import com.learningobjects.cpxp.component.web.ServletBinding;
import com.learningobjects.cpxp.component.web.WebResponse;
import com.learningobjects.cpxp.component.web.WebResponseOps;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.domain.DomainDTO;
import com.learningobjects.cpxp.service.domain.DomainWebService;
import com.learningobjects.cpxp.service.script.ComponentFacade;
import com.learningobjects.cpxp.service.user.UserDTO;
import com.learningobjects.cpxp.shale.JsonMap;
import com.learningobjects.cpxp.util.HttpUtils;
import com.learningobjects.cpxp.util.MimeUtils;
import com.typesafe.config.Config;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import loi.apm.Apm;
import loi.cp.right.Right;
import loi.cp.right.RightService;
import loi.cp.util.Lazy;
import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ItemSiteBinding(type = "Domain", action = "view")
public class GenericDomain extends BaseComponent implements ItemSiteComponent, ComponentDecorator { // sheesh

    private final ComponentEnvironment _componentEnvironment;

    private final HttpServletRequest _request;

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final ServletBinding _binding; // !!!

    private final Lazy<Map> _componentConfiguration;

    private final Config config;

    public GenericDomain(
      @Init ServletBinding binding,
      ComponentInstance ci,
      ComponentEnvironment env,
      HttpServletRequest req,
      Config config
    ) {
        super(ci);
        _componentEnvironment = env;
        _request = req;
        _binding = binding;
        this.config = config;
        /* this is here b/c javac can't figure out what "final" means, evidently */
        _componentConfiguration = new Lazy<>(() -> {
            try {
                return _componentEnvironment.getJsonConfiguration(getComponentInstance().getIdentifier());
            } catch (Exception ex) {
                throw Throwables.propagate(ex);
            }
        });
    }

    @Override
    public WebResponse renderSite(String view) {
        return HtmlResponse.apply(this, "index.html");
    }

    @Tag
    public HtmlTemplate layout(@Parameter ComponentFacade content, @Parameter String title, @Parameter String bodyClass, @Parameter Callable body, @Parameter Callable sidebar, @Parameter Callable head, @Parameter Callable header, @Parameter String autofocus, @Parameter Boolean ecommerce, @Parameter Boolean loginrequired, @Parameter Boolean nologin, @Parameter Boolean nosearch, @Parameter Boolean noheader, @Parameter String itemtype, @Parameter Boolean coursestyle, @Parameter String description, @Parameter String keywords, @Parameter Boolean visibility) {
        return HtmlTemplate.apply(this, "sys/layout.html")
          .bind("content", content)
          .bind("title", title)
          .bind("bodyClass", bodyClass)
          .bind("body", body)
          .bind("sidebar", sidebar)
          .bind("head", head)
          .bind("header", header)
          .bind("autofocus", autofocus)
          .bind("ecommerce", ecommerce)
          .bind("loginrequired", loginrequired)
          .bind("nologin", nologin)
          .bind("nosearch", nosearch)
          .bind("noheader", noheader)
          .bind("itemtype", itemtype)
          .bind("coursestyle", coursestyle)
          .bind("description", description)
          .bind("keywords", keywords)
          .bind("visibility", visibility);
    }

    @Tag
    public HtmlTemplate page(@Parameter String title, @Parameter String bodyClass, @Parameter Callable body) {
        return HtmlTemplate.apply(this, "sys/page.html")
          .bind("title", title)
          .bind("bodyClass", bodyClass)
          .bind("body", body);
    }

    public JsonMap getUser() {
        return _userJson.get();
    }

    private Lazy<JsonMap> _userJson = new Lazy<>(() -> {
        if (Current.isAnonymous()) {
            return null;
        }
        UserDTO u = Current.getUserDTO();
        JsonMap map = new JsonMap();
        map.put("id", u.getId());
        map.put("userName", u.getUserName());
        map.put("givenName", u.getGivenName());
        map.put("familyName", u.getFamilyName());
        map.put("emailAddress", u.getEmailAddress());
        return map;
    });

    public JsonMap getDomain() {
        return _domainJson.get();
    }

    private Lazy<JsonMap> _domainJson = new Lazy<>(() -> {
        DomainDTO d = Current.getDomainDTO();
        JsonMap map = new JsonMap();
        map.put("id", d.getId());
        map.put("name", d.getName());
        map.put("shortName", d.getShortName());
        return map;
    });

    public Map getComponentConfiguration() {
        return _componentConfiguration.get();
    }

    public Set<Class<? extends Right>> getUserRights() {
        return _userRights.get();
    }

    private Lazy<Set<Class<? extends Right>>> _userRights = new Lazy<>(() -> {
        return ComponentSupport.lookupService(RightService.class).getUserRights();
    });

    public Current getCurrent() {
        return Current.getInstance();
    }

    public String getIpAddress() {
        return HttpUtils.getRemoteAddr(_request, BaseServiceMeta.getServiceMeta());
    }

    public String getAdminLink() {
        return _adminLink.get();
    }

    private Lazy<String> _adminLink = new Lazy<>(() -> {
        return ComponentSupport.lookupService(DomainWebService.class).getAdministrationLink();
    });

    private boolean apmEnabled() {
        return config.getBoolean("apm.enabled");
    }

    public String getTrackingHeader() {
        return apmEnabled() ? Apm.getBrowserTimingHeader() : null;
    }

    public String getTrackingFooter() {
        return apmEnabled() ? Apm.getBrowserTimingFooter() : null;
    }


    private static final Pattern TOKEN_RE = Pattern.compile("\\$\\$([^( ='\"/]+/)");

    // TODO: This is a transient hack to serve expanded resources from a domain component
    @Get
    @Direct
    public void notFound(@PathInfo String pi, ComponentDescriptor component, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ItemSiteBinding binding = component.getAnnotation(ItemSiteBinding.class);
        if (binding != null) {
            for (String route : binding.routes()) {
                if (pi.equalsIgnoreCase(route)) {
                    pi = "/"; // SPA routes always serve index.html
                    break;
                }
            }
        }
        if (!pi.endsWith("/") && (component.getResource(pi + "/index.html") != null)) {
            String queryString = request.getQueryString();
            response.sendRedirect("/" + pi + "/" + (queryString != null ? "?" + queryString : ""));
            return;
        }
        pi = pi.endsWith("/") ? pi + "index.html" : pi;
        URL url = component.getResource(pi);
        if ((url == null) || pi.startsWith("sys/")) {
            throw new FileNotFoundException(pi);
        } else if (pi.endsWith(".html")) {
            WebResponseOps.send(HtmlResponse.apply(this, pi), request, response);
        } else if (pi.endsWith(".manifest") || pi.endsWith(".json") || pi.endsWith(".js")) {
            // TODO: FIXME: ugh but no time
            String mimeType = pi.endsWith(".manifest") ? MimeUtils.MIME_TYPE_TEXT_CACHE_MANIFEST :
              pi.endsWith(".json") ? MimeUtils.MIME_TYPE_APPLICATION_JSON : MimeUtils.MIME_TYPE_TEXT_JAVASCRIPT;
            response.setContentType(mimeType + MimeUtils.CHARSET_SUFFIX_UTF_8);
            HttpUtils.setExpired(response);
            String str;
            try (InputStream in = url.openStream()) {
                str = IOUtils.toString(in, "UTF-8");
            }
            Matcher m = TOKEN_RE.matcher(str);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String msg = ComponentUtils.getMessage(m.group(1), component);
                m.appendReplacement(sb, Matcher.quoteReplacement(msg));
            }
            m.appendTail(sb);
            try (Writer w = response.getWriter()) {
                w.write(sb.toString());
            }
        } else {
            throw new FileNotFoundException(pi);
        }
    }
}

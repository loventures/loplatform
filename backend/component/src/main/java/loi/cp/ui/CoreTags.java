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

package loi.cp.ui;

import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.HtmlWriter;
import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.service.name.UrlFacade;
import com.learningobjects.cpxp.util.FormattingUtils;
import com.learningobjects.cpxp.util.StringUtils;
import org.apache.commons.lang3.BooleanUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.Callable;

public interface CoreTags extends ComponentInterface {

    HtmlWriter out();

    @Tag
    default void separator(@Parameter(name="value") final String value) {
        HtmlWriter out = out();
        if (!out.isElementEmpty()) {
            out.write(ComponentUtils.or(value, " | "));
        }
    }

    @Tag
    default void separated(@Required @Parameter(name="body") final Callable body,
                                 @Parameter(name="separator") final String separator) {
        HtmlWriter out = out();
        out.startElement(getComponentInstance().getIdentifier() + ":separator");
        out.startAttribute("value");
        out.write( separator );
        out.closeEndElement();
        out.write(body);
    }

    @Tag
    default void javascript(@Parameter(name="body") final Callable body,
                                  @Parameter(name="src") final String src,
                                  @Parameter(name="charset") final String charset,
                                  @Parameter(name="once") String once,
                                  @Parameter(name="async") final Boolean async,
                                  @Parameter(name="commented") final Boolean commented,
                                  @Parameter(name="allowdups") final Boolean allowDups,
                                  @Infer final HttpServletRequest request) {
        HtmlWriter out = out();
        if (once != null && ! dupCheck(request, "javascript:" + once)) {
            return;
        }
        if (BooleanUtils.isTrue(commented)) {
            out.raw("<!-- ");
        }
        if (StringUtils.isEmpty(src)) {
            out.startElement("script");
            out.startAttribute("type");
            out.raw("text/javascript");
            out.closeElement();
                out.setJsonEncoding(true);
                try {
                    out.write(body);
                } finally {
                    out.setJsonEncoding(false);
                }
            out.endElement("script");
        } else if (BooleanUtils.isTrue(allowDups) || dupCheck(request, "javascript:" + src)) { // TODO: Merge and combine...
            String url = isAbsolute(src) ? src : FormattingUtils.gzLocalizedUrl("scripts/" + src);
            out.startElement("script");
            out.startAttribute("type");
            out.raw("text/javascript");
            out.startAttribute("src");
            out.write( url );
            out.startAttribute("charset");
            out.write( charset );
            if (BooleanUtils.isTrue(async)) {
                out.startAttribute("async");
                out.write( async );
            }
            out.closeElement();
            out.endElement("script");
        }
        if (BooleanUtils.isTrue(commented)) {
            out.raw(" -->");
        }
    }

    @Tag
    default void stylesheet(@Parameter(name="body") final Callable body,
                                  @Parameter(name="href") final String href,
                                  @Infer final HttpServletRequest request) {
        HtmlWriter out = out();
        if (StringUtils.isEmpty(href)) {
            out.startElement("style");
            out.startAttribute("type");
            out.raw("text/css");
            out.closeElement();
                out.write(body);
            out.endElement("style");
        } else if (dupCheck(request, "stylesheet:" + href)) { // TODO: Merge and combine...
            String url = isAbsolute(href) ? href : FormattingUtils.gzStaticUrl("styles/" + href);
            out.startElement("link");
            out.startAttribute("rel");
            out.raw("stylesheet");
            out.startAttribute("type");
            out.raw("text/css");
            out.startAttribute("href");
            out.write( url );
            out.closeEndElement();
        }
    }

    static boolean jsDupCheck(final String key) {
        return dupCheck(BaseWebContext.getContext().getRequest(), "javascript:" + key);
    }

    static boolean cssDupCheck(final String key) {
        return dupCheck(BaseWebContext.getContext().getRequest(), "stylesheet:" + key);
    }

    static boolean dupCheck(final HttpServletRequest request, final String key) {
        if (request.getAttribute(key) != null) {
            return false;
        }
        request.setAttribute(key, true);
        return true;
    }

    static boolean isAbsolute(String uri) {
        return uri.startsWith("/") || (uri.indexOf(':') >= 0);
    }

    @Tag
    default void hyperlink(@Parameter(name="id") final String id,
                                 @Parameter(name="href") final Object href,
                                 @Parameter(name="action") final String action,
                                 @Parameter(name="label") final String label,
                                 @Parameter(name="disabled") final Object disabled,
                                 @Parameter(name="target") final String target,
                                 @Parameter(name="body") final Callable body,
                                 @Parameters final Map<String, Object> parameters) {
        HtmlWriter out = out();
        final boolean off = ComponentUtils.test(disabled);
        String url = (href instanceof UrlFacade) ? ((UrlFacade) href).getUrl() : (String) href;
        if (url != null) {
            url = url + StringUtils.defaultString(action);
        }
        if (StringUtils.contains(label, "<a>")) {
            out.write(StringUtils.substringBefore(label, "<a>"));
            out.startElement("a");
            out.startAttribute("id");
            out.write( id );
            out.startAttribute("href");
            out.write( url );
            out.startAttribute("target");
            out.write( target );
            out.startAttribute("title");
            out.write( ComponentUtils.dereference(parameters, "title") );
            out.startAttribute("role");
            out.write( ComponentUtils.dereference(parameters, "role") );
            out.startAttribute("target");
            out.write( ComponentUtils.dereference(parameters, "target") );
            out.startAttribute("onclick");
            out.write( ComponentUtils.dereference(parameters, "onclick") );
            out.startAttribute("class");
            out.write( ComponentUtils.dereference(parameters, "linkClass") );
            out.startAttribute("style");
            out.write( ComponentUtils.dereference(parameters, "style") );
            out.closeElement();
                out.write(StringUtils.substringBetween(label, "<a>", "</a>"));out.write(body);
            out.endElement("a");
            out.write(StringUtils.substringAfter(label, "</a>"));
        } else {
            out.startElement("a");
            out.startAttribute("id");
            out.write( id );
            out.startAttribute("href");
            out.write( off ? null : url );
            out.startAttribute("target");
            out.write( target );
            out.startAttribute("title");
            out.write( ComponentUtils.dereference(parameters, "title") );
            out.startAttribute("role");
            out.write( ComponentUtils.dereference(parameters, "role") );
            out.startAttribute("target");
            out.write( ComponentUtils.dereference(parameters, "target") );
            out.startAttribute("onclick");
            out.write( off ? null : ComponentUtils.dereference(parameters, "onclick") );
            out.startAttribute("class");
            out.write( ComponentUtils.dereference(parameters, "linkClass") );
            out.raw(" ");
            out.write( off ? "unavailableLink" : null );
            out.startAttribute("style");
            out.write( ComponentUtils.dereference(parameters, "style") );
            out.closeElement();
                out.write(label);out.write(body);
            out.endElement("a");
        }
    }

    @Tag
    default void jslink(@Parameter(name="id") final String id,
                              @Parameter(name="href") final Object href,
                              @Parameter(name="script") final String script,
                              @Parameter(name="function") final String function,
                              @Parameter(name="paramThis") final Object paramThis,
                              @Parameter(name="label") final String label,
                              @Parameter(name="disabled") final Object disabled,
                              @Parameter(name="body") final Callable body,
                              @Parameters final Map<String, Object> parameters) {
        HtmlWriter out = out();
        out.startElement(getComponentInstance().getIdentifier() + ":hyperlink");
        out.startAttribute("id");
        out.write( id );
        out.startAttribute("href");
        out.write( (href == null) ? "#" : href );
        out.startAttribute("label");
        out.write( label );
        out.startAttribute("body");
        out.write( body );
        out.startAttribute("onclick");
        out.write( onClick(script, function, paramThis, parameters) );
        out.startAttribute("parameters");
        out.write( parameters );
        out.startAttribute("disabled");
        out.write( disabled );
        out.closeEndElement();
    }

    @Tag
    default void button(@Parameter(name="id") final String id,
                              @Parameter(name="script") final String script,
                              @Parameter(name="function") final String function,
                              @Parameter(name="paramThis") final Object paramThis,
                              @Parameter(name="label") final String label,
                              @Parameter(name="style") final String style,
                              @Parameter(name = "class") final String clas,
                              @Parameter(name="disabled") final Object disabled,
                              @Parameters final Map<String, Object> parameters,
                              @Parameter(name="title") final String title) {
        HtmlWriter out = out();
        out.startElement("input");
        out.startAttribute("id");
        out.write( id );
        out.startAttribute("type");
        out.raw("button");
        out.startAttribute("disabled");
        out.write( disabled );
        out.startAttribute("class");
        out.raw("cp_button ");
        out.write(clas);
        out.startAttribute("style");
        out.write( style );
        out.startAttribute("value");
        out.write( label );
        out.startAttribute("onclick");
        out.write( onClick(script, function, paramThis, parameters) );
        out.startAttribute("title");
        out.write( title );
        out.closeEndElement();
    }

    static String onClick(String script, String function, Object paramThis, Map<String, Object> parameters) {
        if (StringUtils.isNotEmpty(script)) {
            return script;
        }
        if (StringUtils.isNotEmpty(function)) {
            StringBuilder onclick = new StringBuilder();
            onclick.append(function).append("(");
            boolean hasThis = ComponentUtils.test(paramThis);
            if (hasThis) {
                onclick.append("this");
            }
            for (int i = 0; parameters.containsKey("param" + i); ++ i) {
                if (hasThis || (i > 0)) {
                    onclick.append(',');
                }
                onclick.append(ComponentUtils.toJson(parameters.get("param" + i)));
            }
            onclick.append(");return false;");
            return onclick.toString();
        }
        return null;
    }

    @Tag
    default void allClear() {
        HtmlWriter out = out();
        out.startElement("div");
        out.startAttribute("role");
        out.raw("presentation");
        out.startAttribute("class");
        out.raw("allClear");
        out.closeElement();out.endElement("div");
    }

    @Tag
    default void div(@Parameter(name="id") final String id,
                           @Parameter(name="role") final String role,
                           @Parameter(name = "class") final String clas,
                           @Parameter(name="style") final String style,
                           @Parameter(name="body") final Object body) {
        HtmlWriter out = out();
        out.startElement("div");
        out.startAttribute("id");
        out.write( id );
        out.startAttribute("role");
        out.write( role );
        out.startAttribute("class");
        out.write( clas );
        out.startAttribute("style");
        out.write( style );
        out.closeElement();out.write(body);out.endElement("div");
    }

    @Tag
    default void span(@Parameter(name="id") final String id,
                           @Parameter(name="role") final String role,
                           @Parameter(name = "class") final String clas,
                           @Parameter(name="style") final String style,
                           @Parameter(name="body") final Object body) {
        HtmlWriter out = out();
        out.startElement("span");
        out.startAttribute("id");
        out.write( id );
        out.startAttribute("role");
        out.write( role );
        out.startAttribute("class");
        out.write( clas );
        out.startAttribute("style");
        out.write( style );
        out.closeElement();out.write(body);out.endElement("span");
    }

    @Tag
    default void label(@Required @Parameter(name = "for") final String phor,
                             @Parameter(name = "class") final String clas,
                             @Parameter(name="style") final String style,
                             @Parameter(name="body") final Object body) {
        HtmlWriter out = out();
        out.startElement("label");
        out.startAttribute("for");
        out.write( phor );
        out.startAttribute("class");
        out.write( clas );
        out.startAttribute("style");
        out.write( style );
        out.closeElement();out.write(body);out.endElement("label");
    }

    @Tag
    default void h3(@Parameter(name="style") final String style,
                          @Parameter(name = "class") final String clas,
                          @Parameter(name="body") final Object body) {
        HtmlWriter out = out();
        out.startElement("h3");
        out.startAttribute("style");
        out.write( style );
        out.startAttribute("class");
        out.write( clas );
        out.closeElement();out.write(body);out.endElement("h3");
    }

    @Tag
    default void out(@Parameter(name="body") final Object body) {
        out().write(body);
    }

    @Tag
    default void ngtemplate(@Required @Parameter(name="id") String id, @Parameter(name="body") final Object body) {
        HtmlWriter out = out();
        out.startElement("script");
        out.startAttribute("type");
        out.raw("text/ng-template");
        out.startAttribute("id");
        out.write(id);
        out.closeElement();
            out.write(body);
        out.endElement("script");
    }
}

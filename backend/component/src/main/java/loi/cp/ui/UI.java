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

import com.learningobjects.cpxp.component.*;
import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.element.CustomTag;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.component.site.ItemSiteComponent;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.component.util.HtmlTemplate;
import com.learningobjects.cpxp.component.util.PropertyAccessorException;
import com.learningobjects.cpxp.component.util.ScriptToken;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.domain.DomainWebService;
import com.learningobjects.cpxp.service.script.ComponentFacade;
import com.learningobjects.cpxp.util.InternationalizationUtils;
import com.learningobjects.cpxp.util.StringUtils;
import com.typesafe.config.Config;
import loi.apm.Apm;
import loi.cp.admin.right.AdminRight;
import loi.cp.right.RightMatch;
import loi.cp.right.RightService;
import loi.cp.util.Lazy;
import org.apache.commons.lang3.BooleanUtils;

import javax.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Callable;

@Component
public class UI extends AbstractComponent implements CoreTags {
    @Inject
    private HtmlWriter out;

    @Inject
    private HttpServletRequest request;

    @Infer
    private ComponentEnvironment _environment;

    @Inject
    private Config config;

    /* Inject `out` into the cake... */

    public HtmlWriter out() { return out; }
    @Tag
    public void embeddedLayout(@Infer HttpServletRequest request,
                                      @Required @Parameter(name="content") final ComponentFacade content,
                                      @Parameter(name="title") final String title,
                                      @Parameter(name="autofocus") final String autofocus,
                                      @Parameter(name="body") final Callable body,
                                      @Parameter(name="visibility") final Boolean visibility) {
        out.startElement("html");
        out.closeElement();
          out.startElement("head");
          out.closeElement();
            out.startElement(getComponentInstance().getIdentifier() + ":layoutHead");
            out.startAttribute("content");
            out.write(content);
            out.startAttribute("title");
            out.write(title);
            out.startAttribute("autofocus");
            out.write(autofocus);
            out.closeEndElement();
          out.endElement("head");
          out.startElement("body");
          out.closeElement();
            // Content Header
            ComponentInstance component = lookupItemSiteComponent(content);
            if (ComponentUtils.test( !Boolean.TRUE.equals(request.getAttribute("noContentHeader")) && (component != null) )) {
            out.startElement(getComponentInstance().getIdentifier() + ":contentHeader");
            out.startAttribute("content");
            out.write( content );
            out.startAttribute("component");
            out.write( component );
            out.startAttribute("visibility");
            out.write( BooleanUtils.isNotFalse(visibility) );
            out.startAttribute("context");
            out.write( false );
            out.closeEndElement();
            }
            out.write(body);
          out.endElement("body");
        out.endElement("html");
    }

    @Rpc
    public void setTimeZone(@Parameter(name="timeZone") String timeZone) {
        if (StringUtils.isNotEmpty(timeZone)) {
            InternationalizationUtils.setTimeZone(TimeZone.getTimeZone(timeZone));
        }
    }

    public boolean isTimeZoneSet() {
        return InternationalizationUtils.isSessionTimeZoneSet();
    }

    public String getTimeZone() {
        TimeZone timeZone = InternationalizationUtils.getTimeZone();
        return timeZone == null ? null : timeZone.getID();
    }

    @Tag
    public HtmlTemplate pageLayout(@Parameter(name="content") ComponentFacade content,
                           @Parameter(name="title") final String title,
                           @Parameter(name="bodyClass") final String bodyClass,
                           @Parameter(name="body") final Callable body,
                           @Parameter(name="sidebar") final Callable sidebar,
                           @Parameter(name="head") final Callable head,
                           @Parameter(name="header") final Callable header,
                           @Parameter(name="autofocus") final String autofocus,
                           @Parameter(name="coursestyle") final Boolean coursestyle,
                           @Parameter(name="noheader") final Boolean noheader,
                           @Parameters final Map<String, Object> parameters) {
        HtmlTemplate template = HtmlTemplate.apply(this, "pageLayout.html")
                .bind("content", content)
                .bind("title", title)
                .bind("bodyClass", bodyClass)
                .bind("body", body)
                .bind("sidebar", sidebar)
                .bind("head", head)
                .bind("header", header)
                .bind("autofocus", autofocus)
                .bind("coursestyle", coursestyle)
                .bind("noheader", noheader);
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            template = template.bind(entry.getKey(), entry.getValue());
        }
        return template;
    }

    @Tag
    public HtmlTemplate loginForm() {
        return HtmlTemplate.apply(this, "loginForm.html");
    }

    @Tag
    public HtmlTemplate clusterLabel() {
        return HtmlTemplate.apply(this, "clusterLabel.html");
    }

    public String getAdminLink() {
        return _adminLink.get();
    }

    private Lazy<String> _adminLink = new Lazy<>(() -> {
        if (!ComponentSupport.lookupService(RightService.class).getUserHasRight(AdminRight.class, RightMatch.ANY)) {
            return null;
        }
        return ServiceContext.getContext().getService(DomainWebService.class).getAdministrationLink();
    });


    //TODO figure out right long term approach for this - CBLPROD-587

    public final String DCM_IDENTIFIER =  "loi.authoring.Authoring";

    public String getTrackingHeader() {
        return apmEnabled() ? Apm.getBrowserTimingHeader() : null;
    }

    public String getTrackingFooter() {
        return apmEnabled() ? Apm.getBrowserTimingFooter() : null;
    }


    @Tag
    public void dialog(@Required @Parameter(name="id") final String id,  @Parameter(name="title") final String title,
                              @Parameter(name="styleClass") final String styleClass, @Parameter(name="body") final Callable body) {
        out.startElement("div");
        out.startAttribute("id");
        out.write(id);
        out.raw("-mask");
        out.startAttribute("class");
        out.raw("ext-el-mask undisplayed");
        out.closeElement();out.endElement("div");
        out.startElement("div");
        out.startAttribute("id");
        out.write(id);
        out.startAttribute("class");
        out.write(styleClass);
        out.raw(" cp_dialog portlet x-hidden");
        out.closeElement();
            out.startElement("div");
            out.startAttribute("class");
            out.raw("portletHeader");
            out.closeElement();
                out.startElement("span");
                out.startAttribute("role");
                out.raw("heading");
                out.startAttribute("class");
                out.raw("portletHeaderTitle");
                out.closeElement();
                    out.write(title);
                out.endElement("span");
            out.endElement("div");
            out.startElement("div");
            out.startAttribute("class");
            out.raw("portletBody");
            out.closeElement();
                out.write(body);
            out.endElement("div");
        out.endElement("div");
    }

    @Tag
    public void prevNextPagingBar(@Required @Parameter(name="label") String label,
                                 @Required @Parameter(name="offset") Integer offset,
                                 @Required @Parameter(name="limit") Integer limit,
                                 @Required @Parameter(name="total") Integer total,
                                 @Required @Parameter(name="pagingFunction") String pagingFunction,
                                 @Parameters final Map<String, Object> parameters) {

        final int endOffset = Math.min(offset + limit, total);
        final ScriptToken pagingFunctionToken = new ScriptToken(pagingFunction);

        if (ComponentUtils.test( total > 0 )) {
        out.startElement("div");
        out.startAttribute("class");
        out.raw("cp_navigationBar contentEntryBottomNavigation ");
        out.write(ComponentUtils.dereference(parameters, "pagingClass"));
        out.closeElement();
            out.startElement("span");
            out.startAttribute("class");
            out.raw("contentEntryNavigationNext");
            out.closeElement();
                if (endOffset < total) {
                    out.startElement(getComponentInstance().getIdentifier() + ":hyperlink");
                    out.startAttribute("linkClass");
                    out.raw("pagingButton");
                    out.startAttribute("href");
                    out.raw("#");
                    out.startAttribute("onclick");
                    out.write(pagingFunctionToken);
                    out.raw("(");
                    out.write(endOffset);
                    out.raw(", ");
                    out.write(limit);
                    out.raw(")");
                    out.closeElement();
                        out.callable(new Callable<Void>() {
                            public Void call() {
                        out.raw(ComponentUtils.getMessage("tags_navigation_action_nextMultiple", getComponentDescriptor()));
                                return null;
                            }
                        });
                    out.endElement(getComponentInstance().getIdentifier() + ":hyperlink");
                }
            out.endElement("span");
            out.startElement("span");
            out.startAttribute("class");
            out.raw("contentEntryNavigationPrevious");
            out.closeElement();
                if (offset > 0) {
                    final int previousOffset = Math.max(offset - limit, 0);
                    out.startElement(getComponentInstance().getIdentifier() + ":hyperlink");
                    out.startAttribute("linkClass");
                    out.raw("pagingButton");
                    out.startAttribute("href");
                    out.raw("#");
                    out.startAttribute("onclick");
                    out.write(pagingFunctionToken);
                    out.raw("(");
                    out.write(previousOffset);
                    out.raw(", ");
                    out.write(limit);
                    out.raw(")");
                    out.closeElement();
                        out.callable(new Callable<Void>() {
                            public Void call() {
                        out.raw(ComponentUtils.getMessage("tags_navigation_action_previousMultiple", getComponentDescriptor()));
                                return null;
                            }
                        });
                    out.endElement(getComponentInstance().getIdentifier() + ":hyperlink");
                }
            out.endElement("span");
            out.startElement("span");
            out.startAttribute("class");
            out.raw("heading");
            out.closeElement();out.startElement("h2");
out.closeElement();out.write(label);out.endElement("h2");out.endElement("span");
            out.startElement("span");
            out.startAttribute("class");
            out.raw("contentEntryNavigationStatus");
            out.closeElement();out.write(ComponentUtils.formatMessage("tags_navigation_label_rangeOfCount", getComponentDescriptor(), offset+1, endOffset, total));out.endElement("span");
        out.endElement("div");
        }
    }

    @Messages({
        "$$action_first=|<",
        "$$action_previous=<",
        "$$ellipsis=...",
        "$$action_next=>",
        "$$action_last=>|",
        "$$message_showingThrough=Showing {0} through {1} of {2}"
    })
    @Tag
    public void multiPagingBar(@Parameter(name="item") final Long item,
                                @Required @Parameter(name="pageIndex") final Integer pageIndex,
                                @Required @Parameter(name="limit") final Integer limit,
                                @Required @Parameter(name="total") final Integer total,
                                @Required @Parameter(name="surroundingPages") final Integer surroundingPages,
                                @Required @Parameter(name="pagingFunction") final String pagingFunction,
                                @Parameters final Map<String, Object> parameters) {
        if (ComponentUtils.test(total > 1)) {
        out.startElement("div");
        out.startAttribute("class");
        out.raw("cp_multiPaging ");
        out.write( ComponentUtils.dereference(parameters, "pagingClass") );
        out.closeElement();
            out.startElement("span");
            out.startAttribute("style");
            out.raw("float: right");
            out.closeElement();
                final Integer validPageIndex = pageIndex < 0 ? 0: pageIndex;
                int firstResult = (validPageIndex * limit);
                long pages = (total + limit - 1) / limit; // round up
                if (pages > 1) {
                    int page = validPageIndex + 1;

                    final ScriptToken pagingFunctionToken = new ScriptToken(pagingFunction);
                    if (item != null) {
                        out.startElement(getComponentInstance().getIdentifier() + ":javascript");
                        out.closeElement();
                            out.callable(new Callable<Void>() {
                                public Void call() {out.raw("\n                            var pager = function(index) {\n                                ");
out.write(pagingFunctionToken);
out.raw("(");
out.write(item);
out.raw(", index, ");
out.write(limit);
out.raw(");\n                            }\n                        ");
                                    return null;
                                }
                            });out.endElement(getComponentInstance().getIdentifier() + ":javascript");
                    } else {
                        out.startElement(getComponentInstance().getIdentifier() + ":javascript");
                        out.closeElement();
                            out.callable(new Callable<Void>() {
                                public Void call() {out.raw("\n                            var pager = function(index) {\n                                ");
out.write(pagingFunctionToken);
out.raw("(index, ");
out.write(limit);
out.raw(");\n                            }\n                        ");
                                    return null;
                                }
                            });out.endElement(getComponentInstance().getIdentifier() + ":javascript");
                    }
                    if (ComponentUtils.test( page > 2 )) {
                    out.startElement(getComponentInstance().getIdentifier() + ":jslink");
                    out.startAttribute("linkClass");
                    out.raw("cp_pageLink");
                    out.startAttribute("function");
                    out.raw("pager");
                    out.startAttribute("param0");
                    out.write( 0 );
                    out.startAttribute("label");
                    out.write(ComponentUtils.getMessage("action_first", getComponentDescriptor()));
                    out.closeEndElement();
                    }
                    if (ComponentUtils.test( page > 1 )) {
                    out.startElement(getComponentInstance().getIdentifier() + ":jslink");
                    out.startAttribute("linkClass");
                    out.raw("cp_pageLink");
                    out.startAttribute("function");
                    out.raw("pager");
                    out.startAttribute("param0");
                    out.write(validPageIndex - 1);
                    out.startAttribute("label");
                    out.write(ComponentUtils.getMessage("action_previous", getComponentDescriptor()));
                    out.closeEndElement();
                    }

                    int lowerBound = (page > surroundingPages) ? (page - surroundingPages) : 1;
                    int upperBound = (page + surroundingPages);

                    if (lowerBound > 1) {
                        out.write(ComponentUtils.getMessage("ellipsis", getComponentDescriptor()));
                    }

                    for (int i = lowerBound; (i <= pages) && (i <= upperBound); i++) {
                        out.startElement(getComponentInstance().getIdentifier() + ":jslink");
                        out.startAttribute("linkClass");
                        out.raw("cp_pageLink ");
                        out.write( (i == page) ? "cp_currentPage" : null );
                        out.startAttribute("function");
                        out.raw("pager");
                        out.startAttribute("param0");
                        out.write(i - 1);
                        out.startAttribute("label");
                        out.write( String.valueOf(i) );
                        out.closeEndElement();
                    }

                    if (upperBound < pages) {
                        out.write(ComponentUtils.getMessage("ellipsis", getComponentDescriptor()));
                    }

                    if (ComponentUtils.test( page < pages )) {
                    out.startElement(getComponentInstance().getIdentifier() + ":jslink");
                    out.startAttribute("linkClass");
                    out.raw("cp_pageLink");
                    out.startAttribute("function");
                    out.raw("pager");
                    out.startAttribute("param0");
                    out.write(validPageIndex + 1);
                    out.startAttribute("label");
                    out.write(ComponentUtils.getMessage("action_next", getComponentDescriptor()));
                    out.closeEndElement();
                    }
                    if (ComponentUtils.test( page + 1 < pages )) {
                    out.startElement(getComponentInstance().getIdentifier() + ":jslink");
                    out.startAttribute("linkClass");
                    out.raw("cp_pageLink");
                    out.startAttribute("function");
                    out.raw("pager");
                    out.startAttribute("param0");
                    out.write(pages - 1);
                    out.startAttribute("label");
                    out.write(ComponentUtils.getMessage("action_last", getComponentDescriptor()));
                    out.closeEndElement();
                    }
                }
                if (ComponentUtils.test( total > 1 )) {
                out.startElement("div");
                out.closeElement();
                    out.write(ComponentUtils.formatMessage("message_showingThrough", getComponentDescriptor(), (firstResult + 1), (total < (firstResult + limit)) ? total : (firstResult + limit), total));
                out.endElement("div");
                }
            out.endElement("span");
            out.startElement("div");
            out.startAttribute("class");
            out.raw("allClear");
            out.closeElement();out.endElement("div");
        out.endElement("div");
        }
    }

    @Messages({
        "$$action_first=|<",
        "$$action_previous=<",
        "$$ellipsis=...",
        "$$action_next=>",
        "$$action_last=>|",
        "$$message_showingThrough=Showing {0} through {1} of {2}"
    })
    @Tag
    public void multiPagingBar2(@Parameter(name="item") final String item,
                                @Required @Parameter(name="pageIndex") final Integer pageIndex,
                                @Required @Parameter(name="limit") final Integer limit,
                                @Required @Parameter(name="total") final Integer total,
                                @Required @Parameter(name="surroundingPages") final Integer surroundingPages,
                                @Required @Parameter(name="pagingFunction") final String pagingFunction,
                                @Parameters final Map<String, Object> parameters) {
        if (ComponentUtils.test(total > 1)) {
        out.startElement("div");
        out.startAttribute("class");
        out.raw("cp_multiPaging ");
        out.write( ComponentUtils.dereference(parameters, "pagingClass") );
        out.closeElement();
            out.startElement("span");
            out.startAttribute("style");
            out.raw("float: right");
            out.closeElement();
                final Integer validPageIndex = pageIndex < 0 ? 0: pageIndex;
                int firstResult = (validPageIndex * limit);
                long pages = (total + limit - 1) / limit; // round up
                if (pages > 1) {
                    int page = validPageIndex + 1;

                    final ScriptToken pagingFunctionToken = new ScriptToken(pagingFunction);
                    if (item != null) {
                        out.startElement(getComponentInstance().getIdentifier() + ":javascript");
                        out.closeElement();
                            out.callable(new Callable<Void>() {
                                public Void call() {out.raw("\n                            var pager = function(index) {\n                                ");
out.write(pagingFunctionToken);
out.raw("(");
out.write(item);
out.raw(", index, ");
out.write(limit);
out.raw(");\n                            }\n                        ");
                                    return null;
                                }
                            });out.endElement(getComponentInstance().getIdentifier() + ":javascript");
                    } else {
                        out.startElement(getComponentInstance().getIdentifier() + ":javascript");
                        out.closeElement();
                            out.callable(new Callable<Void>() {
                                public Void call() {out.raw("\n                            var pager = function(index) {\n                                ");
out.write(pagingFunctionToken);
out.raw("(index, ");
out.write(limit);
out.raw(");\n                            }\n                        ");
                                    return null;
                                }
                            });out.endElement(getComponentInstance().getIdentifier() + ":javascript");
                    }
                    if (ComponentUtils.test( page > 2 )) {
                    out.startElement(getComponentInstance().getIdentifier() + ":jslink");
                    out.startAttribute("linkClass");
                    out.raw("cp_pageLink");
                    out.startAttribute("function");
                    out.raw("pager");
                    out.startAttribute("param0");
                    out.write( 0 );
                    out.startAttribute("label");
                    out.write(ComponentUtils.getMessage("action_first", getComponentDescriptor()));
                    out.closeEndElement();
                    }
                    if (ComponentUtils.test( page > 1 )) {
                    out.startElement(getComponentInstance().getIdentifier() + ":jslink");
                    out.startAttribute("linkClass");
                    out.raw("cp_pageLink");
                    out.startAttribute("function");
                    out.raw("pager");
                    out.startAttribute("param0");
                    out.write(validPageIndex - 1);
                    out.startAttribute("label");
                    out.write(ComponentUtils.getMessage("action_previous", getComponentDescriptor()));
                    out.closeEndElement();
                    }

                    int lowerBound = (page > surroundingPages) ? (page - surroundingPages) : 1;
                    int upperBound = (page + surroundingPages);

                    if (lowerBound > 1) {
                        out.write(ComponentUtils.getMessage("ellipsis", getComponentDescriptor()));
                    }

                    for (int i = lowerBound; (i <= pages) && (i <= upperBound); i++) {
                        out.startElement(getComponentInstance().getIdentifier() + ":jslink");
                        out.startAttribute("linkClass");
                        out.raw("cp_pageLink ");
                        out.write( (i == page) ? "cp_currentPage" : null );
                        out.startAttribute("function");
                        out.raw("pager");
                        out.startAttribute("param0");
                        out.write(i - 1);
                        out.startAttribute("label");
                        out.write( String.valueOf(i) );
                        out.closeEndElement();
                    }

                    if (upperBound < pages) {
                        out.write(ComponentUtils.getMessage("ellipsis", getComponentDescriptor()));
                    }

                    if (ComponentUtils.test( page < pages )) {
                    out.startElement(getComponentInstance().getIdentifier() + ":jslink");
                    out.startAttribute("linkClass");
                    out.raw("cp_pageLink");
                    out.startAttribute("function");
                    out.raw("pager");
                    out.startAttribute("param0");
                    out.write(validPageIndex + 1);
                    out.startAttribute("label");
                    out.write(ComponentUtils.getMessage("action_next", getComponentDescriptor()));
                    out.closeEndElement();
                    }
                    if (ComponentUtils.test( page + 1 < pages )) {
                    out.startElement(getComponentInstance().getIdentifier() + ":jslink");
                    out.startAttribute("linkClass");
                    out.raw("cp_pageLink");
                    out.startAttribute("function");
                    out.raw("pager");
                    out.startAttribute("param0");
                    out.write(pages - 1);
                    out.startAttribute("label");
                    out.write(ComponentUtils.getMessage("action_last", getComponentDescriptor()));
                    out.closeEndElement();
                    }
                }
                if (ComponentUtils.test( total > 1 )) {
                out.startElement("div");
                out.closeElement();
                    out.write(ComponentUtils.formatMessage("message_showingThrough", getComponentDescriptor(), (firstResult + 1), (total < (firstResult + limit)) ? total : (firstResult + limit), total));
                out.endElement("div");
                }
            out.endElement("span");
            out.startElement("div");
            out.startAttribute("class");
            out.raw("allClear");
            out.closeElement();out.endElement("div");
        out.endElement("div");
        }
    }

    @Tag
    @Messages({ "$$contentTemplate_action_preview=Preview",
                "$$contentTemplate_action_add=Add" })
    public void xxxyyy(
            @Required @Parameter(name="item") final Facade item,
            @Required @Parameter(name="image") final String image,
            @Required @Parameter(name="name") final String name,
            @Required @Parameter(name="description") final String description,
            @Parameter(name="previewFunction") final String previewFunction) {
        out.startElement("div");
        out.startAttribute("class");
        out.raw("addContent");
        out.startAttribute("id");
        out.raw("addContent-");
        out.write( ComponentUtils.dereference(item, "id") );
        out.startAttribute("onclick");
        out.raw("expandTemplate(");
        out.write( ComponentUtils.dereference(item, "id") );
        out.raw(", 'Component')");
        out.startAttribute("tabindex");
        out.raw("1");
        out.closeElement();
            out.startElement("img");
            out.startAttribute("src");
            out.write( image );
            out.closeEndElement();
            out.startElement("div");
            out.startAttribute("class");
            out.raw("templateDescription");
            out.closeElement();
                if (ComponentUtils.test( previewFunction )) {
                out.startElement("div");
                out.startAttribute("class");
                out.raw("addContentPreview");
                out.closeElement();
                    out.startElement("a");
                    out.startAttribute("href");
                    out.raw("#");
                    out.startAttribute("onclick");
                    out.write( previewFunction );
                    out.raw("(this);");
                    out.closeElement();out.write(ComponentUtils.getMessage("contentTemplate_action_preview", getComponentDescriptor()));out.endElement("a");
                out.endElement("div");
                }
                out.startElement("h3");
                out.startAttribute("class");
                out.raw("templateName");
                out.closeElement();out.write(name);out.endElement("h3");
                out.startElement("div");
                out.closeElement();out.write(description);out.endElement("div");
            out.endElement("div");
            out.startElement("form");
            out.startAttribute("id");
            out.raw("form-");
            out.write( ComponentUtils.dereference(item, "id") );
            out.startAttribute("style");
            out.raw("display:none");
            out.closeElement();
              out.startElement("input");
              out.startAttribute("id");
              out.raw("addComponent-");
              out.write( ComponentUtils.dereference(item, "id") );
              out.startAttribute("type");
              out.raw("button");
              out.startAttribute("value");
              out.write(ComponentUtils.getMessage("contentTemplate_action_add", getComponentDescriptor()));
              out.startAttribute("class");
              out.raw("cp_button");
              out.startAttribute("onclick");
              out.raw("addTemplatedComponentContent(");
              out.write( ComponentUtils.dereference(item, "id") );
              out.raw(");");
              out.closeEndElement();
            out.endElement("form");
            out.startElement("div");
            out.startAttribute("role");
            out.raw("presentation");
            out.startAttribute("class");
            out.raw("allClear");
            out.closeElement();out.endElement("div");
        out.endElement("div");
    }

    @Tag
    public void pagePlain(@Required @Parameter(name="title") final String title,
                          @Parameter(name="onload") final String onload,
                          @Required @Parameter(name="body") final Callable body) {
        out.raw("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
        out.startElement("html");
        out.closeElement();
          out.startElement("head");
          out.closeElement();
            out.startElement("meta");
            out.startAttribute("http-equiv");
            out.raw("Content-Type");
            out.startAttribute("content");
            out.raw("text/html;charset=UTF-8");
            out.closeEndElement();
            out.startElement("title");
            out.closeElement();out.write(title);out.endElement("title");
            out.startElement("link");
            out.startAttribute("rel");
            out.raw("stylesheet");
            out.startAttribute("type");
            out.raw("text/css");
            out.startAttribute("href");
            out.raw("/static/static/styles/underground.css");
            out.closeEndElement();
          out.endElement("head");
          out.startElement("body");
          out.startAttribute("class");
          out.raw("cp_plainPage");
          out.startAttribute("onload");
          out.write( onload );
          out.closeElement();
            out.startElement("div");
            out.startAttribute("class");
            out.raw("cp_pageBody");
            out.closeElement();
              out.startElement("div");
              out.startAttribute("class");
              out.raw("cp_pageBodyInner");
              out.closeElement();
                out.startElement("div");
                out.startAttribute("class");
                out.raw("cp_pageBodyInnerMost");
                out.closeElement();
                  out.write(body);
                out.endElement("div");
              out.endElement("div");
            out.endElement("div");
          out.endElement("body");
        out.endElement("html");
    }

    @Tag
    public void pageTitle(@Required @Parameter(name="label") final String label,
                          @Parameter(name="body") final Callable body) {
        out.startElement("div");
        out.startAttribute("class");
        out.raw("pageTitle");
        out.closeElement();
            out.startElement("span");
            out.startAttribute("class");
            out.raw("titleLabel");
            out.closeElement();out.write(label);out.endElement("span");
            out.write(body);
        out.endElement("div");
    }

    private ComponentInstance lookupItemSiteComponent(ComponentFacade content) {
        if (content == null) {
            return null;
        } else if (content.getIdentifier() != null) {
            //DE385 added try/catch
            try{
                return ComponentSupport.getComponent(content, content.getParentId());
            } catch (PropertyAccessorException paex) {
                return null;
            }
        } else {
            DelegateDescriptor descriptor = _environment.getRegistry().lookup(ItemSiteComponent.class, content.getItemType(), null, null);
            if (descriptor != null) {
                return descriptor.getComponent().getInstance(_environment, content, content.getParentId());
            } else {
                return null;
            }
        }
    }

    @Tag
    public CustomTag coalesce(@Parameter Callable body) {
        return new CoalesceTag(body);
    }

    private boolean apmEnabled() {
        return config.getBoolean("apm.enabled");
    }
}

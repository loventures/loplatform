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

package loi.cp.zip;

import com.learningobjects.cpxp.WebContext;
import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.BaseHtmlWriter;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.HtmlWriter;
import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.query.ApiQuerySupport;
import com.learningobjects.cpxp.component.template.LohtmlTemplate;
import com.learningobjects.cpxp.component.template.RenderContext;
import com.learningobjects.cpxp.component.web.*;
import com.learningobjects.cpxp.filter.SendFileFilter;
import com.learningobjects.cpxp.service.attachment.AttachmentFacade;
import com.learningobjects.cpxp.service.attachment.AttachmentWebService;
import com.learningobjects.cpxp.service.mime.MimeWebService;
import com.learningobjects.cpxp.util.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import loi.cp.attachment.AttachmentComponent;
import loi.cp.right.RightService;
import org.apache.commons.lang3.BooleanUtils;
import scala.Option;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.File;
import java.net.URLDecoder;

@Component(alias = "loi.cp.zip.ZipSite")
public class ZipSiteImpl extends AbstractComponent implements ZipSite {
    @Instance
    private ZipSiteFacade _site;

    // TODO: now broken
    @Configuration(label = "$$field_interpreted=Interpreted", order = 10)
    private Boolean _interpreted;

    @Inject
    private AttachmentWebService _attachmentWebService;

    @Inject
    private MimeWebService _mimeWebService;

    @Inject
    private RightService _rightService;

    @Inject
    private HttpServletRequest request;

    @Inject
    private HttpServletResponse response;

    @Inject
    private WebContext wc;

    @Override
    public Long getId() {
        return _site.getId();
    }

    @Override
    public String getName() {
        return _site.getName();
    }

    @Override
    public String getPath() {
        return _site.getUrl();
    }

    @Override
    public boolean getDisabled() {
        return BooleanUtils.isTrue(_site.getDisabled());
    }

    @Override
    public AttachmentComponent getSite() {
        return ComponentSupport.get(_site.getActiveAttachment(), AttachmentComponent.class);
    }

    @Override
    public ApiQueryResults<AttachmentComponent> getRevisions(ApiQuery aq) {
        return ApiQuerySupport.query(getId(), aq, AttachmentComponent.class);
    }

    @Override
    public Option<AttachmentComponent> getRevision(long id) {
        AttachmentFacade facade = _site.getAttachment(id);
        return Option.apply(ComponentSupport.get(facade, AttachmentComponent.class));
    }

    @Override
    public WebResponse deleteRevision(long id) {
        if (_site.getActiveAttachment().getId() == id) {
            return ErrorResponse.methodNotAllowed();
        }

        AttachmentFacade facade = _site.getAttachment(id);
        if (facade == null) {
            return ErrorResponse.notFound();
        }

        facade.delete();
        return NoContentResponse.instance();
    }

    @Override
    public WebResponse render(Option<Long> revision) {
        AttachmentFacade facade =
          revision.isDefined()
            ? _site.getAttachment(revision.get())
            : _site.getActiveAttachment();
        if (facade == null) {
            return ErrorResponse.notFound();
        } else {
            String path =
              (revision.isEmpty() && !getDisabled())
                ? getPath() // redirect to "real" url if possible
                : ZipSiteRenderServlet.Path$.MODULE$.apply(getId(), facade.getId(), "");

            return RedirectResponse.temporary(path);
        }
    }

    @Override
    @Nonnull
    public WebResponse renderSite(String view) throws Exception {
        if (getDisabled()) {
            return ErrorResponse.notFound();
        }

        String uri = URLDecoder.decode(request.getRequestURI(), "UTF-8");
        if (uri.equals(_site.getUrl())) {
            String qs = request.getQueryString();
            qs = (qs != null) ? "?" + qs : "";
            return RedirectResponse.permanent(uri + "/" + qs);
        }
        notFound(request);
        return NoResponse.instance(); // YUCK, notFound should return AttachmentResponse or equivalent
    }

    @Rpc(method = Method.GET)
    @Direct
    public void notFound(@Infer HttpServletRequest request) throws Exception {
        String path = StringUtils.removeStart(URLDecoder.decode(request.getRequestURI(), "UTF-8"), _site.getUrl());
        AttachmentFacade attachment = _site.getActiveAttachment();
        serve(attachment, path, false);
    }

    public void serve(AttachmentFacade attachment, String path, boolean preview) throws Exception {
        if (attachment == null || unauthorized()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        final String prefix = attachment.asFacade(ZipAttachmentFacade.class).getPathPrefix().orElse("");
        String relative = prefix + StringUtils.removeStart(path, "/");
        if ("".equals(relative) || relative.endsWith("/")) {
            relative = relative + "index.html";
        }
        File file = _attachmentWebService.getZipAttachmentFile(attachment.getId(), relative);
        if ((file == null) || !file.exists()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (Boolean.TRUE.equals(_interpreted) && relative.endsWith(".html")) {
            HtmlWriter out = wc.getHtmlWriter();
            if (out != null) { // TODO: KILL ME, exists now for the renderSite version
                RenderContext context = new RenderContext(getComponentDescriptor(), null, out, null, null);
                LohtmlTemplate.getInstance(file.toURL()).render(context);
            } else {
                response.setContentType(MimeUtils.MIME_TYPE_TEXT_HTML + MimeUtils.CHARSET_SUFFIX_UTF_8);
                out = new BaseHtmlWriter(response.getWriter());
                wc.setHtmlWriter(out);
                try {
                    RenderContext context = new RenderContext(getComponentDescriptor(), null, out, null, null);
                    LohtmlTemplate.getInstance(file.toURL()).render(context);
                    out.close();
                } finally {
                    wc.setHtmlWriter(null);
                }
            }
        } else {
            FileInfo info = new LocalFileInfo(file);
            info.setLastModified(attachment.getCreated());
            info.setContentType(_mimeWebService.getMimeType(relative));
            if (!relative.endsWith(".html")) {
                info.setDoCache(true);
                info.setExpires(DateUtils.Unit.year.getValue());
            }
            request.setAttribute(SendFileFilter.REQUEST_ATTRIBUTE_SEND_FILE, info);
        }
    }

    private boolean unauthorized() {
        // redirecting to login exceeds my appetite
        //
        // there is no UI for the viewRightClassName String, you must add the data
        // property in sys/storage on the zip site item. This is because some
        // staff possess the Manage Zip Sites right. They could change it and make the
        // zip site public. The horror.
        //
        // add it under the key "json"
        // set this value for example: { "viewRightClassName" : "loi.cp.admin.right.ViewReportingDatabaseDocumentationRight" }
        var rightClassName = _site.getViewRightClassName();
        if (rightClassName == null) {
            return false; // zip site is public
        } else {
            var right = _rightService.getRight(rightClassName);
            return !_rightService.getUserRights().contains(right);
        }

    }
}

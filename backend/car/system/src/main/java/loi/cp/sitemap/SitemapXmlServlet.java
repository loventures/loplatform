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

package loi.cp.sitemap;

import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.web.AbstractComponentServlet;
import com.learningobjects.cpxp.component.web.ServletBinding;
import com.learningobjects.cpxp.filter.SendFileFilter;
import com.learningobjects.cpxp.service.attachment.AttachmentFacade;
import com.learningobjects.cpxp.service.attachment.AttachmentWebService;
import com.learningobjects.cpxp.service.name.NameService;
import com.learningobjects.cpxp.util.DateUtils;
import com.learningobjects.cpxp.util.FileInfo;
import com.learningobjects.cpxp.util.MimeUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.inject.Inject;

@Component
@ServletBinding(path = "/sitemap.xml")
public class SitemapXmlServlet extends AbstractComponentServlet {
    @Inject
    private NameService _nameService;

    @Inject
    private AttachmentWebService _attachmentWebService;

    @Override
    public void get(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        Long id = _nameService.getItemId("/sitemap.xml");
        if (id == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        AttachmentFacade attachment = _attachmentWebService.getRawAttachment(id);
        FileInfo info = _attachmentWebService.getAttachmentBlob(attachment.getId());
        info.setLastModified(attachment.getCreated());
        response.setContentType(MimeUtils.MIME_TYPE_TEXT_XML + MimeUtils.CHARSET_SUFFIX_UTF_8);
        info.setDoCache(true);
        info.setExpires(DateUtils.Unit.day.getValue());
        info.setNoRedirect(true);
        request.setAttribute(SendFileFilter.REQUEST_ATTRIBUTE_SEND_FILE, info);
    }
}

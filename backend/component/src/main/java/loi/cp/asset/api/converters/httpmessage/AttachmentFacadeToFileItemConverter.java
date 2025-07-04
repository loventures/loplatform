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

package loi.cp.asset.api.converters.httpmessage;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.RequestBody;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.cpxp.component.web.converter.ConvertOptions;
import com.learningobjects.cpxp.component.web.converter.HttpMessageConverter;
import com.learningobjects.cpxp.component.web.exception.HttpMessageNotReadableException;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.service.attachment.AttachmentFacade;
import com.learningobjects.de.web.MediaType;
import loi.cp.asset.base.service.AttachmentService;

import javax.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

@Component
public class AttachmentFacadeToFileItemConverter extends AbstractComponent implements HttpMessageConverter<AttachmentFacade> {

    @Inject
    private AttachmentService _attachmentService;

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(MediaType.ALL);
    }

    @Override
    public boolean canRead(Type target, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(final Object value, MediaType mediaType) {
        return value instanceof UploadInfo;
    }

    @Override
    public AttachmentFacade read(final RequestBody requestBody, WebRequest request, Type target) {
        throw new UnsupportedOperationException("AttachmentFacade reading not implemented");
    }

    @Override
    public void write(AttachmentFacade source, ConvertOptions options, HttpServletRequest request, HttpServletResponse response) {
        try {
            // we have a filter do the downloading for some reason
            _attachmentService.triggerDownload(source, request);
        } catch (Exception e) {
            throw new HttpMessageNotReadableException("could not read attachment from request body", e);
        }
    }
}

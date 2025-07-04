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
import com.learningobjects.cpxp.controller.upload.Uploads;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.de.web.MediaType;
import loi.cp.asset.base.service.AttachmentService;

import javax.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

@Component
public class AnyToUploadInfoConverter extends AbstractComponent implements HttpMessageConverter<UploadInfo> {

    @Inject
    private AttachmentService _attachmentService;

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(MediaType.ALL);
    }

    @Override
    public boolean canRead(Type targetType, MediaType mediaType) {
        return targetType instanceof Class && UploadInfo.class.isAssignableFrom((Class<?>) targetType);
    }

    @Override
    public boolean canWrite(final Object value, MediaType mediaType) {
        return false;
    }

    @Override
    public UploadInfo read(final RequestBody requestBody, WebRequest request, Type targetType) {
        try {
            // support retrieving pre-staged uploads.. do this with multipart too when we support multiparts
            if (MediaType.APPLICATION_FORM_URLENCODED.equals(request.getContentType())) {
                return Uploads.retrieveUpload(request.getRawRequest().getParameter(StringUtils.defaultIfEmpty(requestBody.part(), "guid")));
            } else {
                return _attachmentService.receive(request.getRawRequest(), requestBody.part());
            }
        } catch (Exception e) {
            throw new HttpMessageNotReadableException("could not read attachment from request body", e);
        }
    }

    @Override
    public void write(UploadInfo source, ConvertOptions options, HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("UploadInfo writing not implemented");
    }
}

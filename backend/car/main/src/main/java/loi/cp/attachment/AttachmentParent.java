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

package loi.cp.attachment;

import com.learningobjects.cpxp.component.AbstractComponent;
import com.learningobjects.cpxp.component.ComponentSupport;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.Instance;
import com.learningobjects.cpxp.component.annotation.RequestBody;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.query.ApiQuerySupport;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.cpxp.controller.upload.Uploader;
import com.learningobjects.cpxp.service.attachment.AttachmentWebService;
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import org.apache.commons.collections4.IterableUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@SuppressWarnings("unused")
public class AttachmentParent extends AbstractComponent implements AttachmentParentComponent {
    @Inject
    private AttachmentWebService _attachmentWebService;

    @Instance
    private AttachmentParentFacade _context;

    @Override
    public ApiQueryResults<AttachmentComponent> getAttachments(ApiQuery query) {
        QueryBuilder qb = _context.queryAttachments();
        return ApiQuerySupport.query(qb, query, AttachmentComponent.class);
    }

    private List<AttachmentComponent> getAttachments(List<Long> ids) {
        return ComponentSupport.getById(ids, AttachmentComponent.class);
    }

    @Override
    public Optional<AttachmentComponent> getAttachment(Long id) {
        return _context.getAttachment(id);
    }


    @Override
    public void deleteAttachment(Long id) {
        Optional<AttachmentComponent> attachment = getAttachment(id);
        if (!attachment.isPresent()) {
            throw new ResourceNotFoundException("Unknown attachment: " + id);
        }
        attachment.get().delete();
    }

    @Override
    public void deleteAttachments(List<Long> ids) {
        Map<Long, AttachmentComponent> attachmentMap =
          getAttachments(ids).stream().collect(Collectors.toMap(AttachmentComponent::getId, Function.identity()));

        ids.stream().forEach(id -> {
            if (!attachmentMap.containsKey(id)) {
                throw new ResourceNotFoundException("Unknown attachment: " + id);
            }
            attachmentMap.get(id).delete();
        });
    }

    @Override
    public List<AttachmentComponent> uploadAttachments(WebRequest request) throws Exception {
        try (Uploader uploader = Uploader.parse(request.getRawRequest())) {
            return attachUploads(new Uploads(IterableUtils.toList(uploader.getUploads())));
        }
    }

    @Override
    public List<AttachmentComponent> attachUploads(@RequestBody Uploads uploads) {
        return uploads.getUploads().stream().map(upload -> {
            Long id = _attachmentWebService.createAttachment(_context.getId(), upload);
            AttachmentComponent attachment = ComponentSupport.get(id, AttachmentComponent.class);
            for (String size : upload.getThumbnailSizes()) {
                attachment.view(false, false, size); // trigger thumbnail generation, ignore the result
            }
            return attachment;
        }).collect(Collectors.toList());
    }

    @Override
    public AttachmentComponent addAttachment(UploadInfo uploadInfo) {
        try {
            Long attachmentId = _attachmentWebService.createAttachment(_context.getId(), uploadInfo);
            return ComponentSupport.get(attachmentId, AttachmentComponent.class);
        } finally {
            uploadInfo.destroy();
        }
    }
}

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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.learningobjects.cpxp.component.ComponentDecorator;
import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.query.ApiQuery;
import com.learningobjects.cpxp.component.query.ApiQueryResults;
import com.learningobjects.cpxp.component.web.ApiRootComponent;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.component.web.WebRequest;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.de.authorization.Secured;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

// NOTE: Do not merge this with AttachmentViewComponent, else this writable API will be a
// valid instance of a read-only API

@SuppressWarnings("unused")
@Controller(value = "attachments")
@RequestMapping(path = "attachments")
@Secured // Parent enforces read access control
public interface AttachmentParentComponent extends ApiRootComponent, ComponentDecorator {
    /**
     * Get all attachment children of this component.
     */
    @RequestMapping(method = Method.GET)
    ApiQueryResults<AttachmentComponent> getAttachments(@MaxLimit(100) ApiQuery query);

    /**
     * Get a specific attachment child of this component.
     */
    @RequestMapping(path = "{id}", method = Method.GET)
    Optional<AttachmentComponent> getAttachment(@PathVariable("id") Long id);

    /**
     * Delete a specific attachment child of this component.
     */
    @Secured // Parent enforces write access control
    @RequestMapping(path = "{id}", method = Method.DELETE)
    void deleteAttachment(@PathVariable("id") Long id);

    void deleteAttachments(List<Long> id);

    /**
     * Upload one or more attachments to this component. Both single-part
     * and multipart uploads are supported.
     */
    @Secured // Parent enforces write access control
    @RequestMapping(path = "upload", method = Method.POST)

    public List<AttachmentComponent> uploadAttachments(WebRequest request) throws Exception;

    /**
     * Upload one attachment to this component.  Used internally when WebRequest is handled by another API
     * @param uploadInfo
     * @return
     */
    public AttachmentComponent addAttachment(UploadInfo uploadInfo);

    /**
     * Attach a series of uploads to this component. If any upload includes
     * thumbnail sizing, schedule those thumbnails to be generated.
     */
    @Secured // Parent enforces write access control
    @RequestMapping(path = "attach", method = Method.POST)
    List<AttachmentComponent> attachUploads(@RequestBody Uploads uploads);

    class Uploads {
        private final List<UploadInfo> uploads;

        @JsonCreator
        public Uploads(@Nonnull @JsonProperty("uploads") final List<UploadInfo> uploads) {
            this.uploads = uploads;
        }

        @Nonnull
        public List<UploadInfo> getUploads() {
            return uploads;
        }
    }
}

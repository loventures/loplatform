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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.component.web.WebResponse;
import com.learningobjects.cpxp.service.attachment.AttachmentConstants;
import com.learningobjects.cpxp.service.attachment.ResourceDTO;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.document.DocumentConstants;
import com.learningobjects.de.web.DeletableEntity;
import com.learningobjects.de.web.Queryable;
import com.learningobjects.de.web.QueryableId;
import loi.cp.user.Profile;

import java.util.Date;
import java.util.Optional;

@Schema("attachment")
@ItemMapping(value = AttachmentConstants.ITEM_TYPE_ATTACHMENT, singleton = true)
public interface AttachmentComponent extends ComponentInterface, QueryableId, DeletableEntity {

    /**
     * A permanent *client*-generated handle
     */
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Queryable(dataType = AttachmentConstants.DATA_TYPE_ATTACHMENT_CLIENT_ID)
    String getClientId();
    void setClientId(String clientId);

    @JsonProperty
    @Queryable(dataType = AttachmentConstants.DATA_TYPE_ATTACHMENT_FILE_NAME)
    String getFileName();

    @JsonProperty
    String getMimeType();

    @JsonProperty
    long getVersion();

    /**
     * The creation time..
     */
    @JsonProperty
    @Queryable(dataType = DataTypes.DATA_TYPE_CREATE_TIME)
    Date getCreateTime();

    @JsonProperty
    @Queryable(dataType = DocumentConstants.DATA_TYPE_CREATOR)
    Profile getCreator();

    /**
     * The size in bytes.
     */
    @JsonProperty
    @Queryable(dataType = AttachmentConstants.DATA_TYPE_ATTACHMENT_SIZE)
    Long getSize();

    /**
     * For image attachments, the height in pixels.
     */
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Queryable(dataType = AttachmentConstants.DATA_TYPE_ATTACHMENT_HEIGHT)
    Long getHeight();

    /**
     * For image attachments, the width in pixels.
     */
    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Queryable(dataType = AttachmentConstants.DATA_TYPE_ATTACHMENT_WIDTH)
    Long getWidth();

    ResourceDTO toDTO();

    /**
     * Return the actual attachment to the browser.
     *
     * @param download Use Content-Disposition header to force a download
     * rather than inline display.
     *
     * @param direct Force direct return of the response; inhibits use of
     * a CDN.
     *
     * @param size a logical thumbnail size - small, medium, large, xlarge
     */
    @RequestMapping(path = "view", method = Method.GET, csrf = false)
    WebResponse view(@QueryParam(required = false) Boolean download,
                     @QueryParam(required = false) Boolean direct,
                     @MatrixParam(required = false) String size);

    /** Internal method for viewing attachments that may bypass the standard safety
     * controls to prevent serving XSS-vulnerable MIME types.
     *
     * direct ironically means download directly from the appserver. Otherwise it is
     * a tossup between routing through the appserver and getting from S3. Now,
     * noDirect lets you mandate straight from S3. Really direct should be a ternary,
     * and it is, but the consequences of changing now...*/
    WebResponse viewInternal(Boolean download, Boolean direct, String size, boolean safe, boolean noDirect);

    void delete();
    void lock(boolean pessimistic);

    Long getParentId();

    Optional<Long> getReference();

    enum ThumbnailSize {
        small(32), medium(64), large(128), xlarge(256);

        private final int size;

        ThumbnailSize(final int size) {
            this.size = size;
        }

        public int getSize() {
            return size;
        }

        public static Optional<ThumbnailSize> parse(String size) {
            try {
                return Optional.of(ThumbnailSize.valueOf(size));
            } catch (Exception ex) {
                return Optional.empty();
            }
        }
    }
}

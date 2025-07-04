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

package loi.cp.context.accesscode;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.learningobjects.cpxp.component.annotation.QueryParam;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.controller.upload.UploadInfo;
import com.learningobjects.de.authorization.Secured;
import loi.cp.accesscode.AccessCodeAdminRight;
import loi.cp.accesscode.AccessCodeBatchComponent;
import loi.cp.accesscode.CsvValidationResponse;

@Schema(EnrollAccessCodeBatchComponent.SCHEMA)
public interface EnrollAccessCodeBatchComponent extends AccessCodeBatchComponent {

    public static final String SCHEMA = "enrollmentAccessCodeBatch";

    @JsonProperty
    public Long getRole();

    @JsonProperty
    public Long getCourseId();

    @RequestMapping(path = "generate", method = Method.POST)
    @Secured(AccessCodeAdminRight.class)
    public void generateBatch(@QueryParam String prefix, @QueryParam Long quantity);

    @RequestMapping(path = "import", method = Method.POST, async = true)
    @Secured(AccessCodeAdminRight.class)
    public void importBatch(@QueryParam Boolean skipHeader, @QueryParam("upload") UploadInfo uploadInfo);

    // This is a funny "static" method. No like.
    @RequestMapping(path = "validateUpload", method = Method.GET)
    public CsvValidationResponse validateUpload(@QueryParam("upload") UploadInfo uploadInfo);
}

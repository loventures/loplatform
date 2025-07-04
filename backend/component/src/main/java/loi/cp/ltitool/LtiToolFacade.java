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

package loi.cp.ltitool;

import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.component.misc.LtiToolConstants;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.script.ComponentFacade;
import com.learningobjects.cpxp.service.attachment.AttachmentFacade;
import com.learningobjects.cpxp.controller.upload.UploadInfo;

@FacadeItem(LtiToolConstants.ITEM_TYPE_LTI_TOOL)
public interface LtiToolFacade extends ComponentFacade {

    @FacadeData(LtiToolConstants.DATA_TYPE_LTI_TOOL_ID)
    String getToolId();
    void setToolId(String toolId);

    @FacadeData(LtiToolConstants.DATA_TYPE_LTI_TOOL_CONFIGURATION)
    LtiToolConfiguration getLtiConfiguration();
    void setLtiConfiguration(LtiToolConfiguration ltiConfiguration);

    @FacadeData(DataTypes.DATA_TYPE_DISABLED)
    Boolean getDisabled();
    void setDisabled(Boolean disabled);

    @FacadeData(value = DataTypes.DATA_TYPE_ICON)
    AttachmentFacade getIcon();
    void setIcon(UploadInfo upload);

    @FacadeData(LtiToolConstants.DATA_TYPE_LTI_TOOL_BRANCHED)
    Boolean getCopyBranchSection();
    void setCopyBranchSection(Boolean copyBranchSection);

}

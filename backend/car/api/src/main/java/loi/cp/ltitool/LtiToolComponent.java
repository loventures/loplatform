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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.ItemMapping;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.service.component.misc.LtiToolConstants;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.de.web.Queryable;
import com.learningobjects.de.web.QueryableId;

@ItemMapping(value = LtiToolConstants.ITEM_TYPE_LTI_TOOL, singleton = true)
@Schema("ltiTool")
public interface LtiToolComponent extends ComponentInterface, QueryableId {

    @JsonProperty
    @Queryable(dataType = DataTypes.DATA_TYPE_NAME, traits = Queryable.Trait.CASE_INSENSITIVE)
    String getName();
    void setName(String name);

    @JsonProperty
    String getToolId();
    void setToolId(String toolId);

    @JsonProperty
    Boolean getDisabled();
    void setDisabled(Boolean disabled);

    @JsonProperty
    LtiToolConfiguration getLtiConfiguration();
    void setLtiConfiguration(LtiToolConfiguration configuration);

    @JsonProperty
    Boolean isCopyBranchSection();
    void setCopyBranchSection(Boolean copyBranchSection);

    void delete();
}

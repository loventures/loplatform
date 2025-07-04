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

package loi.cp.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.RestfulComponent;
import com.learningobjects.cpxp.component.annotation.ItemMapping;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.service.component.ComponentConstants;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.integration.IntegrationConstants;
import com.learningobjects.de.web.Queryable;

import javax.validation.groups.Default;

import static com.learningobjects.de.web.Queryable.Trait.CASE_INSENSITIVE;
import static com.learningobjects.de.web.Queryable.Trait.NOT_SORTABLE;

@Schema("connector")
@ItemMapping(IntegrationConstants.ITEM_TYPE_SYSTEM)
public interface SystemComponent<A extends SystemComponent<A>> extends ComponentInterface, RestfulComponent<A> {
    String PROPERTY_IMPLEMENTATION = "implementation";

    @JsonProperty
    @Queryable(dataType = IntegrationConstants.DATA_TYPE_SYSTEM_ID,
            traits = CASE_INSENSITIVE)
    String getSystemId();

    @JsonProperty
    @Queryable(dataType = IntegrationConstants.DATA_TYPE_SYSTEM_NAME,
            traits = CASE_INSENSITIVE)
    String getName();

    @JsonProperty
    @Queryable(dataType = DataTypes.DATA_TYPE_DISABLED)
    Boolean getDisabled();
    void setDisabled(Boolean disabled);

    @JsonView(Default.class)
    @Queryable(dataType = ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER,
               handler = SystemImplementationHandler.class,
               traits = NOT_SORTABLE)
    String getImplementation();

    @JsonProperty
    String getKey();
}

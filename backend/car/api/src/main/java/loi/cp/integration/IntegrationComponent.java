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
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.Controller;
import com.learningobjects.cpxp.component.annotation.ItemMapping;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.component.web.Method;
import com.learningobjects.cpxp.service.integration.IntegrationConstants;
import com.learningobjects.de.authorization.Secured;
import com.learningobjects.de.web.DeletableEntity;
import com.learningobjects.de.web.Queryable;
import com.learningobjects.de.web.QueryableId;
import loi.cp.admin.right.IntegrationAdminRight;

import static com.learningobjects.de.web.Queryable.Trait.CASE_INSENSITIVE;

// dataSource is deprecated and not supported by this API

@Controller(value = "integration", category = Controller.Category.CONTEXTS)
@Schema("integration")
@ItemMapping(value = IntegrationConstants.ITEM_TYPE_INTEGRATION, singleton = true)
public interface IntegrationComponent extends ComponentInterface, QueryableId, DeletableEntity {
    public static final String PROPERTY_CONNECTOR = "connector";

    @JsonProperty
    @Queryable(dataType = IntegrationConstants.DATA_TYPE_UNIQUE_ID, traits = CASE_INSENSITIVE)
    public String getUniqueId();
    public void setUniqueId(String uniqueId);

    @JsonProperty("connector_id")
    @Queryable(dataType = IntegrationConstants.DATA_TYPE_EXTERNAL_SYSTEM)
    public Long getSystemId();

    @RequestMapping(path = PROPERTY_CONNECTOR, method = Method.GET)
    @Queryable(name = PROPERTY_CONNECTOR, dataType = IntegrationConstants.DATA_TYPE_EXTERNAL_SYSTEM, joinComponent = SystemComponent.class)
    @Secured(IntegrationAdminRight.class)
    public SystemComponent getSystem();

    public static class Init {
        public String uniqueId;
        public Long systemId;
    }
}

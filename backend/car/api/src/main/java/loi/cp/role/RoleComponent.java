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

package loi.cp.role;

import com.fasterxml.jackson.annotation.JsonView;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.ItemMapping;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.service.relationship.RelationshipConstants;
import com.learningobjects.de.web.Queryable;
import com.learningobjects.de.web.QueryableId;

import javax.validation.groups.Default;

@Schema("role")
@ItemMapping(value = RelationshipConstants.ITEM_TYPE_ROLE, singleton = true)
public interface RoleComponent extends ComponentInterface, QueryableId {
    @JsonView(Default.class)
    @Queryable(dataType = RelationshipConstants.DATA_TYPE_ROLE_ID)
    public String getRoleId();

    @JsonView(Default.class)
    // This is logical so not queryable.. Hmm.. @Queryable(dataType = DataTypes.DATA_TYPE_NAME)
    public String getName();
}

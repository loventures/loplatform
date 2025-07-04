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

package com.learningobjects.cpxp.service.relationship;

import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataTypedef;

/**
 * Relationship constants.
 */
public interface RelationshipConstants {

    // Role
    String ITEM_TYPE_ROLE = "Role";

    String DATA_TYPE_ROLE_ID = "roleId";

    @DataTypedef(DataFormat.item)
    String DATA_TYPE_SUPPORTED_ROLE = "Relationship.supportedRole";

    String ID_FOLDER_ROLES = "folder-role";

    String ITEM_TYPE_SUPPORTED_ROLE = "SupportedRole";

    String DATA_TYPE_SUPPORTED_ROLE_ROLE = "SupportedRole.role";

    String DATA_TYPE_SUPPORTED_ROLE_RIGHTS = "SupportedRole.rights";

}

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

package com.learningobjects.cpxp.service.integration;

import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataTypedef;

public interface IntegrationConstants {
    public static final String ITEM_TYPE_SYSTEM = "System";

    public static final String DATA_TYPE_SYSTEM_NAME = "System.name";

    public static final String DATA_TYPE_SYSTEM_ID = "System.id";

    public static final String DATA_TYPE_SYSTEM_KEY = "System.key";

    @DataTypedef(DataFormat.text)
    public static final String DATA_TYPE_SYSTEM_RIGHTS = "System.rights";

    public static final String DATA_TYPE_SYSTEM_ALLOW_LOGIN = "System.allowLogin";

    @DataTypedef(DataFormat.bool)
    public static final String DATA_TYPE_SYSTEM_USE_EXTERNAL_IDENTIFIER = "System.useExternalIdentifier";

    public static final String DATA_TYPE_CALLBACK_PATH = "System.callbackPath";

    public static final String DATA_TYPE_SYSTEM_URL = "System.lmsUrl";

    // LTI stuff...

    @DataTypedef(DataFormat.string)
    public static final String DATA_TYPE_SYSTEM_LOGIN = "System.login";

    @DataTypedef(DataFormat.string)
    public static final String DATA_TYPE_SYSTEM_PASSWORD = "System.password";

    public static final String ITEM_TYPE_INTEGRATION = "Integration";

    public static final String DATA_TYPE_DATA_SOURCE = "dataSource";

    public static final String DATA_TYPE_UNIQUE_ID = "uniqueId";

    // TODO: This should be normalized into a local id integration table so I have external
    // system on sites too...
    @DataTypedef(value = DataFormat.string, global = true)
    public static final String DATA_TYPE_LOCAL_ID = "localId";

    @DataTypedef(value = DataFormat.string, global = true)
    public static final String DATA_TYPE_LEGACY_ID = "legacyId";

    @DataTypedef(value = DataFormat.item, itemType = IntegrationConstants.ITEM_TYPE_SYSTEM)
    public static final String DATA_TYPE_EXTERNAL_SYSTEM = "externalSystem";

    public static final String FOLDER_ID_SYSTEMS = "folder-systems";

    public static final String FOLDER_TYPE_SYSTEM = "system";

    public static final String DATA_SOURCE_SYSTEM = "CampusPack";

    public static final String DATA_TYPE_EXTERNAL_ROLE_ID = "externalRoleId";

    public static final String DATA_TYPE_EXTERNAL_ROLE_TYPE = "externalRoleType";

    public static final String ITEM_TYPE_EXTERNAL_ROLE = "ExternalRole";

    public static final String ITEM_TYPE_ROLE_MAPPING = "RoleMapping";

    public static final String DATA_TYPE_ROLE_MAPPING_ROLE_ID = "RoleMapping.roleId";

    public static final String DATA_TYPE_ROLE_MAPPING_ROLE_TYPE = "RoleMapping.type";

    public static final String DATA_TYPE_ROLE_MAPPING_MAPPED_ROLE = "RoleMapping.mappedRole";

    @Deprecated
    @DataTypedef(DataFormat.text)
    public static final String DATA_TYPE_SYSTEM_CONFIGURATION = "System.config";

    @Deprecated
    @DataTypedef(DataFormat.string)
    public static final String DATA_TYPE_SYSTEM_BASIC_LTI_INSTITUTION_NAME = "System.basicLTIInstitutionName";

    @Deprecated
    @DataTypedef(DataFormat.string)
    public static final String DATA_TYPE_SYSTEM_BASIC_LTI_INSTITUTION_DESCRIPTION = "System.basicLTIInstitutionDescription";

    @Deprecated
    @DataTypedef(DataFormat.string)
    public static final String DATA_TYPE_SYSTEM_BASIC_LTI_INSTITUTION_EMAIL_ADDRESS = "System.basicLTIInstitutionEmailAddress";
}

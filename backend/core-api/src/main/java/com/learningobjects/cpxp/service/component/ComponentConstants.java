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

package com.learningobjects.cpxp.service.component;

import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataTypedef;

public interface ComponentConstants {
    String ITEM_TYPE_COMPONENT = "Component";

    @DataTypedef(value = DataFormat.string, entityOnly = true)
    String DATA_TYPE_COMPONENT_IDENTIFIER = "componentId";

    @DataTypedef(DataFormat.text)
    String DATA_TYPE_COMPONENT_CONFIGURATION = "componentConfiguration";

    @DataTypedef(DataFormat.text)
    String DATA_TYPE_ACCOUNT_REQUEST_ATTRIBUTES = "AccountRequest.attributes";

    @DataTypedef(DataFormat.string)
    String DATA_TYPE_PATH_PREFIX = "ZipSite.pathPrefix";
}

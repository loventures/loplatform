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

package com.learningobjects.cpxp.service.script;

import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataTypedef;

public interface ScriptConstants {

    String DATA_TYPE_SCRIPT_ARCHIVE_NAME = "ComponentArchive.name";

    String DATA_TYPE_SCRIPT_ARCHIVE_VERSION = "ComponentArchive.version";

    String DATA_TYPE_SCRIPT_ARCHIVE_IDENTIFIER = "ComponentArchive.identifier";

    String DATA_TYPE_SCRIPT_ARCHIVE_PREFIX = "ComponentArchive.prefix";

    String DATA_TYPE_SCRIPT_ARCHIVE_STRIP = "ComponentArchive.strip";

    String DATA_TYPE_SCRIPT_ARCHIVE_FILE = "ComponentArchive.file";

    String DATA_TYPE_SCRIPT_ARCHIVE_GENERATION = "ComponentArchive.generation";

    String ITEM_TYPE_SCRIPT_ARCHIVE = "ComponentArchive";

    String ID_FOLDER_SCRIPTS = "folder-scripts";

    //TODO: right now we're storing these in the datatable.  Should we make a ScriptFolderFinder??

    @DataTypedef(DataFormat.text)
    String DATA_TYPE_ADMIN_COMPONENT_AVAILABILITY = "ScriptFolder.adminAvailability";

    @Deprecated // :'(
    @DataTypedef(DataFormat.text)
    String DATA_TYPE_STATIC_COMPONENT_CONFIGURATIONS = "ScriptFolder.staticConfigurations";

}

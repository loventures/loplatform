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

package com.learningobjects.cpxp.service.data;

import com.learningobjects.cpxp.service.attachment.AttachmentConstants;
import com.learningobjects.cpxp.service.document.DocumentConstants;

/**
 * Standard data types.
 */
public interface DataTypes {
    String META_DATA_TYPE_ITEM = "#item";

    String META_DATA_TYPE_ID = "#id";

    String META_DATA_TYPE_PARENT = "#parent";

    String META_DATA_TYPE_PARENT_ID = "#parentId";

    String META_DATA_TYPE_ROOT = "#root";

    String META_DATA_TYPE_ROOT_ID = "#rootId";

    @DataTypedef(DataFormat.bool)
    String DATA_TYPE_DELETED = "deleted";

    // SPECIAL
    @DataTypedef(value = DataFormat.string, global = true)
    String DATA_TYPE_ID = "id";

    @DataTypedef(DataFormat.string)
    String DATA_TYPE_NAME = "name";

    @DataTypedef(DataFormat.string)
    String DATA_TYPE_MSG = "msg";

    @DataTypedef(value = DataFormat.string, mappedName = "xtype")
    String DATA_TYPE_TYPE = "type";

    @DataTypedef(DataFormat.text)
    String DATA_TYPE_BODY = "body";

    @DataTypedef(DataFormat.json)
    String DATA_TYPE_JSON = "json";

    @Deprecated
    String DATA_TYPE_DESCRIPTION = DATA_TYPE_BODY;

    @DataTypedef(DataFormat.number)
    String DATA_TYPE_INDEX = "index";

    @DataTypedef(DataFormat.number)
    String DATA_TYPE_X = "x";

    @DataTypedef(DataFormat.number)
    String DATA_TYPE_Y = "y";

    @DataTypedef(DataFormat.bool)
    String DATA_TYPE_AVAILABLE = "available";

    @DataTypedef(DataFormat.bool)
    String DATA_TYPE_UNAVAILABLE = "unavailable";

    @DataTypedef(DataFormat.bool)
    String DATA_TYPE_LOCKED = "locked";

    // SPECIAL
    @DataTypedef(DataFormat.bool)
    String DATA_TYPE_DISABLED = "disabled";

    @DataTypedef(DataFormat.bool)
    String DATA_TYPE_ACTIVE = "active";

    @DataTypedef(DataFormat.bool)
    String DATA_TYPE_IMMUTABLE = "immutable";

    @DataTypedef(DataFormat.bool)
    String DATA_TYPE_CRITICAL = "critical";

    @DataTypedef(DataFormat.item)
    String DATA_TYPE_PROTOTYPE = "prototype";

    @DataTypedef(DataFormat.item)
    String DATA_TYPE_ARCHETYPE = "archetype";

    @DataTypedef(value = DataFormat.string, global = true)
    String DATA_TYPE_URL = "url";

    @DataTypedef(DataFormat.string)
    String DATA_TYPE_ICON_NAME = "iconName";

    @DataTypedef(value = DataFormat.item, itemType = AttachmentConstants.ITEM_TYPE_ATTACHMENT)
    String DATA_TYPE_ICON = "icon";

    @DataTypedef(value = DataFormat.item, itemType = AttachmentConstants.ITEM_TYPE_ATTACHMENT)
    String DATA_TYPE_IMAGE = "image";

    @DataTypedef(value = DataFormat.item, itemType = AttachmentConstants.ITEM_TYPE_ATTACHMENT)
    String DATA_TYPE_LOGO = "logo";

    @DataTypedef(value = DataFormat.item, itemType = AttachmentConstants.ITEM_TYPE_ATTACHMENT)
    String DATA_TYPE_LOGO2 = "logo2";

    @DataTypedef(value = DataFormat.item, itemType = AttachmentConstants.ITEM_TYPE_ATTACHMENT)
    String DATA_TYPE_ATTACHMENT = "attachment";

    @DataTypedef(DataFormat.bool)
    String DATA_TYPE_IN_DIRECTORY = "inDirectory";

    @DataTypedef(DataFormat.string)
    String DATA_TYPE_HOST_NAME = "hostName";

    @DataTypedef(DataFormat.time)
    String DATA_TYPE_START_TIME = "startTime";

    @DataTypedef(DataFormat.time)
    String DATA_TYPE_STOP_TIME = "stopTime";

    @DataTypedef(DataFormat.time)
    String DATA_TYPE_PUBLISH_TIME = "publishTime";

    @DataTypedef(DataFormat.time)
    String DATA_TYPE_ACTIVITY_TIME = "activityTime";

    @DataTypedef(DataFormat.time)
    String DATA_TYPE_LAST_MODIFIED = "lastModified";

    @DataTypedef(DataFormat.string)
    String DATA_TYPE_ICON_CLASS = "iconClass";

    String DATA_TYPE_CREATE_TIME = DocumentConstants.DATA_TYPE_CREATE_TIME;

    String DATA_TYPE_CREATOR = DocumentConstants.DATA_TYPE_CREATOR;
}

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

package com.learningobjects.cpxp.component.messaging;

import com.learningobjects.cpxp.service.data.DataFormat;
import com.learningobjects.cpxp.service.data.DataTypedef;
import com.learningobjects.cpxp.service.group.GroupConstants;
import com.learningobjects.cpxp.service.user.UserConstants;

public class MessageConstants {
    public static final String ITEM_TYPE_MESSAGE = "Message";

    public static final String ITEM_TYPE_MESSAGE_STORAGE = "MessageStorage";

    public static final String DATA_TYPE_MESSAGE_LABEL = "Message.label";

    public static final String DATA_TYPE_MESSAGE_READ = "Message.read";

    public static final String DATA_TYPE_MESSAGE_STORAGE = "Message.storage";

    @DataTypedef(value = DataFormat.string, nullable = true)
    public static final String DATA_TYPE_MESSAGE_SUBJECT = "Message.subject";

    @DataTypedef(value = DataFormat.text, nullable = true)
    public static final String DATA_TYPE_MESSAGE_BODY = "Message.body";

    @DataTypedef(value = DataFormat.item, itemType = UserConstants.ITEM_TYPE_USER)
    public static final String DATA_TYPE_MESSAGE_SENDER = "Message.sender";

    @DataTypedef(value = DataFormat.item, itemType = GroupConstants.ITEM_TYPE_GROUP)
    public static final String DATA_TYPE_MESSAGE_CONTEXT = "Message.context";

    @DataTypedef(value = DataFormat.json)
    public static final String DATA_TYPE_MESSAGE_METADATA = "Message.metadata";

    @DataTypedef(value = DataFormat.time, nullable = true)
    public static final String DATA_TYPE_MESSAGE_TIMESTAMP = "Message.timestamp";

    @DataTypedef(value = DataFormat.item, itemType = MessageConstants.ITEM_TYPE_MESSAGE_STORAGE)
    public static final String DATA_TYPE_MESSAGE_THREAD = "Message.thread";

    @DataTypedef(value = DataFormat.item, itemType = MessageConstants.ITEM_TYPE_MESSAGE_STORAGE)
    public static final String DATA_TYPE_MESSAGE_IN_REPLY_TO = "Message.inReplyTo";
}


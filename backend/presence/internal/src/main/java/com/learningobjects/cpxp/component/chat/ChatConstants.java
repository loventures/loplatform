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

package com.learningobjects.cpxp.component.chat;

/**
 * Constants for the chat machinery.
 */
public class ChatConstants {
    /** Model of a chat room. For now the room identifier doubles as the membership. */
    public static final String ITEM_TYPE_CHAT_ROOM = "ChatRoom";

    public static final String DATA_TYPE_CHAT_ROOM_IDENTIFIER = "ChatRoom.roomId";

    /** Model of a chat message. In a subsequent iteration these could be stored as messages
     * with a message thread identifier being the room.
     */
    public static final String ITEM_TYPE_CHAT_MESSAGE = "ChatMessage";

    public static final String DATA_TYPE_CHAT_MESSAGE_ROOM = "ChatMessage.room";

    public static final String DATA_TYPE_CHAT_MESSAGE_SENDER = "ChatMessage.sender";

    public static final String DATA_TYPE_CHAT_MESSAGE_TIMESTAMP = "ChatMessage.timestamp";

    public static final String DATA_TYPE_CHAT_MESSAGE_BODY = "ChatMessage.body";
}


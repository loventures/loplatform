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

package loi.cp.chat

import java.util.Date

import com.learningobjects.cpxp.component.chat.ChatConstants.*
import com.learningobjects.cpxp.dto.{Facade, FacadeData, FacadeItem}

/** Chat message facade.
  */
@FacadeItem(ITEM_TYPE_CHAT_MESSAGE)
trait ChatMessageFacade extends Facade:
  @FacadeData(DATA_TYPE_CHAT_MESSAGE_ROOM)
  def getRoomId: Long
  def setRoomId(roomId: Long): Unit

  @FacadeData(DATA_TYPE_CHAT_MESSAGE_SENDER)
  def getSenderId: Long
  def setSenderId(senderId: Long): Unit

  @FacadeData(DATA_TYPE_CHAT_MESSAGE_TIMESTAMP)
  def getTimestamp: Date
  def setTimestamp(timestamp: Date): Unit

  @FacadeData(DATA_TYPE_CHAT_MESSAGE_BODY)
  def getBody: String
  def setBody(body: String): Unit
end ChatMessageFacade

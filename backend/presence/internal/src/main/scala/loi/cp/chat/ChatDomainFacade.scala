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

import com.learningobjects.cpxp.component.chat.ChatConstants.*
import com.learningobjects.cpxp.dto.*
import com.learningobjects.cpxp.service.domain.DomainConstants
import com.learningobjects.cpxp.service.query.QueryBuilder

/** Chat domain facade. Provides global access to chat rooms and chat messages.
  */
@FacadeItem(DomainConstants.ITEM_TYPE_DOMAIN)
trait ChatDomainFacade extends Facade:
  @FacadeChild
  def getChatRoom(id: Long): Option[ChatRoomFacade]
  def getOrCreateChatRoom(
    @FacadeCondition(DATA_TYPE_CHAT_ROOM_IDENTIFIER) roomId: String
  ): ChatRoomFacade

  @FacadeChild
  def addChatMessage(init: ChatMessageFacade => Unit): ChatMessageFacade
  @FacadeQuery(domain = true)
  def queryChatMessages: QueryBuilder
end ChatDomainFacade

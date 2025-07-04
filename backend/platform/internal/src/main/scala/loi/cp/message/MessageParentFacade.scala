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

package loi.cp.message

import com.learningobjects.cpxp.component.messaging.MessageConstants
import com.learningobjects.cpxp.dto.{Facade, FacadeChild, FacadeCondition, FacadeItem}
import com.learningobjects.cpxp.service.query.QueryBuilder
import com.learningobjects.cpxp.service.user.UserConstants.ITEM_TYPE_USER

@FacadeItem(ITEM_TYPE_USER)
trait MessageParentFacade extends Facade:
  @FacadeChild
  def queryMessages: QueryBuilder
  def getMessage(id: Long): Option[MessageFacade]
  def addMessage(f: MessageFacade => Unit): MessageFacade
  def findMessageByStorage(
    @FacadeCondition(MessageConstants.DATA_TYPE_MESSAGE_STORAGE) storage: Long
  ): Option[MessageFacade]

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

package loi.cp.email

import com.learningobjects.cpxp.component.DataModel
import com.learningobjects.cpxp.dto.*
import com.learningobjects.cpxp.service.email.EmailFinder.*
import com.learningobjects.cpxp.service.reply.ReplyFinder.*
import com.learningobjects.cpxp.service.user.UserConstants
import loi.cp.reply.ReplyFacade

/** Decorates users to manage emails and replies stored beneath them. The two concepts are entangled so the single
  * parent facade is convenient.
  */
@FacadeItem(UserConstants.ITEM_TYPE_USER)
trait EmailParentFacade extends Facade:
  @FacadeData(value = UserConstants.DATA_TYPE_EMAIL_ADDRESS)
  def getEmailAddress: Option[String]

  @FacadeComponent
  def addEmail[A <: Email](cls: Class[A], init: Email.Init)(implicit dm: DataModel[Email]): A

  def getEmail(id: Long)(implicit dm: DataModel[Email]): Option[Email]

  def findEmailByEntity(
    @FacadeCondition(DATA_TYPE_EMAIL_ENTITY) entity: Long,
  )(implicit dm: DataModel[Email]): Option[Email]

  @FacadeChild
  def addReply[A](f: ReplyFacade => A): ReplyFacade

  def findReplyByEntity(
    @FacadeCondition(DATA_TYPE_REPLY_ENTITY) entity: Long,
  ): Option[ReplyFacade]

  def findReplyByMessageId(
    @FacadeCondition(DATA_TYPE_REPLY_MESSAGE_ID) messageId: String,
  ): Option[ReplyFacade]
end EmailParentFacade

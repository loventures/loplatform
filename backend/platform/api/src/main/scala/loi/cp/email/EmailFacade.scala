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

import java.util.Date

import com.learningobjects.cpxp.dto.{Facade, FacadeData, FacadeItem}
import com.learningobjects.cpxp.service.component.ComponentConstants
import com.learningobjects.cpxp.service.email.EmailFinder.*

@FacadeItem(ITEM_TYPE_EMAIL)
trait EmailFacade extends Facade:
  @FacadeData(value = ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER)
  def getComponentIdentifier: String
  def setComponentIdentifier(identifier: String): Unit

  @FacadeData(value = DATA_TYPE_EMAIL_SENT)
  def getSent: Option[Date]
  def setSent(sent: Date): Unit

  @FacadeData(value = DATA_TYPE_EMAIL_SUCCESS)
  def getSuccess: Option[Boolean]
  def setSuccess(success: Boolean): Unit

  @FacadeData(value = DATA_TYPE_EMAIL_NO_REPLY)
  def getNoReply: Option[Boolean]
  def setNoReply(noReply: Boolean): Unit

  @FacadeData(value = DATA_TYPE_EMAIL_ENTITY)
  def getEntity: Option[Long]
  def setEntity(entity: Option[Long]): Unit

  @FacadeData(value = DATA_TYPE_EMAIL_BODY)
  def getBody[T](clas: Class[T]): Option[T]
  def setBody[T](body: Option[T]): Unit

  def refresh(timeout: Int): Boolean
end EmailFacade

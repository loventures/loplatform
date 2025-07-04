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

package loi.cp.imports

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentDescriptor, ComponentImplementation, ComponentInstance}
import loi.cp.imports.errors.{GenericError, ImportErrorComponent}

import java.lang.Long as JLong

@Component
class ImportErrorImpl(
  val componentInstance: ComponentInstance,
  self: ImportErrorFacade
) extends ImportErrorComponent
    with ComponentImplementation:

  implicit val cd: ComponentDescriptor = componentInstance.getComponent

  override def getReason: String               = self.getReason
  override def setReason(reason: String): Unit = self.setReason(reason)

  override def getError: GenericError              = self.getError
  override def setError(error: GenericError): Unit = self.setError(error)

  override def getIndex: JLong                   = self.getIndex
  override def setIndex(lineNumber: JLong): Unit = self.setIndex(lineNumber)

  override def getMessages: Seq[String] = self.getError.messages

  override def getId: JLong = self.getId
end ImportErrorImpl

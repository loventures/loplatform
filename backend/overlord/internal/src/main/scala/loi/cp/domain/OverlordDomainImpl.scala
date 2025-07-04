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

package loi.cp.domain

import java.lang.Long as JLong

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.domain.{DomainFacade, DomainState, SecurityLevel}
import com.learningobjects.cpxp.util.ClassUtils

import scala.jdk.CollectionConverters.*

@Component
class OverlordDomainImpl(
  val componentInstance: ComponentInstance,
  self: DomainFacade
) extends OverlordDomain
    with ComponentImplementation:

  override def getId: JLong = self.getId

  override def update(d: OverlordDomain): OverlordDomain =
    ???

  override def delete() = ???

  override def getDomainId: String = self.getDomainId

  override def getName: String = self.getName

  override def getShortName: String = self.getShortName

  override def getPrimaryHostName: String = self.getPrimaryHostName

  override def getHostNames: List[String] = self.getHostNames.asScala.toList

  override def getState: DomainState = self.getState

  override def getLocale: String =
    ClassUtils.parseLocale(self.getLocale).toLanguageTag

  override def getTimeZone: String = self.getTimeZone

  override def getSecurityLevel: SecurityLevel = self.getSecurityLevel
end OverlordDomainImpl

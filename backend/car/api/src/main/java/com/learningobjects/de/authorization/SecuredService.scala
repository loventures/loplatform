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

package com.learningobjects.de.authorization

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.right.RightService

import scala.jdk.CollectionConverters.*

@Service
class SecuredService(
  domainDto: => DomainDTO,
  rightService: RightService,
  securityContext: => SecurityContext,
  userDto: => UserDTO
):

  /** Decides if the current user passes the `security` checks for `subject`.
    */
  def isPermitted(security: Secured, subject: Option[Long]): Boolean =

    if security.conjunction() then
      // FIXME
      throw new UnsupportedOperationException("Secured.conjunction true is unsupported")

    lazy val allowedBecauseSrsCollectedRights = security.value.toSet.exists(collectedRights.contains)
    lazy val allowedBecauseSubjectIsOwner     = security.byOwner && subject.contains(userDto.id)
    lazy val allowedBecauseUserHasRights      =
      val context = subject.map(s => (() => s): Id).getOrElse(domainDto)
      rightService.getUserHasAtLeastOneRight(context, userDto, security.value.toSet.asJava)

    security.allowAnonymous() ||
    allowedBecauseSrsCollectedRights ||
    allowedBecauseSubjectIsOwner ||
    allowedBecauseUserHasRights
  end isPermitted

  private def collectedRights = securityContext.get(classOf[CollectedRights]).getRights.asScala.toSet
end SecuredService

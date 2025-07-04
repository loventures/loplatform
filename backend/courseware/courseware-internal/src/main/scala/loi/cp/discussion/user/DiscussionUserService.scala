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

package loi.cp.discussion.user

import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.context.ContextId
import loi.cp.user.UserComponent
import loi.cp.web.HandleService

/** Service intended to grab users and rights about a user associated with a discussion board.
  */
@Service
trait DiscussionUserService:

  def getUser(id: UserId, contextId: ContextId, anonymize: Boolean = false): DiscussionUserProfile

  def getUsers(
    ids: Iterable[UserId],
    contextId: ContextId,
    anonymize: Boolean = false
  ): Map[UserId, DiscussionUserProfile]
end DiscussionUserService

@Service
class DiscussionUserServiceComponentBacked(
  domainDto: => DomainDTO,
  handleService: HandleService
)(implicit cs: ComponentService)
    extends DiscussionUserService:

  override def getUser(userId: UserId, contextId: ContextId, anonymize: Boolean): DiscussionUserProfile =
    Option(userId.component[UserComponent])
      .fold[DiscussionUserProfile](NonexistantDiscussionUserProfile(handleService.mask(userId)))(
        (user: UserComponent) => DiscussionUserProfile(user, anonymize = anonymize)
      )

  override def getUsers(
    ids: Iterable[UserId],
    contextId: ContextId,
    anonymize: Boolean
  ): Map[UserId, DiscussionUserProfile] =

    val existingUserMap = ids
      .components[UserComponent]
      .map((user: UserComponent) => UserId(user.getId.longValue) -> DiscussionUserProfile(user, anonymize = anonymize))
      .toMap

    val imaginaryUserMap =
      ids.toSet
        .diff(existingUserMap.keys.toSet)
        .map(key => key -> NonexistantDiscussionUserProfile(handleService.mask(key)))
        .toMap

    existingUserMap ++ imaginaryUserMap
  end getUsers
end DiscussionUserServiceComponentBacked

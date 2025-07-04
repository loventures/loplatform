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

package loi.cp.user

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.exception.AccessForbiddenException
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.Widen
import loi.cp.context.ContextId
import scalaz.\/

/** Service to determine if whether users have the authority to impersonate others
  */
@Service
trait ImpersonationService:

  /** Checks whether `currentUser` may impersonate `targetUser` within the context of `courseId`.
    *
    * @param courseId
    *   the ID of the course in which to impersonate
    * @param targetUser
    *   the target of the impersonation
    *
    * @return
    *   nothing on success, or an [[ImpersonationError]] if impersonation is not allowed.
    */
  def checkImpersonation(
    courseId: ContextId,
    targetUser: UserId,
  ): ImpersonationError \/ Unit

  // For Java.
  @throws[ImpersonationError]
  @deprecated("use the one that's explicit about its error cases", since = "we started using scala")
  final def checkImpersonationOrThrow(courseId: Long, targetUser: UserId): Unit =
    checkImpersonation(ContextId(courseId), targetUser).valueOr(throw _)
end ImpersonationService

sealed abstract class ImpersonationError(msg0: String)
    extends AccessForbiddenException(msg0)
    with Widen[ImpersonationError] // bad but comports with tryful uses elsewhere

object ImpersonationError:
  // TODO: this is _bad_, and (as the impl says) an "unexpected side effect".
  // By putting it here, maybe it is a little more obvious.
  final case class ContentProtected() extends ImpersonationError(ContentProtected.Message)
  object ContentProtected:
    final val Message = "Protected content."

  final case class NotSuperUser() extends ImpersonationError(NotSuperUser.Message)
  object NotSuperUser:
    final val Message = "You do not have permission to view this user's content."
end ImpersonationError

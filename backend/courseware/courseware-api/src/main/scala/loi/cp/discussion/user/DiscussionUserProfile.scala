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

import loi.cp.user.{Profile, UserComponent}

/** Minimal set of information needed for a user interacting on a a discussion board.
  */
sealed trait DiscussionUserProfile:

  /** This should be an obfuscated user id through the [[loi.cp.web.HandleService]]
    */
  val handle: String

  val firstName: String

  val fullName: String

  val thumbnailId: Option[Long]
end DiscussionUserProfile

/** Fully anonymized user based on handle.
  * @param identifier
  *   handle obfuscated from user identifier
  */
case class AnonymousDiscussionUserProfile(identifier: String) extends DiscussionUserProfile:
  override val firstName: String = identifier

  override val fullName: String = identifier

  override val handle: String = identifier

  override val thumbnailId: Option[Long] = None

/** User that used to exist in the system, but no longer does. Placeholder for that user's properties
  * @param identifier
  *   Handle, typically garnered from the post the non-existent user wrote.
  */
case class NonexistantDiscussionUserProfile(identifier: String) extends DiscussionUserProfile:
  override val firstName: String = "Unknown"

  override val fullName: String = "Unknown"

  override val handle: String = identifier

  override val thumbnailId: Option[Long] = None

case class CompleteDiscussionUserProfile(
  handle: String,
  firstName: String,
  fullName: String,
  thumbnailId: Option[Long]
) extends DiscussionUserProfile

object DiscussionUserProfile:

  def apply(
    user: UserComponent,
    anonymize: Boolean
  ): DiscussionUserProfile =
    val u: DiscussionUserProfile =
      CompleteDiscussionUserProfile(user.getHandle, user.getGivenName, user.getFullName, None)
    if anonymize then anonymizeUser(u) else u

  def apply(
    profile: Profile,
    anonymize: Boolean
  ): DiscussionUserProfile =
    val u: DiscussionUserProfile =
      CompleteDiscussionUserProfile(
        profile.getHandle,
        profile.getGivenName,
        profile.getFullName,
        profile.getThumbnailId
      )
    if anonymize then anonymizeUser(u) else u
  end apply

  def anonymizeUser(user: DiscussionUserProfile): AnonymousDiscussionUserProfile =
    AnonymousDiscussionUserProfile(user.handle)
end DiscussionUserProfile

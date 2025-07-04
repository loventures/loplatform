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

package loi.cp.discussion.dto
import java.time.Instant

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.attachment.AttachmentId
import loi.cp.discussion.PostId

/** Container for a user-agnostic post data
  */
trait PostProperties extends Id:

  /** PK of this post.
    */
  def id: PostId

  /** Direct parent post id if one exists.
    */
  def parentId: Option[PostId]

  /** Unsanitized content of this post. THis should generally not be used by a frontend.
    */
  def unsafeContent: String

  /** optional post title
    */
  def title: Option[String]

  /** Sanitized content of the post.
    */
  def content: String

  /** Who made this post.
    */
  def authorId: UserId

  /** when was this post first created.
    */
  def created: Instant

  /** WHen was the content last updated for this post.
    */
  def updated: Instant

  /** PK value list of all direct ancestors of this post.
    */
  def postPath: Seq[PostId]

  /** The topmost ancestor of this post's PK
    */
  def threadId: PostId

  /** Has this post been marked as inappropriate.
    */
  def inappropriate: Boolean

  /** Was this post made by a moderator.
    */
  def moderatorPost: Boolean

  /** Has this post been removed, usually only moderators should have this property ever set ot true
    */
  def removed: Boolean

  /** Has this post been pinned by a moderator and when was that done.
    */
  def pinnedOn: Option[Instant]

  /** Attachements associated with this post
    */
  def attachmentIds: Seq[AttachmentId]

  /** How many direct ancestors does this post have.
    */
  def depth: Long
end PostProperties

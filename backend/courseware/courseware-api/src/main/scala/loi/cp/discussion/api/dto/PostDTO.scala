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

package loi.cp.discussion.api.dto

import java.time.Instant

import com.learningobjects.cpxp.util.Id.*
import loi.cp.attachment.{AttachmentId, AttachmentInfo}
import loi.cp.discussion.dto.Post
import loi.cp.discussion.user.DiscussionUserProfile
import loi.cp.reference.ContentIdentifier

case class PostDTO(
  id: Long,
  contentId: ContentIdentifier,
  parentPostId: Option[Long],
  threadId: Long,
  depth: Long,
  author: DiscussionUserProfile,
  title: Option[String],
  content: String,
  createTime: Instant,
  updateTime: Instant,
  moderatorPost: Boolean,
  removed: Boolean,
  inappropriate: Boolean,
  canEdit: Boolean,
  pinned: Boolean,
  bookmarked: Boolean,
  read: Boolean,
  descendantActivity: Option[Instant],
  descendantCount: Option[Long],
  newDescendantCount: Option[Long],
  unreadDescendantCount: Option[Long],
  attachmentIds: Seq[AttachmentId],
  attachmentInfos: Map[Long, AttachmentInfo]
)

object PostDTO:

  def apply(
    discussionId: ContentIdentifier,
    markedAsRead: Boolean,
    canEdit: Boolean,
    post: Post,
    attachmentInfos: Seq[AttachmentInfo]
  ): PostDTO =
    PostDTO(
      post.id,
      discussionId,
      post.parentId,
      post.threadId,
      post.depth,
      post.author,
      post.title,
      post.content,
      post.created,
      post.updated,
      post.moderatorPost,
      post.removed,
      post.inappropriate,
      canEdit,
      post.pinnedOn.isDefined,
      post.bookmarked,
      markedAsRead,
      post.descendantActivity,
      post.descendantCount,
      post.newDescendantCount,
      post.unreadDescendantCount,
      post.attachmentIds,
      attachmentInfos.byId
    )
end PostDTO

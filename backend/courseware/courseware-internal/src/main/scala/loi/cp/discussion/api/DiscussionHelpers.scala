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

package loi.cp.discussion.api
import loi.cp.attachment.AttachmentInfo
import loi.cp.course.lightweight.Lwc
import loi.cp.discussion.api.dto.PostDTO
import loi.cp.discussion.dto.Post
import loi.cp.discussion.{DiscussionSetting, StoragedDiscussionSettings}
import loi.cp.reference.{ContentIdentifier, EdgePath}
import loi.cp.storage.CourseStorageService

import scala.util.Try

object DiscussionHelpers:

  def toPostDto(
    discussionId: ContentIdentifier,
    post: Post,
    currentUserId: Long,
    discussionClosed: Boolean,
    isModerator: Boolean,
    attachments: Seq[AttachmentInfo]
  ): PostDTO =
    val markedAsRead = post.read || currentUserId == post.authorId.value
    val canEdit      = post.canEdit && !discussionClosed || isModerator

    PostDTO(discussionId, markedAsRead, canEdit, post, attachments)
  end toPostDto

  /** Useful for singular check * */
  def isClosed(setting: Map[EdgePath, DiscussionSetting], edgePath: EdgePath): Boolean =
    setting.getOrElse(edgePath, DiscussionSetting.default).closed

  def discussionSettings(
    lwc: Lwc
  )(implicit courseStorageService: CourseStorageService): Map[EdgePath, DiscussionSetting] =
    (for settings <- Try(courseStorageService.get[StoragedDiscussionSettings](lwc)).toOption
    yield settings.settings.discussionBoardSettings).getOrElse(Map.empty)
end DiscussionHelpers

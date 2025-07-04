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

package loi.cp.discussion

import java.lang
import java.time.Instant

import com.learningobjects.cpxp.component.discussion.{PostEntity, PostValue, PostValueEntity}
import com.learningobjects.cpxp.component.util.ComponentUtils
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.attachment.AttachmentId
import loi.cp.discussion.dto.PostProperties

case class PostPropertiesImpl(postValue: PostValue) extends PostProperties:

  override val id: PostId = postValue.id

  override val getId: lang.Long = id

  override val authorId: UserId = postValue.user

  override val parentId: Option[PostId] = postValue.parentPostId

  override val unsafeContent: String = postValue.content

  override def title: Option[String] = postValue.title

  override val content: String = ComponentUtils.dexss(postValue.content)

  override val created: Instant = postValue.created

  override val updated: Instant = postValue.updated

  override val postPath: Seq[PostId] = postValue.postPath

  override val threadId: PostId = postPath.head

  override val inappropriate: Boolean = postValue.inappropriate

  override val removed: Boolean = postValue.removed

  override val pinnedOn: Option[Instant] = postValue.pinnedOn

  override val moderatorPost: Boolean = postValue.moderatorPost

  override val depth: Long = postValue.depth

  override val attachmentIds: Seq[AttachmentId] = postValue.attachmentIds
end PostPropertiesImpl

object PostPropertiesImpl:

  def apply(postEntity: PostEntity): PostProperties =
    PostPropertiesImpl(PostValueEntity(postEntity, None, None))

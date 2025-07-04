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

package com.learningobjects.cpxp.component.discussion

import jakarta.persistence.{Column, Entity, Index, Table}

import com.learningobjects.cpxp.entity.DomainEntity
import com.learningobjects.cpxp.scala.util.JTypes.{JBoolean, JLong}
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.discussion.PostId
import org.apache.commons.lang3.BooleanUtils
import org.hibernate.annotations.{Cache as HCache, NaturalId}
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

@Entity
@Table(
  name = PostInteractionEntity.ITEM_TYPE_DISCUSSION_POST_INTERATION,
  indexes = Array(new Index(name = "postinteration_user_post_idx", columnList = "userId,postId"))
)
@HCache(usage = READ_WRITE)
class PostInteractionEntity extends DomainEntity:

  @NaturalId
  @Column(nullable = false)
  var userId: JLong = scala.compiletime.uninitialized

  @NaturalId
  @Column(nullable = false)
  var postId: JLong = scala.compiletime.uninitialized

  @Column
  var favorited: JBoolean = scala.compiletime.uninitialized

  @Column
  var bookmarked: JBoolean = scala.compiletime.uninitialized

  @Column
  var viewed: JBoolean = scala.compiletime.uninitialized
end PostInteractionEntity

object PostInteractionEntity:

  final val ITEM_TYPE_DISCUSSION_POST_INTERATION = "DiscussionPostInteraction"

  def apply(id: Long, postId: PostId, user: UserId): PostInteractionEntity =
    val entity: PostInteractionEntity = new PostInteractionEntity
    entity.setId(id)
    entity.userId = user.id
    entity.postId = postId
    entity
end PostInteractionEntity

sealed trait PostInteractionValue:

  val postId: PostId
  val user: UserId
  val favorited: Boolean
  val bookmarked: Boolean
  val viewed: Boolean

case class PostInteractionValueRaw(
  _postId: JLong,
  _userId: JLong,
  _favorited: JBoolean,
  _bookmarked: JBoolean,
  _viewed: JBoolean
) extends PostInteractionValue:
  override val postId: PostId      = _postId.longValue()
  override val user: UserId        = UserId(_userId.longValue())
  override val favorited: Boolean  = BooleanUtils.toBooleanDefaultIfNull(_favorited, false)
  override val bookmarked: Boolean = BooleanUtils.toBooleanDefaultIfNull(_bookmarked, false)
  override val viewed: Boolean     = BooleanUtils.toBooleanDefaultIfNull(_viewed, false)
end PostInteractionValueRaw

case class PostInteractionValueEntity(entity: PostInteractionEntity) extends PostInteractionValue:

  override val postId: PostId = entity.postId

  override val user: UserId = UserId(entity.userId)

  override val favorited: Boolean  = BooleanUtils.toBooleanDefaultIfNull(entity.favorited, false)
  override val bookmarked: Boolean = BooleanUtils.toBooleanDefaultIfNull(entity.bookmarked, false)
  override val viewed: Boolean     = BooleanUtils.toBooleanDefaultIfNull(entity.viewed, false)

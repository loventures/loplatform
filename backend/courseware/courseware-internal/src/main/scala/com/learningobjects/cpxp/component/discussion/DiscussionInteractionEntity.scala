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

import java.time.Instant
import java.util.Date

import com.learningobjects.cpxp.entity.DomainEntity
import com.learningobjects.cpxp.scala.util.JTypes.{JBoolean, JLong}
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.service.user.UserId
import jakarta.persistence.{Column, Entity, Index, Table}
import loi.cp.context.ContextId
import loi.cp.reference.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

@Entity
@Table(
  name = DiscussionInteractionEntity.ITEM_TYPE_DISCUSSION_INTERACTION,
  indexes = Array(
    new Index(
      name = "discussioninteration_user_post_idx",
      columnList = "contextId,edgePath"
    ),
    new Index(name = "discussioninteraction_user_id_idx", columnList = "userId")
  )
)
@HCache(usage = READ_WRITE)
class DiscussionInteractionEntity extends DomainEntity with UserContentIdentifierEntity:

  @Column(nullable = false)
  var visited: Date = scala.compiletime.uninitialized

  @Column()
  var canEditOwnPosts: JBoolean = scala.compiletime.uninitialized
end DiscussionInteractionEntity

object DiscussionInteractionEntity:

  final val ITEM_TYPE_DISCUSSION_INTERACTION = "DiscussionInteraction"
  def apply(
    id: Long,
    domain: Item,
    discussionId: ContentIdentifier,
    user: UserId,
    visited: Instant
  ): DiscussionInteractionEntity =
    val discussionInteractionEntity: DiscussionInteractionEntity = new DiscussionInteractionEntity

    discussionInteractionEntity.setId(id)
    discussionInteractionEntity.setRoot(domain)

    discussionInteractionEntity.userId = user.id
    discussionInteractionEntity.edgePath = discussionId.edgePath.toString
    discussionInteractionEntity.contextId = discussionId.contextId.value
    discussionInteractionEntity.visited = Date.from(visited)
    discussionInteractionEntity
  end apply
end DiscussionInteractionEntity

sealed trait DiscussionInteraction:

  /** This value is the last time the user visited the discussion board. From a sanity/comparison perspective this
    * should default to the beginning of time.
    */
  val visited: Instant

  /** Can this user edit their own posts in the discussion board. Specifically for non-moderators. Original use case was
    * to make sure that users cannot edit after their contribution to a discussion board receives a grade.
    */
  val canEditOwnPosts: Boolean
end DiscussionInteraction

/** Containing class that strips out non-essential information from the interaction. userid/edgepath/context are
  * necessary for persistence but not at higher levels.
  */
case class DiscussionInteractionBase(visited: Instant, canEditOwnPosts: Boolean) extends DiscussionInteraction

case class DiscussionInteractionRaw(
  _userId: JLong,
  edgePath: String,
  _contextId: JLong,
  _visited: Date,
  _canEditPosts: Boolean
) extends DiscussionInteraction:
  val contentId                         = ContentIdentifier(ContextId(_contextId), EdgePath.parse(edgePath))
  override val visited: Instant         = Option(_visited).fold(DiscussionInteraction.DEFAULT_LAST_VISITED)(_.toInstant)
  override val canEditOwnPosts: Boolean =
    Option(_canEditPosts).fold(DiscussionInteraction.DEFAULT_CAN_EDIT_OWN_POSTS)(_.booleanValue())
end DiscussionInteractionRaw

object DiscussionInteraction:

  val empty: DiscussionInteraction = DiscussionInteractionBase(Instant.EPOCH, canEditOwnPosts = true)

  def apply(entity: DiscussionInteractionEntity): DiscussionInteraction =
    DiscussionInteractionBase(
      Option(entity.visited).fold(DEFAULT_LAST_VISITED)(_.toInstant),
      Option(entity.canEditOwnPosts).fold(DEFAULT_CAN_EDIT_OWN_POSTS)(_.booleanValue())
    )

  val DEFAULT_CAN_EDIT_OWN_POSTS: Boolean = true
  val DEFAULT_LAST_VISITED: Instant       = Instant.EPOCH
end DiscussionInteraction

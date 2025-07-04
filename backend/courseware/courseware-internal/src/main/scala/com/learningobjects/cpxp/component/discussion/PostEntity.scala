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

import argonaut.Argonaut.*
import argonaut.Json
import com.learningobjects.cpxp.entity.DomainEntity
import com.learningobjects.cpxp.postgresql.{ArgonautUserType, TSVectorUserType}
import com.learningobjects.cpxp.scala.util.JTypes.{JBoolean, JLong}
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.service.user.UserId
import jakarta.persistence.*
import loi.cp.attachment.AttachmentId
import loi.cp.context.ContextId
import loi.cp.discussion.PostId
import loi.cp.reference.*
import org.apache.commons.lang3.BooleanUtils
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{ColumnTransformer, JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.time.Instant
import java.util.Date

@Entity
@Table(
  name = PostEntity.ITEM_TYPE_DISCUSSION_POST,
  indexes = Array(
    new Index(
      name = "post_complex_idx",
      columnList =
        "contextid,edgepath,depth,userid,created,updated,descendantActivity,purged,inappropriate,removed,moderatorpost"
    ),
    new Index(name = "post_thread_id_idx", columnList = "threadId"),
    // See bfr/de/deploy/src/main/resources/postgresql/discussion-post-search-index.sql for searchable content index.
  )
)
@HCache(usage = READ_WRITE)
class PostEntity extends DomainEntity with UserContentIdentifierEntity:

  @Column()
  var title: String = scala.compiletime.uninitialized

  @Lob
  @Column(nullable = false, columnDefinition = "TEXT")
  var content: String = scala.compiletime.uninitialized

  @Column(columnDefinition = "TSVECTOR")
  @ColumnTransformer(write = "to_tsvector('english',LOWER(?))")
  @Type(classOf[TSVectorUserType])
  var searchableContent: String = scala.compiletime.uninitialized

  @Column
  var parentPostId: JLong = scala.compiletime.uninitialized

  @Column(nullable = false)
  var depth: JLong = scala.compiletime.uninitialized

  /** TODO: Validate max depth assumptions in a discussion board. NOTE: Max depth for any discussion thread is 21
    * assuming we hit 11 digits per item. We may want to consider making this varchar(512) so our max depth is 42 and
    * specifically disallowing deeper replies than that.
    */
  @Column(nullable = false)
  var postPath: String = scala.compiletime.uninitialized

  /** Storing the thread id so that aggregation subqueries have a good thing to index off of TODO: nullable:false
    */
  @Column
  var threadId: JLong = scala.compiletime.uninitialized

  @Column
  var descendantActivity: Date = scala.compiletime.uninitialized

  @Column
  var pinnedOn: Date = scala.compiletime.uninitialized

  @Column
  var inappropriate: JBoolean = scala.compiletime.uninitialized

  @Column
  var removed: JBoolean = scala.compiletime.uninitialized

  @Column
  var moderatorPost: JBoolean = scala.compiletime.uninitialized

  @Column(nullable = false)
  var created: Date = scala.compiletime.uninitialized

  @Column(nullable = false)
  var updated: Date = scala.compiletime.uninitialized

  @Column
  var purged: String = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @Type(classOf[ArgonautUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var attachmentIds: Json = scala.compiletime.uninitialized
end PostEntity

object PostEntity:

  final val ITEM_TYPE_DISCUSSION_POST = "DiscussionPost"
  final val ATTACHMENT_SEPARATOR      = ","

  def apply(
    id: Long,
    domain: Item,
    discussionId: ContentIdentifier,
    user: UserId,
    isModerator: Boolean,
    parent: Option[PostEntity],
    created: Instant,
    title: Option[String],
    content: String,
    searchableContent: String,
    attachmentIds: Seq[AttachmentId]
  ): PostEntity =
    val postEntity: PostEntity = new PostEntity

    postEntity.setId(id)
    postEntity.setRoot(domain)

    postEntity.userId = user.value
    postEntity.moderatorPost = isModerator
    postEntity.contextId = discussionId.contextId.value
    postEntity.edgePath = discussionId.edgePath.toString

    postEntity.created = Date.from(created)
    postEntity.updated = Date.from(created)
    postEntity.descendantActivity = Date.from(created)

    if parent.isDefined then
      val pp: PostEntity = parent.get
      postEntity.parentPostId = pp.getId
      postEntity.depth = pp.depth + 1
      postEntity.postPath = pp.postPath + id.toString + "/"
      postEntity.threadId = pp.threadId
    else
      postEntity.depth = 0L
      postEntity.postPath = id.toString + "/"
      postEntity.threadId = id
    end if

    if title.isDefined then postEntity.title = title.get
    postEntity.content = content

    postEntity.searchableContent = searchableContent

    postEntity.attachmentIds = attachmentIds.toList.asJson

    postEntity
  end apply

  def updateAttachments(
    validPost: PostEntity,
    newAttachmentIds: Seq[AttachmentId],
    maybeAttachmentsToKeep: Option[Seq[AttachmentId]]
  ): PostEntity =
    val validPostAttachmentIds: Seq[AttachmentId] = mapAttachmentIds(validPost.attachmentIds)
    val attachmentsToKeep: Seq[AttachmentId]      =
      maybeAttachmentsToKeep.fold(validPostAttachmentIds)(_.intersect(validPostAttachmentIds))
    validPost.attachmentIds = (newAttachmentIds ++ attachmentsToKeep).toList.asJson
    validPost
  end updateAttachments

  def mapAttachmentIds(attachmentIds: Json): Seq[AttachmentId] =
    if attachmentIds.isArray then attachmentIds.as[List[AttachmentId]].getOr(Seq())
    else Nil
end PostEntity

/** This is a value object for rapid db read support. By leveraging a value object rather than an entity, we can bypass
  * hibernate cache management. See CBLPROD-15468.
  */
sealed trait PostValue:
  val contentIdentifier: ContentIdentifier
  val id: PostId
  val contextId: ContextId
  val parentPostId: Option[PostId]
  val threadId: PostId
  val postPath: Seq[PostId]
  val user: UserId
  val title: Option[String]
  val content: String
  val moderatorPost: Boolean
  val inappropriate: Boolean
  val removed: Boolean
  val pinnedOn: Option[Instant]
  val created: Instant
  val updated: Instant
  val descendantActivity: Option[Instant]
  val depth: Long
  val descendantCount: Option[Long]
  val newDescendantCount: Option[Long]
  val attachmentIds: Seq[AttachmentId]
end PostValue

/** Use this when new-ing an object in hql. See CBLPROD-15468.
  */
case class PostValueRaw(
  _id: JLong,
  _contextId: JLong,
  _edgePath: String,
  _parentPostId: JLong,
  _threadId: JLong,
  _postPathString: String,
  _userId: JLong,
  _title: String,
  content: String,
  _moderatorPost: JBoolean,
  _inappropriate: JBoolean,
  _removed: JBoolean,
  _pinnedOn: Date,
  _created: Date,
  _updated: Date,
  _descendantActivity: Date,
  _depth: JLong,
  _attachmentIds: Json,
  _descendantCount: JLong,
  _newDescendantCount: JLong
) extends PostValue:
  override val id: PostId                           = _id.longValue()
  override val contextId: ContextId                 = ContextId(_contextId.longValue())
  override val parentPostId: Option[PostId]         = Option(_parentPostId).map(_.longValue)
  override val threadId: PostId                     = _threadId.longValue()
  override val user: UserId                         = UserId(_userId.longValue())
  override val moderatorPost: Boolean               = BooleanUtils.toBooleanDefaultIfNull(_moderatorPost, false)
  override val inappropriate: Boolean               = BooleanUtils.toBooleanDefaultIfNull(_inappropriate, false)
  override val removed: Boolean                     = BooleanUtils.toBooleanDefaultIfNull(_removed, false)
  override val pinnedOn: Option[Instant]            = Option(_pinnedOn).map(_.toInstant)
  override val created: Instant                     = _created.toInstant
  override val updated: Instant                     = _updated.toInstant
  override val descendantActivity: Option[Instant]  = Option(_descendantActivity).map(_.toInstant)
  override val depth: Long                          = _depth.longValue
  override val descendantCount: Option[Long]        = Option(_descendantCount).map(_.longValue)
  override val newDescendantCount: Option[Long]     = Option(_newDescendantCount).map(_.longValue)
  override val contentIdentifier: ContentIdentifier = ContentIdentifier(contextId, EdgePath.parse(_edgePath))
  override val postPath: Seq[PostId]                =
    if _postPathString == null then Seq() else _postPathString.split("/").toSeq.map(_.toLong)
  override val attachmentIds: Seq[AttachmentId]     = PostEntity.mapAttachmentIds(_attachmentIds)
  override val title: Option[String]                = Option(_title)
end PostValueRaw

/** Transform to value object if we've already grabbed the entity for write access for whatever reason
  *
  * @param entity
  *   what to wrap
  * @param descendantCount
  *   How many descendants does this post have, None implies not calculated
  * @param newDescendantCount
  *   How many descendants are considered new.
  */
case class PostValueEntity(entity: PostEntity, descendantCount: Option[Long], newDescendantCount: Option[Long])
    extends PostValue:
  override val id: PostId = entity.id

  override val contextId: ContextId = ContextId(entity.contextId)

  override val parentPostId: Option[PostId] = Option(entity.parentPostId)

  override val threadId: PostId = entity.threadId

  override val postPath: Seq[PostId] =
    if entity.postPath == null then Seq() else entity.postPath.split("/").toSeq.map(_.toLong)

  override val user: UserId = UserId(entity.userId)

  override val title: Option[String] = Option(entity.title)

  override val content: String = entity.content

  override val moderatorPost: Boolean = entity.moderatorPost

  override val inappropriate: Boolean = entity.inappropriate

  override val removed: Boolean = entity.removed

  override val pinnedOn: Option[Instant] = Option(entity.pinnedOn).map(_.toInstant)

  override val depth: Long = entity.depth.longValue()

  override val created: Instant = entity.created.toInstant

  override val updated: Instant = entity.updated.toInstant

  override val descendantActivity: Option[Instant] = Option(entity.descendantActivity).map(_.toInstant)

  override val contentIdentifier: ContentIdentifier =
    ContentIdentifier(ContextId(entity.contextId), EdgePath.parse(entity.edgePath))

  override val attachmentIds: Seq[AttachmentId] = PostEntity.mapAttachmentIds(entity.attachmentIds)
end PostValueEntity

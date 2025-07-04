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

package loi.cp.discussion.persistence

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.discussion.*
import com.learningobjects.cpxp.component.query.{ApiOrder, ApiPage}
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.discussion.PostId
import loi.cp.discussion.dto.FilteredSeq
import loi.cp.reference.ContentIdentifier
import org.hibernate.Session
import org.hibernate.query.Query

import scala.jdk.CollectionConverters.*
import scala.reflect.{ClassTag, classTag}

@Service
trait UnreadPostDao:

  /** List of unread posts for the target user
    */
  def unreadPosts(
    discussionId: ContentIdentifier,
    targetUser: UserId,
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[PostValue]

  /** Looks at posts and interactions to figure out the unread counts.
    */
  def unreadReplyCounts(discussionId: ContentIdentifier, user: UserId, posts: Seq[PostId]): Map[PostId, Long]
end UnreadPostDao

@Service
class UnreadPostDaoImpl(session: => Session) extends UnreadPostDao:

  import PostDao.*

  override def unreadPosts(
    discussionId: ContentIdentifier,
    targetUser: UserId,
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[PostValue] =
    def constructQuery[A: ClassTag](queryString: String): Query[A] =
      session
        .createQuery(queryString, classTag[A].runtimeClass.asInstanceOf[Class[A]])
        .setParameter(contextIdParam, discussionId.contextId.value.longValue())
        .setParameter(edgePathParam, discussionId.edgePath.toString)
        .setParameter(userParam, targetUser.id.longValue())

    val postQuery: Query[PostValueRaw] =
      constructQuery[PostValueRaw](
        s"$postSelectionPrefix $noStatPostfix ) $unreadPostsQuery ${constructOrderPart(apiOrders)}"
      )
    val countQuery: Query[JLong]       = constructQuery(s"select count(post) $unreadPostsQuery")

    if apiPage.isSet then
      postQuery
        .setMaxResults(apiPage.getLimit)
        .setFirstResult(apiPage.getOffset)

    FilteredSeq[PostValue](
      countQuery.getSingleResult.longValue,
      postQuery.getFirstResult,
      postQuery.getResultList.asScala.toSeq
    )
  end unreadPosts

  override def unreadReplyCounts(
    discussionId: ContentIdentifier,
    user: UserId,
    posts: Seq[PostId]
  ): Map[PostId, Long] =
    if posts.nonEmpty then
      session
        .createQuery(unreadRepliesCountQuery, classOf[CountDto])
        .setParameter(userParam, user.id.longValue())
        .setParameter(postIdParam, posts.map(_.longValue).asJava)
        .getResultList
        .asScala
        .map(result => (result.itemId.toLong, result.count.toLong))
        .toMap
    else Map.empty

  // when the where clause removes a row, that means that that post was read
  // the left join is not going to remove any rows, but the where clause can
  private val unreadRepliesCountQuery: String =
    s"""select new ${classOf[CountDto].getName}(post.id,
       |  (select count(*)
       |   from ${classOf[PostEntity].getSimpleName} reply
       |   left join ${classOf[PostInteractionEntity].getSimpleName} interaction
       |     on reply.id = interaction.postId
       |       and interaction.userId = :$userParam
       |   where
       |     reply.purged is null
       |     and (interaction.viewed is null or interaction.viewed = false)
       |     and reply.threadId = post.threadId
       |     and reply.postPath like concat(post.postPath, '_%')
       |     and reply.userId != :$userParam
       |     and $notInappropriateReplyClause
       |     and $notHiddenReplyClause
       |     ))
       | from ${classOf[PostEntity].getSimpleName} as post
       | where post.purged is null and post.id in (:$postIdParam)
     """.stripMargin.replaceAll("\n", " ")

  private val unreadPostsQuery: String =
    s""" from ${classOf[PostEntity].getSimpleName} post
       | left join ${classOf[PostInteractionEntity].getSimpleName} interaction
       |   on post.id = interaction.postId
       |     and interaction.userId = :$userParam
       |   where post.purged is null
       |     and (interaction.viewed is null or interaction.viewed = false)
       |     and post.contextId = :$contextIdParam
       |     and post.edgePath = :$edgePathParam
       |     and post.userId != :$userParam
       |    and $notInappropriateClause
       |    and $notHiddenClause
  """.stripMargin.replaceAll("\n", " ")
end UnreadPostDaoImpl

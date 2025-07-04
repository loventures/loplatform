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
import loi.cp.discussion.dto.FilteredSeq
import loi.cp.reference.{AssetReference, ContentIdentifier}
import org.hibernate.Session
import org.hibernate.query.Query

import scala.jdk.CollectionConverters.*
import scala.reflect.{ClassTag, classTag}

@Service
trait BookmarkedPostsDao:

  /** List of bookmarked posts
    */
  def bookmarkedPosts(
    discussionId: ContentIdentifier,
    userId: UserId,
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[PostValue]
end BookmarkedPostsDao

@Service
class BookmarkedPostsDaoImpl(session: => Session) extends BookmarkedPostsDao:

  import PostDao.*

  override def bookmarkedPosts(
    discussionId: ContentIdentifier,
    userId: UserId,
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[PostValue] =
    def constructQuery[A: ClassTag](queryString: String): Query[A] =
      session
        .createQuery(queryString, classTag[A].runtimeClass.asInstanceOf[Class[A]])
        .setParameter(contextIdParam, discussionId.contextId.value.longValue())
        .setParameter(edgePathParam, discussionId.edgePath.toString)
        .setParameter(userParam, userId.value.longValue())

    val postQuery: Query[PostValueRaw] =
      constructQuery[PostValueRaw](
        s"$postSelectionPrefix $noStatPostfix )$bookmarkedQuery ${constructOrderPart(apiOrders)}"
      )
    val countQuery: Query[JLong]       = constructQuery(s"select count(post) $bookmarkedQuery")

    if apiPage.isSet then
      postQuery
        .setMaxResults(apiPage.getLimit)
        .setFirstResult(apiPage.getOffset)

    FilteredSeq[PostValue](
      countQuery.getSingleResult.longValue,
      postQuery.getFirstResult,
      postQuery.getResultList.asScala.toSeq
    )
  end bookmarkedPosts

  private val bookmarkedQuery: String =
    s"""from ${classOf[PostEntity].getSimpleName} post
       |    join ${classOf[PostInteractionEntity].getSimpleName} interaction
       |     on post.id = interaction.postId
       | where post.purged is null
       | and ${AssetReference.DATA_TYPE_CONTEXT_ID} = :$contextIdParam AND ${AssetReference.DATA_TYPE_EDGE_PATH} = :$edgePathParam
       | and interaction.userId = :$userParam
       | and interaction.bookmarked = true
       | and (post.removed is null or post.removed = false)""".stripMargin.replaceAll("\n", " ")
end BookmarkedPostsDaoImpl

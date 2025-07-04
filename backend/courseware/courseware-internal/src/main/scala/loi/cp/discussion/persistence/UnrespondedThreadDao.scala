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
trait UnrespondedThreadDao:

  /** List of unnresponded threads
    */
  def unrespondedThreads(
    discussionId: ContentIdentifier,
    targetUserId: UserId,
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[PostValue]
end UnrespondedThreadDao

@Service
class UnrespondedThreadDaoImpl(session: => Session) extends UnrespondedThreadDao:

  import PostDao.*

  override def unrespondedThreads(
    discussionId: ContentIdentifier,
    targetUserId: UserId,
    apiOrders: Seq[ApiOrder],
    apiPage: ApiPage
  ): FilteredSeq[PostValue] =
    def constructQuery[A: ClassTag](queryString: String): Query[A] =
      session
        .createQuery(queryString, classTag[A].runtimeClass.asInstanceOf[Class[A]])
        .setParameter(contextIdParam, discussionId.contextId.value.longValue)
        .setParameter(edgePathParam, discussionId.edgePath.toString)
        .setParameter(userParam, Long2long(targetUserId.id))

    val postQuery: Query[PostValueRaw] =
      constructQuery[PostValueRaw](
        s"$postSelectionPrefix $noStatPostfix )$unrespondedThreadQueryBySubselect ${constructOrderPart(apiOrders)}"
      )
    val countQuery: Query[JLong]       = constructQuery(s"select count(post) $unrespondedThreadQueryBySubselect")

    if apiPage.isSet then
      postQuery
        .setMaxResults(apiPage.getLimit)
        .setFirstResult(apiPage.getOffset)

    FilteredSeq[PostValue](
      countQuery.getSingleResult.longValue,
      postQuery.getFirstResult,
      postQuery.getResultList.asScala.toSeq
    )
  end unrespondedThreads

  private val unrespondedThreadQueryBySubselect: String =
    s"""from ${classOf[PostEntity].getSimpleName} post
       | where ${AssetReference.DATA_TYPE_CONTEXT_ID} = :$contextIdParam
       | AND post.purged is null
       | and ${AssetReference.DATA_TYPE_EDGE_PATH} = :$edgePathParam
       | and post.depth = 0
       | and post.moderatorPost = false
       | and (select count(reply.id)
       |      from ${classOf[PostEntity].getSimpleName} reply
       |      where reply.purged is null
       |      and reply.threadId = post.threadId
       |      and reply.depth >= 1
       |      and reply.moderatorPost = true
       |      and $notInappropriateReplyClause
       |      and $notHiddenReplyClause
       |      ) = 0
       | and $notInappropriateClause
       | and $notHiddenClause""".stripMargin
      .replaceAll("\n", " ")
end UnrespondedThreadDaoImpl

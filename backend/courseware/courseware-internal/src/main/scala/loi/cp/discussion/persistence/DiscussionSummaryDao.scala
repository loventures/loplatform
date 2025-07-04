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
package persistence

import java.util.Date

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.discussion.{DiscussionInteractionEntity, PostEntity, PostInteractionEntity}
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.discussion.dto.{DiscussionSummary, GeneralDiscussionSummary}
import loi.cp.reference.ContentIdentifier
import org.hibernate.Session

import scala.jdk.CollectionConverters.*
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*
import scaloi.syntax.collection.*

@Service
trait DiscussionSummaryDao:
  def summarizeForLearner(contentIds: Seq[ContentIdentifier], user: UserId): Map[ContentIdentifier, DiscussionSummary]

  def summarizeForReviewer(contentIds: Seq[ContentIdentifier], user: UserId): Map[ContentIdentifier, DiscussionSummary]

final case class VisitationRow(
  edgePath: String,
  visited: Date
)

final case class StudentSummaryRow(
  latestUpdateTime: Date,
  userCount: JLong,
  postCount: JLong,
  newPostCount: JLong
)

@Service
class DiscussionSummaryDaoImpl(session: => Session) extends DiscussionSummaryDao:

  import PostDao.*

  override def summarizeForLearner(
    contentIds: Seq[ContentIdentifier],
    user: UserId
  ): Map[ContentIdentifier, DiscussionSummary] =
    if contentIds.isEmpty then Map.empty
    else
      // First find the student's last visited dates to each discussion board

      val visitedSql =
        s"""SELECT new ${classOf[VisitationRow].getName}(
           |  di.edgePath,
           |  di.visited
           |)
           |FROM ${classOf[DiscussionInteractionEntity].getSimpleName} di
           |WHERE di.contextId = :$contextIdParam
           |  AND di.edgePath IN :$edgePathParam
           |  AND di.userId = :$userParam
           |""".stripMargin

      val visitations = session
        .createQuery(visitedSql, classOf[VisitationRow])
        .setParameter(contextIdParam, contentIds.head.contextId.value)
        .setParameter(edgePathParam, contentIds.map(_.edgePath.toString).asJava)
        .setParameter(userParam, user.value)
        .getResultList
        .foldToMap(row => row.edgePath -> row.visited)

      // Then one-by-one pull discussion board statistics. This used to be a giant grouped nested
      // query but performance was abysmal. Running N (for a small N since courses only have a small
      // number of discussion boards) simple queries instead ought to perform much better.

      contentIds foldToMap { contentId =>
        val visited = visitations.get(contentId.edgePath.toString)

        // Our tests create posts in boards without first visiting them so we can't do the naïve COUNT(*) if
        // you have never visited the board.

        val summarySql   =
          s"""SELECT new ${classOf[StudentSummaryRow].getName}(
             |  MAX(p.updated),
             |  COUNT(DISTINCT p.userId),
             |  COUNT(*),
             |  SUM(
             |    CASE
             |      WHEN p.userId != :$userParam ${visited.isDefined ?? s"AND p.created >= :$sinceParam"}
             |      THEN 1
             |      ELSE 0
             |    END
             |  )
             |)
             |FROM ${classOf[PostEntity].getSimpleName} p
             |WHERE p.purged IS NULL
             |  AND p.contextId = :$contextIdParam
             |  AND p.edgePath = :$edgePathParam
             |  AND (p.inappropriate is null or p.inappropriate = false)
             |  AND (p.removed is null or p.removed = false)
         """.stripMargin
        val summaryQuery = session
          .createQuery(summarySql, classOf[StudentSummaryRow])
          .setParameter(contextIdParam, contentId.contextId.value)
          .setParameter(edgePathParam, contentId.edgePath.toString)
          .setParameter(userParam, user.value)
        visited foreach { since =>
          summaryQuery.setParameter(sinceParam, since)
        }
        val row          = summaryQuery.getSingleResult

        contentId -> GeneralDiscussionSummary(
          lastPostCreationDate = Option(row.latestUpdateTime).map(_.toInstant),
          lastVisited = visited.map(_.toInstant),
          participantCount = row.userCount,
          postCount = row.postCount,
          newPostCount = row.newPostCount
        )
      }

  override def summarizeForReviewer(
    contentIds: Seq[ContentIdentifier],
    user: UserId
  ): Map[ContentIdentifier, DiscussionSummary] =
    if contentIds.isEmpty then Map.empty
    else

      val unreadPostsFragment: String =
        s"""SELECT COUNT(*)
           |FROM ${classOf[PostEntity].getSimpleName} post
           |LEFT JOIN ${classOf[PostInteractionEntity].getSimpleName} interaction
           |ON (
           |  interaction.postId = post.id AND interaction.userId = :$userParam
           |)
           |WHERE post.purged IS NULL
           |  AND (interaction.viewed is null OR interaction.viewed = false)
           |  AND post.userId != :$userParam
           |  AND post.contextId = p.contextId
           |  AND post.edgePath = p.edgePath
           |  AND $notInappropriateClause
           |  AND $notHiddenClause
         """.stripMargin

      val unrespondedThreadFragment: String =
        s"""SELECT COUNT(*)
           |FROM ${classOf[PostEntity].getSimpleName} post
           |WHERE post.purged IS NULL
           |  AND post.contextId = p.contextId
           |  AND post.edgePath = p.edgePath
           |  AND post.depth = 0
           |  AND post.moderatorPost = false
           |  AND (select count(reply.id)
           |      from ${classOf[PostEntity].getSimpleName} reply
           |      where reply.purged is null
           |      and reply.threadId = post.threadId
           |      and reply.depth >= 1
           |      and reply.moderatorPost = true
           |      and $notInappropriateReplyClause
           |      and $notHiddenReplyClause
           |      ) = 0
           |  AND $notInappropriateClause
           |  AND $notHiddenClause
         """.stripMargin

      val summaryQuery: String =
        s"""
           |SELECT new ${classOf[RawReviewerSummaryRow].getName}(
           |  p.contextId,
           |  p.edgePath,
           |  MAX(p.updated),
           |  di.visited,
           |  COUNT(DISTINCT p.userId),
           |  COUNT(*),
           |  ($unreadPostsFragment),
           |  ($unrespondedThreadFragment)
           |)
           |FROM ${classOf[PostEntity].getSimpleName} p
           |LEFT JOIN ${classOf[DiscussionInteractionEntity].getSimpleName} di
           |ON (
           |  di.edgePath = p.edgePath
           |    AND di.userId = :$userParam
           |)
           |WHERE p.purged IS NULL
           |  AND p.contextId IN :$contextIdParam
           |  AND p.edgePath IN :$edgePathParam
           |  AND (p.inappropriate is null or p.inappropriate = false)
           |  AND (p.removed is null or p.removed = false)
           |GROUP BY p.contextId, p.edgePath, di.visited
         """.stripMargin
      session
        .createQuery(summaryQuery, classOf[RawReviewerSummaryRow])
        .setParameter(contextIdParam, contentIds.map(_.contextId.value).asJava)
        .setParameter(edgePathParam, contentIds.map(_.edgePath.toString).asJava)
        .setParameter(userParam, user.value.longValue())
        .getResultList
        .asScala
        .map(_.toReviewerSummaryTuple)
        .toMap
end DiscussionSummaryDaoImpl

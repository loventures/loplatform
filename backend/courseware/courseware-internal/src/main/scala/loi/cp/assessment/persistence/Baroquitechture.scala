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

package loi.cp.assessment.persistence

import java.util.Date
import com.learningobjects.cpxp.util.HibernateSessionOps.{entityName, nativeName}
import loi.cp.assessment.attempt.{AbstractAttemptEntity, AttemptState, AttemptyWempty}
import loi.cp.assessment.{AssessmentGradingPolicy, AssessmentParticipation}
import loi.cp.reference.EdgePath
import org.hibernate.Session

import scala.jdk.CollectionConverters.*
import scalaz.std.map.*
import scaloi.syntax.collection.*

import scala.annotation.nowarn
import scala.reflect.ClassTag

/** I really want to follow the established patter of duplicating the shit out of everything, so in your mind just
  * envision this as an inline macro expansion at call site.
  */
object Baroquitechture:

  @nowarn def getParticipationData[T <: AbstractAttemptEntity: ClassTag: AttemptyWempty](
    session: Session,
    contextId: Long,
    edgePaths: Seq[EdgePath]
  ): Map[EdgePath, AssessmentParticipation] =
    edgePaths ?? {
      session
        .createQuery(
          s"""
             | SELECT
             |   edgePath,
             |   COUNT(*) AS validCount,
             |   COUNT(DISTINCT userId) AS participantCount,
             |   MAX(${AttemptyWempty[T].updateTimeSql}) AS latestAttemptDate
             | FROM
             |   ${entityName[T]}
             | WHERE
             |   contextId = :contextId AND
             |   edgePath IN :edgePaths AND
             |   valid = TRUE
             | GROUP BY
             |   edgePath
             """.stripMargin
        )
        .setParameter("contextId", Long.box(contextId))
        .setParameter("edgePaths", edgePaths.map(_.toString).asJavaCollection)
        .getResultList
        .asScala
        .collectToMap { case Array(edgePath: String, validCount: Number, participantCount: Number, updateTime: Date) =>
          EdgePath.parse(edgePath) ->
            AssessmentParticipation(
              validCount.intValue,
              participantCount.intValue,
              Option(updateTime).map(_.toInstant)
            )
        }
    }

  def attemptsAwaitingGrade[T <: AbstractAttemptEntity: ClassTag](
    session: Session,
    contextId: Long,
    edgePaths: Seq[EdgePath],
    policy: AssessmentGradingPolicy
  ): Map[EdgePath, Int] =
    edgePaths ?? {
      session
        .createNativeQuery(policyQuery[T](policy))
        .setParameter("contextId", Long.box(contextId))
        .setParameter("edgePaths", edgePaths.map(_.toString).asJavaCollection)
        .getResultList
        .asScala
        .collectToMap { case Array(edgePath: String, count: Number) =>
          EdgePath.parse(edgePath) -> count.intValue
        }
    }

  // It should be noted that these queries are far from perfect, they still scale linearly
  // in the number of attempts which is far from ideal in a long-running course.
  // A trivial optimisation would be to just limit this to a window of the last N months
  // which would bound things much more reasonably. Otherwise, it is possible that there are
  // tools such as lateral joins which would allow us to not generate CTE queries that rank
  // over all attempts but....
  private def policyQuery[T <: AbstractAttemptEntity: ClassTag](policy: AssessmentGradingPolicy): String =
    policy match
      case AssessmentGradingPolicy.FirstAttempt | AssessmentGradingPolicy.MostRecent =>
        // These policies just consider the state of the first or last attempt
        val order = if policy == AssessmentGradingPolicy.FirstAttempt then "ASC" else "DESC"
        s"""
         | WITH Attempt AS (
         |   SELECT DISTINCT ON (edgePath, userId)
         |     edgePath,
         |     userId,
         |     attemptState
         |   FROM
         |     ${nativeName[T]}
         |   WHERE
         |     contextId = :contextId AND
         |     edgePath in :edgePaths AND
         |     valid = TRUE AND
         |     attemptState <> '${AttemptState.Open}'
         |   ORDER BY edgePath, userId, submitTime $order
         | )
         | SELECT
         |   edgePath,
         |   COUNT(*)
         | FROM
         |   Attempt
         | WHERE
         |   attemptState = '${AttemptState.Submitted}'
         | GROUP BY
         |   edgePath
         |""".stripMargin

      case AssessmentGradingPolicy.Highest | AssessmentGradingPolicy.Average =>
        // These policies require all attempts to be counted
        s"""
         | SELECT
         |   edgePath,
         |   COUNT(*)
         | FROM
         |   ${nativeName[T]}
         | WHERE
         |   contextId = :contextId AND
         |   edgePath in :edgePaths AND
         |   valid = TRUE AND
         |   attemptState = '${AttemptState.Submitted}'
         | GROUP BY
         |   edgePath
         |""".stripMargin

      case AssessmentGradingPolicy.FullCreditOnCompletion =>
        // Any attempt that is submitted if no attempt is finalized
        s"""
         | WITH Attempt AS (
         |   SELECT
         |     edgePath,
         |     userId,
         |     bool_or(CASE attemptState WHEN '${AttemptState.Submitted}' THEN TRUE ELSE FALSE END) AS submitted,
         |     bool_or(CASE attemptState WHEN '${AttemptState.Finalized}' THEN TRUE ELSE FALSE END) AS finalised
         |   FROM
         |     ${nativeName[T]}
         |   WHERE
         |     contextId = :contextId AND
         |     edgePath in :edgePaths AND
         |     valid = TRUE AND
         |     attemptState <> '${AttemptState.Open}'
         |   GROUP BY
         |     edgePath,
         |     userId
         | )
         | SELECT
         |   edgePath,
         |   COUNT(*)
         | FROM
         |   Attempt
         | WHERE
         |   submitted AND NOT finalised
         | GROUP BY
         |   edgePath
         |""".stripMargin
end Baroquitechture

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

package loi.cp.assessment.attempt

import java.time.Instant

import loi.cp.assessment.Assessment
import loi.cp.context.ContextId
import loi.cp.reference.EdgePath

/** A summary of a user's attempts against an assessment.
  *
  * @param context
  *   the context the attempt data is from
  * @param edgePath
  *   the location of the assessment
  * @param user
  *   the subject id
  * @param attemptStateCounts
  *   a count of the number of valid attempt the user has in the given state
  */
case class UserAttemptCounts(
  context: ContextId,
  edgePath: EdgePath,
  user: Long,
  latestSubmissionTime: Option[Instant],
  attemptStateCounts: Map[AttemptState, Int]
)

object UserAttemptCounts:
  private val emptyCounts = AttemptState.values.map(state => state -> 0).toMap

  def apply(
    contextId: Long,
    edgePath: EdgePath,
    userId: Long,
    stateAndCount: Seq[(String, Int, Option[Instant])]
  ): UserAttemptCounts =
    val stateStringCountMap: Map[String, Int] = stateAndCount.map(t => (t._1, t._2)).toMap
    val stateCountMap: Map[AttemptState, Int] =
      (for state <- AttemptState.values
      yield state -> stateStringCountMap.getOrElse(state.entryName, 0)).toMap

    val latestSubmissionTimes: Seq[Instant]   = stateAndCount.flatMap(_._3)
    val latestSubmissionTime: Option[Instant] = latestSubmissionTimes.sorted.lastOption

    UserAttemptCounts(ContextId(contextId), edgePath, userId, latestSubmissionTime, stateCountMap)
  end apply

  def empty(assessment: Assessment, user: Long): UserAttemptCounts =
    UserAttemptCounts(assessment.contextId, assessment.edgePath, user, None, emptyCounts)

  def empty(context: ContextId, edgePath: EdgePath, user: Long): UserAttemptCounts =
    UserAttemptCounts(context, edgePath, user, None, emptyCounts)
end UserAttemptCounts

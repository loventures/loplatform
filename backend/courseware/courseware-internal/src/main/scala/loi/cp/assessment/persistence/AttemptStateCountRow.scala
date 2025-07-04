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

import java.time.Instant
import java.util.Date
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import loi.cp.assessment.attempt.{AttemptState, UserAttemptCounts}
import loi.cp.context.ContextId
import loi.cp.reference.EdgePath

/** A row representing the number of attempts in a single state
  *
  * @param contextIdValue
  *   the context the attempt is in
  * @param edgePathStr
  *   the location of the assessment in the context
  * @param userId
  *   the id of the subject
  * @param stateName
  *   the state this row represents
  * @param latestSubmissionTime
  *   the latest submission time for attempts in this state
  * @param count
  *   the number of attempts in this state
  */
case class AttemptStateCountRow(
  contextIdValue: JLong,
  edgePathStr: String,
  userId: JLong,
  stateName: String,
  latestSubmissionTime: Date,
  count: JLong
):
  val edgePath: EdgePath = EdgePath.parse(edgePathStr)

object AttemptStateCountRow:

  /** Converts from a collection of DAO rows (AttemptStateCountRow) to a collection service objects (UserAttemptCounts).
    *
    * @param rows
    *   the raw data to aggregate
    * @return
    *   the service object representing the state counts/latest submission aggregation
    */
  def toUserAttemptCounts(rows: Seq[AttemptStateCountRow]): Seq[UserAttemptCounts] =
    rows
      .groupBy(row => (Long.unbox(row.contextIdValue), row.edgePath, Long.unbox(row.userId)))
      .map({ case ((contextIdValue, edgePath, userId), userAssessmentRows) =>
        val submitTimes: Seq[Instant]             =
          userAssessmentRows.flatMap(r => Option(r.latestSubmissionTime).map(_.toInstant))
        val latestSubmissionTime: Option[Instant] = submitTimes.sorted.lastOption

        val stateCounts: Map[AttemptState, Int] =
          (for state <- AttemptState.values
          yield state -> userAssessmentRows.count(_.stateName == state.entryName)).toMap

        UserAttemptCounts(ContextId(contextIdValue), edgePath, userId, latestSubmissionTime, stateCounts)
      })
      .toSeq
end AttemptStateCountRow

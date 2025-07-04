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

package loi.cp.lti.outcomes

import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.lti.outcomes.LtiOutcomesParser.{BasicOutcomesRequest, GradeSourcedId, ProcessResult}
import loi.cp.reference.EdgePath
import scaloi.syntax.OptionOps.*
import scaloi.syntax.StringOps.*

@Service
trait LtiOutcomesParser:

  def parseOutcome(requestBody: String): ProcessResult[BasicOutcomesRequest]

  /** Returns a grade (which would come from the sourcedid refrencing a grade cell database id), or a decoded
    * lightweight outcome info
    *
    * GradeComponent will be used when launched from the context of a Heavyweight Course, and LightweightOutcomeInfo
    * will be used when launched from the context of a lightweight course.
    * @param sourcedId
    * @return
    */
  def parseSourcedId(sourcedId: String): ProcessResult[GradeSourcedId]
end LtiOutcomesParser

object LtiOutcomesParser:
  type ProcessResult[A] = String Either A

  case class Score(points: Double, maxPoints: Double)

  trait BasicOutcomesRequest:
    def id: Option[String]
  case class ReadResult(id: Option[String], sourcedId: String)                    extends BasicOutcomesRequest
  case class DeleteResult(id: Option[String], sourcedId: String)                  extends BasicOutcomesRequest
  case class ReplaceResult(id: Option[String], sourcedId: String, result: String) extends BasicOutcomesRequest

  case class GradeSourcedId(studentId: Long, contextId: Long, edgePath: EdgePath):
    import GradeSourcedId.Separator
    def encode: String = s"$studentId$Separator$contextId$Separator$edgePath"

  object GradeSourcedId:
    final val Separator = ":"

    def decode(outcomeInfo: String): Option[GradeSourcedId] =
      flondOpt(outcomeInfo `split` Separator) { case Array(studentIdStr, contextIdStr, edgePathStr) =>
        for
          studentId <- studentIdStr.toLong_?
          contextId <- contextIdStr.toLong_?
          ci         = EdgePath.parse(edgePathStr)
        yield GradeSourcedId(studentId, contextId, ci)
      }
  end GradeSourcedId
end LtiOutcomesParser

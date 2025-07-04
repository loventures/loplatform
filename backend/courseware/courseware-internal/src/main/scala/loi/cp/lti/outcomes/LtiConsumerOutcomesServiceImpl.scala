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

package loi.cp.lti
package outcomes

import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.lwgrade.Grade.{Graded, NoCredit, Pending, Unassigned, Ungraded}
import loi.cp.lwgrade.*
import loi.cp.progress.{LightweightProgressService, ProgressChange}
import loi.cp.reference.EdgePath
import scalaz.syntax.equal.*

import java.time.Instant
import scala.util.Try

@Service
class LtiConsumerOutcomesServiceImpl(
  lightweightProgressService: LightweightProgressService,
  lwGradeService: GradeService
) extends LtiConsumerOutcomesService:

  import LtiOutcomesParser.*

  override def readGrade(target: GradeTarget): ProcessResult[String] =
    for
      grade <- findLightweightGrade(target)
      score <- computeScore(grade, target)
    yield score

  override def deleteGrade(target: GradeTarget): ProcessResult[Unit] =
    for
      grade <- findLightweightGrade(target)
      _     <- deleteLwGrade(target)
      _     <- setLightweightProgress(target, Nil)
    yield ()

  override def replaceGrade(lwgt: GradeTarget, ltiScore: String): ProcessResult[Unit] =
    val columns = GradeStructure(lwgt.section.contents)
    for
      column <- columns.findColumnForEdgePath(lwgt.edgePath) toRight s"column for target: $lwgt not found"
      grade  <- findLightweightGrade(lwgt)
      score  <- parseLtiScore(ltiScore)
      newGb  <- updateLwGrade(lwgt, score, Instant.now) // TODO: Not sure if this is the right time
      _      <- setLightweightProgress(lwgt, List(ProgressChange.visited))
    yield ()

  /** Takes a score between 0 and 1 and applies it to the gradetarget, scaling using the max value of the column
    * supplied.
    */
  private def updateLwGrade(
    target: GradeTarget,
    score: Double,
    when: Instant
  ): ProcessResult[Grade] =
    lwGradeService
      .setGradePercent(target.student, target.section, target.content, score, when)
      .leftMap(_.msg)
      .toEither

  private def deleteLwGrade(target: GradeTarget): ProcessResult[Grade] =
    lwGradeService
      .unsetGrade(target.student, target.section, target.content)
      .leftMap(_.msg)
      .toEither

  private def findGradebook(target: GradeTarget): StudentGradebook =
    lwGradeService.getGrades(target.student, target.section, target.section.contents)

  private def findLightweightGrade(target: GradeTarget): ProcessResult[Grade] =
    findGradebook(target).grades
      .find(_._1 === target.edgePath)
      .toRight(s"Grade for id: $target not found")
      .map(_._2)

  /** Computes the score to be embedded in a basic outcomes request. Nonexistent scores are returned as an empty string:
    * https://www.imsglobal.org/specs/ltiv1p1/implementation-guide#toc-27
    */
  private def computeScore(grade: Grade, target: GradeTarget): ProcessResult[String] =
    grade match
      case Graded(score, max, _)   => Right(score / max).map(_.toString)
      case Ungraded(_)             => Right("")
      case Pending(_, _)           => Right("")
      case Unassigned              => Right("")
      // TODO: Just because it doesn't mean anything for us may not mean it doesn't mean anything for others.
      case NoCredit(score, max, _) => Right(score / max).map(_.toString)
      case _                       => Left(s"Grade for target: $target doesn't exist")

  private def setLightweightProgress(lwgt: GradeTarget, changes: List[EdgePath => ProgressChange]) =
    lightweightProgressService
      .updateProgress(lwgt.section, lwgt.student, findGradebook(lwgt), changes.map(_.apply(lwgt.edgePath)))
      .leftMap(e => s"Failed to update progress for ${lwgt.student.id} on ${lwgt.edgePath}: ${e.msg}")
      .toEither

  def parseLtiScore(inputScore: String): ProcessResult[Double] =
    Try(inputScore.toDouble).toOption
      .filter(sc => sc >= 0 && sc <= 1.0)
      .toRight(s"LTI score must be between 0 and 1.0")
end LtiConsumerOutcomesServiceImpl

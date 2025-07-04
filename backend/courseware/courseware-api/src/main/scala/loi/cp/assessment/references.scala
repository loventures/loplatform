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

package loi.cp.assessment

import java.lang

import argonaut.Argonaut.*
import argonaut.*
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.annotation.StringConvert
import loi.cp.content.CourseContent
import loi.cp.quiz.QuizAsset
import loi.cp.reference.*
import loi.cp.submissionassessment.SubmissionAssessmentAsset

import scalaz.Functor

@StringConvert(`using` = classOf[AttemptId.AttemptIdStringConverter])
@JsonSerialize(`using` = classOf[AttemptId.AttemptIdJsonSeralizer])
@JsonDeserialize(`using` = classOf[AttemptId.AttemptIdJsonDeserializer])
case class AttemptId(value: Long) extends Id:
  override def getId: lang.Long = value

object AttemptId:
  def applyM[F[_]](f: F[Long])(implicit F: Functor[F]): F[AttemptId] =
    F.map(f)(AttemptId.apply)

  implicit val attemptIdCodec: CodecJson[AttemptId] =
    CodecJson(_.value.asJson, _.as[Long].map(AttemptId.apply))

  private[assessment] class AttemptIdStringConverter  extends ArgonautStringConverter[AttemptId]
  private[assessment] class AttemptIdJsonSeralizer    extends ArgonautReferenceSerializer[AttemptId, Long]
  private[assessment] class AttemptIdJsonDeserializer extends ArgonautReferenceDeserializer[AttemptId]
end AttemptId

/** A reference to a location in given course for an [[Assessment]].
  */
sealed trait AssessmentReference:
  val edgePath: EdgePath
object AssessmentReference:
  def apply(courseContent: CourseContent): Option[AssessmentReference] =
    QuizReference(courseContent).orElse(SubmissionAssessmentReference(courseContent))

/** A reference to a location in given course for a [[loi.cp.quiz.Quiz]].
  *
  * @param edgePath
  *   the location
  */
final case class QuizReference(edgePath: EdgePath) extends AssessmentReference
object QuizReference:
  def apply(courseContent: CourseContent): Option[QuizReference] =
    QuizAsset(courseContent.asset).map(_ => QuizReference(courseContent.edgePath))

/** A reference to a location in given course for a [[loi.cp.submissionassessment.SubmissionAssessment]].
  *
  * @param edgePath
  *   the location
  */
final case class SubmissionAssessmentReference(edgePath: EdgePath) extends AssessmentReference
object SubmissionAssessmentReference:
  def apply(courseContent: CourseContent): Option[SubmissionAssessmentReference] =
    SubmissionAssessmentAsset(courseContent.asset).map(_ => SubmissionAssessmentReference(courseContent.edgePath))

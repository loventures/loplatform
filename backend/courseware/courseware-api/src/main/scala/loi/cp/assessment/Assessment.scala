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

import loi.cp.assessment.settings.AttemptLimit
import loi.cp.content.CourseContent
import loi.cp.context.ContextId
import loi.cp.course.CourseSection
import loi.cp.quiz.Quiz
import loi.cp.reference.{ContentIdentifier, EdgePath}
import loi.cp.submissionassessment.SubmissionAssessment

/** An interactive activity which users respond to and are scored (either by an instructor or the system itself).
  */
trait Assessment:

  /** Returns the content used to make this assessment.
    *
    * @return
    *   the content used to make this assessment
    */
  def courseContent: CourseContent

  /** Returns the location of this assessment.
    *
    * @return
    *   the location of this assessment
    */
  def contentId: ContentIdentifier

  def section: CourseSection

  /** Returns the maximum number valid attempts any single user may have against this assessment.
    *
    * @return
    *   the maximum number valid attempts any single user may have against this assessment
    */
  def maxAttempts: AttemptLimit

  /** Returns the policy used to determine the grade from a set of attempts.
    *
    * @return
    *   the policy used to determine the grade from a set of attempts
    */
  def gradingPolicy: AssessmentGradingPolicy

  /** @return
    *   the context that the assessment is in
    */
  final def contextId: ContextId = contentId.contextId

  /** @return
    *   the location of the assessment in the course
    */
  final def edgePath: EdgePath = courseContent.edgePath
end Assessment

object Assessment:

  /** Partitions a collection of [[Assessment]] s into [[Quiz]] s and [[SubmissionAssessment]] s.
    *
    * @param assessments
    *   the collection to partition
    * @return
    *   the partitioned [[Assessment]] s
    */
  def partition(assessments: Seq[Assessment]): (Seq[Quiz], Seq[SubmissionAssessment]) =
    val quizzes: Seq[Quiz] =
      assessments collect { case q: Quiz =>
        q
      }

    val submissionAssessments: Seq[SubmissionAssessment] =
      assessments collect { case sa: SubmissionAssessment =>
        sa
      }

    (quizzes, submissionAssessments)
  end partition

  implicit class AssessmentCollectionOps(val assessments: Seq[Assessment]) extends AnyVal:

    /** Returns the edges paths if from only the given context. This method throws an [[IllegalArgumentException]] if
      * the assessment come from multiple contexts.
      *
      * @param course
      *   the expected course for the assessments
      * @return
      *   the edge paths from the assessments, if from a single context
      */
    def validateContextOrThrow(course: CourseSection): Seq[EdgePath] =
      val contextIdToAssessments: Map[Long, Seq[Assessment]] = assessments.groupBy(_.contextId.value)
      val unexpectedContextIds: Set[Long]                    = contextIdToAssessments.keys.toSet.filter(_ != course.id)

      if unexpectedContextIds.nonEmpty then
        throw new IllegalArgumentException(
          s"Assessments from multiple courses specified: ${assessments.map(_.toString).mkString(",")}"
        )
      else
        contextIdToAssessments
          .getOrElse(course.id, Nil)
          .map(_.edgePath)
    end validateContextOrThrow
  end AssessmentCollectionOps
end Assessment

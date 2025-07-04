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

package loi.cp.quiz.question

import loi.authoring.asset.Asset
import loi.cp.assessment.rubric.AssessmentRubric
import loi.cp.competency.Competency
import loi.cp.reference.VersionedAssetReference

/** A question in the quiz system. Each question should have a unique {{contentReference}}. Questions may not have
  * semantic meaning outside the context of either a [[loi.cp.quiz.Quiz]] or an [[loi.cp.quiz.attempt.QuizAttempt]].
  */
sealed trait Question:

  val asset: Asset[?]

  /** the reference to the unique content used to generate this question */
  val contentReference: VersionedAssetReference

  /** the number of relative points this question is worth in a quiz */
  val pointValue: Double

  /** the primary question text of the question */
  val text: Option[String]

  /** all rationales (correct and incorrect) associated with the question */
  val rationales: Seq[Rationale]

  /** all competencies associated with this question (including competencies on rubrics) */
  val competencies: Seq[Competency]
end Question

trait AutoScorable  extends Question
trait ManualGrading extends Question:
  val rubric: Option[AssessmentRubric]

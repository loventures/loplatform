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

package loi.cp.quiz.question.essay

import loi.asset.question.{EssayContent, EssayQuestion}
import loi.authoring.asset.Asset
import loi.cp.assessment.rubric.AssessmentRubric
import loi.cp.competency.Competency
import loi.cp.quiz.question.*
import loi.cp.reference.VersionedAssetReference

case class Essay(
  asset: Asset[?],
  contentReference: VersionedAssetReference,
  pointValue: Double,
  text: Option[String],
  rubric: Option[AssessmentRubric],
  rationales: Seq[Rationale],
  questionCompetencies: Seq[Competency]
) extends ManualGrading:
  override val competencies: Seq[Competency] =
    (rubric.map(_.competencies).getOrElse(Nil) ++ questionCompetencies).distinct
end Essay

object Essay:
  def apply(
    ref: VersionedAssetReference,
    content: Asset[EssayQuestion],
    rubric: Option[AssessmentRubric],
    assetRemediations: Seq[AssetRemediation],
    questionCompetencies: Seq[Competency]
  ): Essay =
    val questionContent: EssayContent = content.data.questionContent

    val pointValue: Double = questionContent.pointsPossible.toDouble

    val feedbackRationales: Seq[Rationale] =
      Seq(
        questionContent.richCorrectAnswerFeedback
          .flatMap(_.renderedHtml)
          .filter(_.nonEmpty)
          .map(CorrectRationale.apply),
        questionContent.richIncorrectAnswerFeedback
          .flatMap(_.renderedHtml)
          .filter(_.nonEmpty)
          .map(TextRemediation.apply)
      ).flatten

    Essay(
      content,
      ref,
      pointValue,
      questionContent.getPublishQuestionText(),
      rubric,
      feedbackRationales ++ assetRemediations,
      questionCompetencies
    )
  end apply
end Essay

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

package loi.cp.quiz.question.matching

import loi.asset.question.{DefinitionContent, MatchingContent, MatchingQuestion, TermContent}
import loi.authoring.asset.Asset
import loi.cp.competency.Competency
import loi.cp.quiz.attempt.{DistractorOrder, MatchingDistractorOrder}
import loi.cp.quiz.question.*
import loi.cp.reference.VersionedAssetReference

case class Matching(
  asset: Asset[?],
  contentReference: VersionedAssetReference,
  pointValue: Double,
  text: Option[String],
  allowDistractorRandomization: Boolean,
  awardsPartialCredit: Boolean,
  terms: Seq[Term],
  definitions: Seq[Definition],
  correctDefinitionForTerm: Map[Int, Int],
  rationales: Seq[Rationale],
  competencies: Seq[Competency]
) extends RandomizableQuestion:
  override def generateDistractorOrder(): MatchingDistractorOrder =
    MatchingDistractorOrder(DistractorOrder.randomIndices(terms), DistractorOrder.randomIndices(definitions))
end Matching

case class Term(text: String)

case class Definition(text: String)

object Matching:

  def apply(
    ref: VersionedAssetReference,
    content: Asset[MatchingQuestion],
    assetRemediations: Seq[AssetRemediation],
    competencies: Seq[Competency]
  ): Matching =
    val questionContent: MatchingContent = content.data.questionContent
    val text: Option[String]             = questionContent.getPublishQuestionText()

    val questionTerms: Seq[Term]             = terms(questionContent.terms)
    val questionDefinitions: Seq[Definition] = definitions(questionContent.definitionContent)

    val correctDefinitionForTerm: Map[Int, Int] =
      (for (termIndex, term) <- questionContent.terms.indices zip questionContent.terms
      yield termIndex -> term.correctIndex.toInt).toMap

    val pointValue: Double                    = questionContent.pointsPossible.toDouble
    val allowPartialCredit: Boolean           = content.data.allowPartialCredit
    val allowDistractorRandomization: Boolean = true
    val rationales: Seq[Rationale]            =
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

    Matching(
      content,
      ref,
      pointValue,
      text,
      allowDistractorRandomization,
      allowPartialCredit,
      questionTerms,
      questionDefinitions,
      correctDefinitionForTerm,
      rationales ++ assetRemediations,
      competencies
    )
  end apply

  def terms(terms: Seq[TermContent]): Seq[Term] =
    terms.map(term => Term(term.termText))

  def definitions(definitionContents: Seq[DefinitionContent]): Seq[Definition] =
    definitionContents.map(definition => Definition(definition.definitionText))
end Matching

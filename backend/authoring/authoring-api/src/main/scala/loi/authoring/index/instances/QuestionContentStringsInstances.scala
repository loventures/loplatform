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

package loi.authoring.index
package instances

import loi.asset.question.*
import loi.authoring.syntax.index.*

/** [[Strings]] evidence for the various asset question content types. */
trait QuestionContentStringsInstances:
  implicit val binDropContentStrings: Strings[BinDropContent] = new Strings[BinDropContent]:
    override def strings(a: BinDropContent): List[String] =
      questionContentStrings(a) ::: a.bins.strings ::: a.options.strings
    override def htmls(a: BinDropContent): List[String]   = questionContentHtmls(a) ::: a.bins.htmls ::: a.options.htmls

  implicit val binContentStrings: Strings[BinContent] = Strings.plaintext(a => a.label :: Nil)

  implicit val optionContentStrings: Strings[OptionContent] = Strings.plaintext(a => a.label :: Nil)

  implicit val essayContentStrings: Strings[EssayContent] = new Strings[EssayContent]:
    override def strings(a: EssayContent): List[String] = questionContentStrings(a, contentBlock = true)
    override def htmls(a: EssayContent): List[String]   = questionContentHtmls(a, contentBlock = true)

  implicit val fillInTheBlankContentStrings: Strings[FillInTheBlankContent] = new Strings[FillInTheBlankContent]:
    override def strings(a: FillInTheBlankContent): List[String] = questionContentStrings(a)
    override def htmls(a: FillInTheBlankContent): List[String]   = questionContentHtmls(a)

  implicit val hotspotQuestionContentStrings: Strings[HotspotQuestionContent] = new Strings[HotspotQuestionContent]:
    override def strings(a: HotspotQuestionContent): List[String] = questionContentStrings(a)
    override def htmls(a: HotspotQuestionContent): List[String]   = questionContentHtmls(a)

  implicit val matchingContentStrings: Strings[MatchingContent] = new Strings[MatchingContent]:
    override def strings(a: MatchingContent): List[String] =
      questionContentStrings(a) ::: a.terms.strings ::: a.definitionContent.strings
    override def htmls(a: MatchingContent): List[String]   =
      questionContentHtmls(a) ::: a.terms.htmls ::: a.definitionContent.htmls

  implicit val termsContentStrings: Strings[TermContent] = new Strings[TermContent]:
    override def strings(a: TermContent): List[String] = a.termText :: a.feedbackInline.strings
    override def htmls(a: TermContent): List[String]   = a.feedbackInline.htmls

  implicit val definitionContentStrings: Strings[DefinitionContent] = Strings.plaintext(a => a.definitionText :: Nil)

  implicit val choiceQuestionContentStrings: Strings[ChoiceQuestionContent] = new Strings[ChoiceQuestionContent]:
    override def strings(a: ChoiceQuestionContent): List[String] = questionContentStrings(a) ::: a.choices.strings
    override def htmls(a: ChoiceQuestionContent): List[String]   = questionContentHtmls(a) ::: a.choices.htmls

  implicit val choiceContentStrings: Strings[ChoiceContent] = new Strings[ChoiceContent]:
    // a.choiceText deprecated
    override def strings(a: ChoiceContent): List[String] =
      a.choiceContent.strings ::: a.correctChoiceFeedback.strings ::: a.incorrectChoiceFeedback.strings
    override def htmls(a: ChoiceContent): List[String]   =
      a.choiceContent.htmls ::: a.correctChoiceFeedback.htmls ::: a.incorrectChoiceFeedback.htmls

  implicit val orderingContentStrings: Strings[OrderingContent] = new Strings[OrderingContent]:
    override def strings(a: OrderingContent): List[String] = questionContentStrings(a) ::: a.choices.strings
    override def htmls(a: OrderingContent): List[String]   = questionContentHtmls(a) ::: a.choices.htmls

  implicit val orderingChoiceStrings: Strings[OrderingChoice] = new Strings[OrderingChoice]:
    // a.choiceText deprecated
    override def strings(a: OrderingChoice): List[String] =
      a.choiceContent.strings ::: a.correctChoiceFeedback.strings ::: a.incorrectChoiceFeedback.strings
    override def htmls(a: OrderingChoice): List[String]   =
      a.choiceContent.htmls ::: a.correctChoiceFeedback.htmls ::: a.incorrectChoiceFeedback.htmls

  implicit val shortAnswerContentStrings: Strings[ShortAnswerContent] = new Strings[ShortAnswerContent]:
    override def strings(a: ShortAnswerContent): List[String] = questionContentStrings(a) ::: a.answer :: Nil
    override def htmls(a: ShortAnswerContent): List[String]   = questionContentHtmls(a)

  // question title is derived from the question text and so ignorable
  def questionContentStrings(a: QuestionContent, contentBlock: Boolean = false): List[String] =
    (if contentBlock then a.questionContentBlockText.strings else a.questionComplexText.strings) :::
      a.richCorrectAnswerFeedback.strings :::
      a.richIncorrectAnswerFeedback.strings

  // question title is derived from the question text and so ignorable
  def questionContentHtmls(a: QuestionContent, contentBlock: Boolean = false): List[String] =
    (if contentBlock then a.questionContentBlockText.htmls else a.questionComplexText.htmls) :::
      a.richCorrectAnswerFeedback.htmls :::
      a.richIncorrectAnswerFeedback.htmls
end QuestionContentStringsInstances

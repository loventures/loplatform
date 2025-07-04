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

package loi.asset.survey

import cats.data.{Validated, ValidatedNel}
import com.learningobjects.cpxp.service.mime.MimeWebService
import loi.asset.contentpart.HtmlPart
import loi.authoring.AssetType
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.asset.{Asset, AssetExtractor}
import loi.authoring.blob.BlobService
import loi.authoring.edge.Group
import loi.authoring.index.{AssetDataDocument, Strings}
import loi.authoring.syntax.index.*
import scaloi.syntax.option.*

import java.util.UUID

final case class SurveyChoiceQuestion1(
  prompt: HtmlPart,
  choices: List[SurveyChoice],
  keywords: String = "",
  archived: Boolean = false,
  title: String = "",
)

object SurveyChoiceQuestion1:

  implicit val assetTypeForSurveyChoiceQuestion1: AssetType[SurveyChoiceQuestion1] =
    new AssetType[SurveyChoiceQuestion1](AssetTypeId.SurveyChoiceQuestion1):

      override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
        Group.Resources -> AssetTypeId.FileTypes
      )

      override def computeTitle(a: SurveyChoiceQuestion1): Option[String] = Some(a.prompt.plainText)

      override def receiveTitle(a: SurveyChoiceQuestion1, title: String): SurveyChoiceQuestion1 =
        a.copy(prompt = HtmlPart(title))

      override def edgeIds(a: SurveyChoiceQuestion1): Set[UUID] = a.prompt.edgeIds ++ a.choices.flatMap(_.label.edgeIds)

      override def render(a: SurveyChoiceQuestion1, targets: Map[UUID, Asset[?]]): SurveyChoiceQuestion1 =
        a.copy(
          prompt = a.prompt.render(targets),
          choices = a.choices.map(choice => choice.copy(label = choice.label.render(targets)))
        )

      override def validate(data: SurveyChoiceQuestion1): ValidatedNel[String, Unit] =
        val repetitions   = data.choices.groupBy(_.value).view.mapValues(_.size).filter(_._2 > 1).toMap
        lazy val errToken = repetitions.map(entry => s"""value "${entry._1}" occurs ${entry._2} times""").mkString("; ")
        Validated.condNel(repetitions.isEmpty, (), s"choice value must be unique: $errToken")

      override def index(
        data: SurveyChoiceQuestion1
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
        keywords = data.keywords.option,
        content = stringifyOpt(data)
      )

      override def htmls(
        data: SurveyChoiceQuestion1
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
        data.htmls

  implicit val surveyChoiceQuestion1Strings: Strings[SurveyChoiceQuestion1] = new Strings[SurveyChoiceQuestion1]:
    override def strings(a: SurveyChoiceQuestion1): List[String] =
      a.prompt.strings ::: a.choices.flatMap(_.label.strings)

    override def htmls(a: SurveyChoiceQuestion1): List[String] =
      a.prompt.htmls ::: a.choices.flatMap(_.label.htmls)

  object Asset extends AssetExtractor[SurveyChoiceQuestion1]
end SurveyChoiceQuestion1

/** @param value
  *   an internal value unique amongst the choices, to help us aggregate responses in spite of label changes
  */
final case class SurveyChoice(
  value: String,
  label: HtmlPart,
)

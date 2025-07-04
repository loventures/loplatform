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

import com.learningobjects.cpxp.service.mime.MimeWebService
import loi.asset.contentpart.HtmlPart
import loi.authoring.AssetType
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.asset.{Asset, AssetExtractor}
import loi.authoring.blob.BlobService
import loi.authoring.edge.Group
import loi.authoring.index.AssetDataDocument
import loi.authoring.syntax.index.*
import scaloi.syntax.option.*

import java.util.UUID

final case class SurveyEssayQuestion1(
  prompt: HtmlPart,
  keywords: String = "",
  archived: Boolean = false,
  title: String = "",
)

object SurveyEssayQuestion1:

  implicit val assetTypeForSurveyEssayQuestion1: AssetType[SurveyEssayQuestion1] =
    new AssetType[SurveyEssayQuestion1](AssetTypeId.SurveyEssayQuestion1):

      override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
        Group.Resources -> AssetTypeId.FileTypes
      )

      override def computeTitle(a: SurveyEssayQuestion1): Option[String] = Some(a.prompt.plainText)

      override def receiveTitle(a: SurveyEssayQuestion1, title: String): SurveyEssayQuestion1 =
        a.copy(prompt = HtmlPart(title))

      override def edgeIds(a: SurveyEssayQuestion1): Set[UUID] = a.prompt.edgeIds

      override def render(a: SurveyEssayQuestion1, targets: Map[UUID, Asset[?]]): SurveyEssayQuestion1 =
        a.copy(prompt = a.prompt.render(targets))

      override def index(
        data: SurveyEssayQuestion1
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
        keywords = data.keywords.option,
        content = stringifyOpt(data.prompt)
      )

      override def htmls(
        data: SurveyEssayQuestion1
      )(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
        data.prompt.htmls

  object Asset extends AssetExtractor[SurveyEssayQuestion1]
end SurveyEssayQuestion1

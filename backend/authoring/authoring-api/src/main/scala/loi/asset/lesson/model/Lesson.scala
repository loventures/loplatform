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

package loi.asset.lesson.model

import cats.data.ValidatedNel
import cats.syntax.apply.*
import com.learningobjects.cpxp.service.mime.MimeWebService
import loi.asset.license.License
import loi.authoring.AssetType
import loi.authoring.asset.*
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.BlobService
import loi.authoring.edge.Group
import loi.authoring.index.AssetDataDocument
import loi.authoring.validate.Validate
import scaloi.syntax.option.*

import javax.validation.constraints.Size

final case class Lesson(
  @Size(min = 1, max = 255)
  title: String,
  iconAlt: String = "",
  description: String = "",
  @Size(min = 0, max = 255)
  keywords: String = "",
  archived: Boolean = false,
  iconCls: String = "icon-book-open",
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  accessRight: Option[String] = None,
  contentStatus: Option[String] = None,
)

object Lesson:

  implicit val assetTypeForLesson: AssetType[Lesson] = new AssetType[Lesson](AssetTypeId.Lesson):

    override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
      Group.Elements  -> AssetTypeId.LessonElementTypes,
      Group.Resources -> AssetTypeId.FileTypes,
      Group.Teaches   -> AssetTypeId.CompetencyTypes,
    )

    override def validate(data: Lesson): ValidatedNel[String, Unit] = Validate.size("title", 1, 255)(data.title) *>
      Validate.size("keywords", max = 255)(data.keywords)

    override def index(
      data: Lesson
    )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
      title = data.title.option,
      description = data.description.option,
      keywords = data.keywords.option,
      license = data.license,
      author = data.author,
      attribution = data.attribution,
      instructions = None
    )

    override def htmls(data: Lesson)(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
      Nil

  object Asset extends AssetExtractor[Lesson]
end Lesson

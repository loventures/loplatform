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

package loi.asset.unit.model

import cats.data.ValidatedNel
import cats.syntax.apply.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
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

import javax.validation.constraints.{Min, Size}
import scala.Unit as Ewenit

final case class Unit1(
  @Size(min = 1, max = 255)
  title: String,
  iconAlt: String = "",
  description: String = "",
  @Size(min = 0, max = 255)
  keywords: String = "",
  archived: Boolean = false,
  iconCls: String = "icon-cube",
  @Min(value = 0L)
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  duration: Option[Long] = None,
  license: Option[License] = None,
  author: Option[String] = None,
  attribution: Option[String] = None,
  accessRight: Option[String] = None,
  contentStatus: Option[String] = None,
)

object Unit1:

  implicit val assetTypeForUnit: AssetType[Unit1] = new AssetType[Unit1](AssetTypeId.Unit):

    // Methinks the course-lw data model does not lend itself to lessons in units
    override val edgeConfig: Map[Group, Set[AssetTypeId]] = Map(
      Group.Elements  -> (AssetTypeId.CourseContentTypes - AssetTypeId.Unit - AssetTypeId.Lesson),
      Group.Resources -> AssetTypeId.FileTypes,
    )

    override def validate(data: Unit1): ValidatedNel[String, Ewenit] =
      Validate.size("title", 1, 255)(data.title) *> Validate.size("keywords", max = 255)(data.keywords)

    override def index(
      data: Unit1
    )(implicit blobService: BlobService, mimeWebService: MimeWebService): AssetDataDocument = AssetDataDocument(
      title = data.title.option,
      description = data.description.option,
      keywords = data.keywords.option,
      license = data.license,
      author = data.author,
      attribution = data.attribution,
      instructions = None
    )

    override def htmls(data: Unit1)(implicit blobService: BlobService, mimeWebService: MimeWebService): List[String] =
      Nil

  object Asset extends AssetExtractor[Unit1]
end Unit1

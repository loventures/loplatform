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

import argonaut.Argonaut.*
import argonaut.*
import com.sksamuel.elastic4s.fields.{DateField, KeywordField, LongField}
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import loi.authoring.project.Project
import scaloi.json.ArgoExtras.*

import java.time.ZoneId
import java.util.Date

final case class ProjectMetadata(
  code: Option[String],
  productType: Option[String],
  category: Option[String],
  subCategory: Option[String],
  revision: Option[Int],
  launchDate: Option[Date],
  liveVersion: Option[String], // aka status
  s3: Option[String],
)

object ProjectMetadata:
  def apply(project: Project): ProjectMetadata = new ProjectMetadata(
    code = project.code,
    productType = project.productType,
    category = project.category,
    subCategory = project.subCategory,
    revision = project.revision,
    launchDate = project.launchDate.map(ld => Date.from(ld.atStartOfDay(ZoneId.of("America/New_York")).toInstant)),
    liveVersion = project.liveVersion,
    s3 = project.s3,
  )

  implicit val projectMetadataEncodeJson: EncodeJson[ProjectMetadata] = EncodeJson { a =>
    Json(
      "code"        := a.code,
      "productType" := a.productType,
      "category"    := a.category,
      "subCategory" := a.subCategory,
      "revision"    := a.revision,
      "launchDate"  := a.launchDate,
      "liveVersion" := a.liveVersion,
      "s3"          := a.s3
    )
  }

  val mappingDefinition: MappingDefinition = MappingDefinition(properties =
    List(
      KeywordField("code"),
      KeywordField("productType"),
      KeywordField("category"),
      KeywordField("subCategory"),
      LongField("revision"),
      DateField("launchDate"),
      KeywordField("liveVersion"),
      KeywordField("s3"),
    )
  )
end ProjectMetadata

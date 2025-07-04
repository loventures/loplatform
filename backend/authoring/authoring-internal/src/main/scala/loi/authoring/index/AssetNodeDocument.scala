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
import com.sksamuel.elastic4s.Indexable
import com.sksamuel.elastic4s.fields.*
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import loi.authoring.asset.factory.AssetTypeId
import scalaz.syntax.id.*
import scalaz.syntax.std.option.*
import scaloi.json.ArgoExtras.*

import java.util.{Date, UUID}

/** Indexing document capturing asset metadata and the searchable [[AssetDataDocument]]. */
final case class AssetNodeDocument(
  project: Long,
  projectRetired: Boolean,
  projectMetadata: ProjectMetadata,
  branch: Long,
  branchArchived: Boolean,
  commit: Long,
  offering: Long,
  name: UUID,
  archived: Boolean,
  used: Boolean,
  typeId: AssetTypeId,
  created: Date,
  modified: Date,
  data: AssetDataDocument
)

object AssetNodeDocument:

  implicit val nodeDocumentEncoder: EncodeJson[AssetNodeDocument] = EncodeJson { a =>
    Json(
      "project"         := a.project,
      "projectRetired"  := a.projectRetired,
      "projectMetadata" := a.projectMetadata,
      "branch"          := a.branch,
      "branchArchived"  := a.branchArchived,
      "commit"          := a.commit,
      "offering"        := a.offering,
      "name"            := a.name,
      "archived"        := a.archived,
      "used"            := a.used,
      "typeId"          := a.typeId,
      "created"         := a.created,
      "modified"        := a.modified,
      "data"            := a.data
    )
  }

  val mappingDefinition: MappingDefinition = MappingDefinition(properties =
    List(
      LongField("project"),
      BooleanField("projectRetired"),
      ObjectField(
        "projectMetadata",
        properties = ProjectMetadata.mappingDefinition.properties
      ),
      LongField("branch"),
      BooleanField("branchArchived"),
      LongField("commit"),
      LongField("offering"),
      KeywordField("name"),
      BooleanField("archived"),
      BooleanField("used"),
      DateField("created"),
      DateField("modified"),
      KeywordField("typeId"),
      ObjectField(
        "data",
        properties = AssetDataDocument.mappingDefinition.properties
      )
    )
  )

  private def flatten(field: JsonField)(o: JsonObject): JsonObject =
    o(field).cata(_.assoc.cata(_.foldLeft(o - field)((j, t) => j :+ (s"$field.${t._1}" -> t._2)), o), o)

  implicit val nodeDocumentIndexable: Indexable[AssetNodeDocument] = document =>
    // flatten the sub-objects into the parent
    val json = document.asJson withObject { o =>
      o |> flatten("data") |> flatten("projectMetadata")
    }
    json.nospaces
end AssetNodeDocument

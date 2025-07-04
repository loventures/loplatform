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

package loi.authoring.search.web

import argonaut.CodecJson
import loi.authoring.edge.Group
import loi.authoring.search.model.Ordering
import scaloi.json.ArgoExtras

import java.util.UUID

/** @param traverseAll
  *   if true, ignores the `traverse` value on edges and the search will traverse all edges. If false, honors the
  *   `traverse` value on edges, meaning some regions of the graph may not be searched
  */
case class GraphWebQuery(
  searchTerm: Option[String],
  typeIds: List[String],
  fields: Option[List[String]] = None,
  includeArchived: Option[Boolean] = None,
  traverseAll: Option[Boolean] = None,
  excludeTargetsOf: Option[ExcludeTargetsOf],
  orderedBy: Option[Ordering],
  limit: Option[Long],
  firstResult: Option[Long]
)

object GraphWebQuery:
  implicit val codec: CodecJson[GraphWebQuery] = CodecJson.casecodec9(GraphWebQuery.apply, ArgoExtras.unapply)(
    "searchTerm",
    "typeIds",
    "fields",
    "includeArchived",
    "traverseAll",
    "excludeTargetsOf",
    "orderedBy",
    "limit",
    "firstResult",
  )
end GraphWebQuery

case class ExcludeTargetsOf(
  // parent name
  parent: UUID,
  group: Group
)

object ExcludeTargetsOf:
  implicit val codec: CodecJson[ExcludeTargetsOf] =
    CodecJson.casecodec2(ExcludeTargetsOf.apply, ArgoExtras.unapply)(
      "parent",
      "group",
    )

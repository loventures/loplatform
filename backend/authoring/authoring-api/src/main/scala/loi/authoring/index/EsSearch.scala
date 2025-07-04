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

import com.learningobjects.cpxp.component.annotation.Service
import com.sksamuel.elastic4s.requests.searches.queries.*
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.term.{TermQuery, TermsQuery}
import loi.authoring.asset.factory.AssetTypeId
import scalaz.std.list.*
import scalaz.std.option.*
import scalaz.syntax.functor.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.syntax.collection.*

import java.util.UUID

@Service
/** Elastic search service. */
trait EsSearch:
  def search(query: EsQuery): EsResults
  def searchAll(query: EsQuery): List[EsHit]

/** Domain model of an elastic search query. Search is a must match, the rest are optional filters. */
final case class EsQuery(
  search: Option[String] = None,
  fields: List[String] = Nil,
  project: Option[Long] = None,
  projectRetired: Option[Boolean] = None,
  branch: Option[Long] = None,
  branchArchived: Option[Boolean] = None,
  commit: Option[Long] = None,
  offering: Option[Long] = None,
  archived: Option[Boolean] = None,
  used: Option[Boolean] = None,
  typeIds: List[AssetTypeId] = Nil,
  excludeNames: List[UUID] = Nil,
  includeNames: List[UUID] = Nil,
  from: Option[Int] = None,
  size: Option[Int] = None,
  sortBy: List[(String, Boolean)] = Nil,
  aggregate: Option[String] = None,
):
  import EsQuerySyntax.*

  /** Convert to an elastic search query. */
  def elastically: Query =
    // UUID searches are sad for some reason so turn them into a term query
    val must = search.map(query =>
      NameRegex
        .matches(query)
        .fold(
          TermQuery("name", query),
          SimpleStringQuery(query = query, fields = explicitFields.strengthR(None), quote_field_suffix = ".exact".some)
        )
    )
    BoolQuery().must(must.toList).not(excludes).filter(filters)
  end elastically

  // If we are not explicit, the search will include the AssetNode fields that don't make a lot of
  // sense, and non quoted queries will be run against both the english and english_exact fields so
  // a search for [the cat] (unquoted) will return all documents with the word "the" or "cat".
  // Instead we name our data fields explicitly and so the search will only go into "content.exact"
  // (which includes stop words) for quoted parts of the query.
  private def explicitFields: List[String] =
    if fields.nonEmpty then fields
    else AssetDataDocument.mappingDefinition.properties.map(p => s"data.${p.name}").toList

  private def filters: List[Query] =
    List(
      "project" -?>: project,
      "projectRetired" -?>: projectRetired,
      "branch" -?>: branch,
      "branchArchived" -?>: branchArchived,
      "commit" -?>: commit,
      "offering" -?>: offering,
      "archived" -?>: archived,
      "used" -?>: used,
      "typeId" -*?>: typeIds.map(_.entryName),
      "name" -*?>: includeNames.map(_.toString),
    ).flatten

  private def excludes: List[Query] = ("name" -*?>: excludeNames.map(_.toString)).toList

  private final val NameRegex = "(?:[0-9a-f]+-){4}[0-9a-f]+".r
end EsQuery

private object EsQuerySyntax:
  implicit class TermOptionQueryOps(private val self: Option[Any]) extends AnyVal:

    /** Map to a term query with the specified name. */
    def -?>:(name: String): Option[TermQuery] = self.map(TermQuery(name, _))

  implicit class TermListQueryOps(private val self: List[String]) extends AnyVal:

    /** If non-empty, map to a terms query with the specified name. */
    def -*?>:(name: String): Option[TermsQuery[String]] = self ?? TermsQuery(name, self).some
end EsQuerySyntax

/** Paged elastic-search query results. */
final case class EsResults(
  total: Long,
  hits: List[EsHit],
  aggregates: Map[Any, Long],
)

/** Minimal returned data for each search hit. */
final case class EsHit(
  project: Long,
  branch: Long,
  name: UUID,
  highlights: Map[String, Seq[String]],
  archived: Boolean,
)

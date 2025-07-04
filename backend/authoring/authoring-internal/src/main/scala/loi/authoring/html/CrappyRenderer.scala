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

package loi.authoring.html

import com.learningobjects.cpxp.service.CurrentUrlService
import loi.authoring.asset.Asset
import loi.authoring.edge.Group.Resources
import loi.authoring.edge.{AssetEdge, TraversedGraph}
import loi.authoring.render.LoEdgeIdUrl
import scaloi.syntax.collection.*

import scala.collection.mutable

// Combines the best of RenderService and HtmlService. JK. I have the graph so this
// avoids database round trips for the resources, plus just rewrites edge ids, not
// all the other crazy.
object CrappyRenderer:
  private final val BrokenImage = "/static/static/images/broken/160x160.png"

  def render(html: String, asset: Asset[?], graph: TraversedGraph)(implicit
    currentUrlService: CurrentUrlService
  ): String =
    graph
      .outEdgesInGroup(asset, Resources)
      .foldLeft(html)((html, resource) => html.replace(LoEdgeIdUrl(resource.edgeId), resourceUrl(resource)))

  // Derenders HTML, returning rendered resource URLs to their edge id form
  def derender(src: String, edgeMap: Map[String, String]): (List[String], String) =
    val warnings   = mutable.ListBuffer.empty[String]
    val derendered = LoEdgeIdUrl.FullyQualifiedServeRE.replaceAllIn(
      src,
      m =>
        edgeMap.getOrElse(
          m.matched, {
            warnings.append(s"Invalid internal URL ${m.matched}")
            BrokenImage
          }
        )
    )
    (warnings.toList, derendered)
  end derender

  // Construct a map from rendered URLs to the original edge-id URL
  def derenderMap(asset: Asset[?], graph: TraversedGraph)(implicit
    currentUrlService: CurrentUrlService
  ): Map[String, String] =
    graph
      .outEdgesInGroup(asset, Resources)
      .groupMapUniq(resourceUrl)(e => LoEdgeIdUrl(e.edgeId))

  def resourceUrl(resource: AssetEdge[?, ?])(implicit currentUrlService: CurrentUrlService): String =
    currentUrlService.getUrl(LoEdgeIdUrl.serveUrl(Some(resource.target)))
end CrappyRenderer

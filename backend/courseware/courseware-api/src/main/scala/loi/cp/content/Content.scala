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

package loi.cp.content

import loi.authoring.asset.Asset
import loi.authoring.edge.AssetEdge
import loi.cp.customisation.{ContentOverlay, Customisation}
import loi.cp.reference.EdgePath
import scalaz.NonEmptyList
import scalaz.std.list.*
import scalaz.syntax.foldable.*
import scalaz.syntax.nel.*

import java.util.UUID

/** A usage of an asset with customisation. "Usage" means that reused assets have one "content" per usage. To
  * distinguish two content instances for the same asset, use `edgePath`. The asset for this content is actually
  * represented by the edge that arrives at the asset (targets the asset).
  *
  * This is a fatter version of `CourseContent` because it contains the entire edge to the asset instead of just the
  * edge's name, target, and data. The only reason this does not replace `CourseContent` is because 1) `CourseContent`
  * is thin enough to go in the CourseContents cache. We tried putting this one in it and performance was noticeably
  * worse and 2) `CourseContent` can hold the root course asset but `Content` cannot, because there is no edge to the
  * root course asset (Kangland doesn't recognize the program-course edge).
  *
  * @param ancestorEdgeNames
  *   the names of edges from the root to this content, but excluding `edge.name`.
  * @param edge
  *   the asset used for this content, represented by the edge that arrives at the asset usage.
  * @param overlay
  *   customisation of this asset usage
  * @param category
  *   gradebook category
  * @param survey
  *   associated survey edge and asset name
  */
case class Content(
  ancestorEdgeNames: List[UUID],
  edge: AssetEdge.Any,
  overlay: ContentOverlay = ContentOverlay.empty,
  accessRight: Option[String] = None,
  category: Option[UUID] = None,
  survey: Option[(UUID, UUID)] = None,
  hyperlinks: Map[UUID, UUID] = Map.empty,
  testsOut: Map[UUID, Double] = Map.empty,
):

  /** the asset for this content */
  lazy val asset: Asset[?] = edge.target

  /** the uncompressed edge path */
  lazy val edgeNamesNel: NonEmptyList[UUID] = ancestorEdgeNames.toIList <::: edge.name.wrapNel

  /** the uncompressed edge path */
  lazy val edgeNames: List[UUID] = edgeNamesNel.toList

  /** the ID for this usage of `asset` */
  lazy val edgePath: EdgePath = EdgePath(edgeNames)

  /** a thinner version of this content, suitable for use in the contents cache */
  lazy val cacheableContent: CourseContent =
    CourseContent(edgeNames, asset, edge.data, overlay, accessRight, category, survey, hyperlinks, testsOut)
end Content

object Content:

  /** a convenient constructor that plucks the overlay out of the customisation document */
  def apply(ancestorEdgeNames: List[UUID], edge: AssetEdge.Any, customisation: Customisation): Content =
    Content(ancestorEdgeNames, edge, customisation(EdgePath(ancestorEdgeNames :+ edge.name)))

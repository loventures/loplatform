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

package loi.authoring.index.web

import loi.asset.util.Assex.*
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.index.{SearchPath, SearchPathElement}
import loi.authoring.node.AssetNodeService
import loi.authoring.workspace.{AttachedReadWorkspace, ReadWorkspace}
import scalaz.NonEmptyList
import scalaz.std.list.*
import scalaz.syntax.cobind.*
import scalaz.syntax.nel.*
import scaloi.syntax.zero.*

import java.util.UUID
import scala.annotation.tailrec
import scala.util.Success

object DcmPathUtils:

  /** Find the shortest path from one node (e.g. a course) to another (e.g. an activity). */
  def shortestPath(workspace: ReadWorkspace, from: UUID, to: UUID, limit: Int = 6): List[UUID] =
    // Breadth-first search upwards from `to` until we reach `from` or our limit
    @tailrec def findPath(paths: List[NonEmptyList[UUID]]): List[UUID] =
      paths match
        case path :: rest =>
          if path.head == from then path.list.toList
          else if path.size >= limit then findPath(rest)
          else
            // This just looks for any type of path, it could consider the edge group
            val incoming = workspace.inEdgeAttrs(path.head).map(_.srcName)
            findPath(rest ::: incoming.map(_ <:: path).toList)
        case Nil          => Nil

    findPath(List(to.wrapNel))
  end shortestPath

  /** Generate a URL path to an asset. */
  def dcmPath(workspace: AttachedReadWorkspace, asset: Asset[?], context: List[UUID]): String =
    s"/Authoring/branch/${workspace.bronchId}/launch/${asset.info.name}"

  def searchPath(workspace: AttachedReadWorkspace, hit: UUID)(implicit
    assetNodeService: AssetNodeService
  ): SearchPath =
    val path = shortestPath(workspace, workspace.homeName, hit) ||| List(hit)
    // cojoin v /ˈkoʊˌhɔɪn/ http://ipa-reader.xyz/?text=%CB%88ko%CA%8A%CB%8Ch%C9%94%C9%AAn
    // Given the path from the course to a hit, create a search path element for each asset
    // with its context path up to the course: [C,M,L,A] -> [ [A,L,M,C], [L,M,C], [M,C], [C] ]
    SearchPath(path.reverse.cojoin collect { case uuid :: context =>
      assetNodeService.load(workspace).byName(uuid) match
        case Success(asset) =>
          SearchPathElement(
            name = asset.info.name,
            typeId = asset.info.typeId,
            title = asset.title,
            href = dcmPath(workspace, asset, context),
          )
        case _              =>
          SearchPathElement(
            name = uuid,
            typeId = AssetTypeId.Unknown("unknown.1"),
            title = Some("Asset failed to load"),
            href = ""
          )
    })
  end searchPath
end DcmPathUtils

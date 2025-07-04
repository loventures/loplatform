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

package loi.authoring.render

import loi.authoring.asset.Asset

import java.util.UUID
import scala.util.Try

object LoEdgeIdUrl:
  private val pattern = "\"loEdgeId://([0-9a-fA-F-]{36})\"".r

  def apply(edgeId: UUID): String = s"loEdgeId://$edgeId"

  def parseAll(str: String): Set[UUID] =
    pattern
      .findAllIn(str)
      .matchData
      .flatMap(m =>
        val guidToken = m.group(1)
        Try(UUID.fromString(guidToken)).toOption
      )
      .toSet

  def replaceAll(str: String, targets: Map[UUID, Asset[?]]): String =
    pattern.replaceAllIn(
      str,
      m =>
        val guidToken = m.group(1)
        val target    = Try(UUID.fromString(guidToken)).toOption.flatMap(targets.get)
        s""""${serveUrl(target)}""""
    )

  def serveUrl(asset: Option[Asset[?]]): String =
    asset
      .map(a => s"/api/v2/authoring/nodes/${a.info.id}/serve")
      .getOrElse("")

  final val FullyQualifiedServeRE = "https://[^/]+/api/v2/authoring/nodes/[0-9]+/serve".r
end LoEdgeIdUrl

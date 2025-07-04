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

import argonaut.CodecJson
import loi.authoring.asset.factory.AssetTypeId
import scaloi.json.ArgoExtras

import java.util.UUID

final case class SearchPath(path: List[SearchPathElement]):
  def head: SearchPathElement = path.head

  def unit: Option[SearchPathElement] = find(AssetTypeId.Unit)

  def module: Option[SearchPathElement] = find(AssetTypeId.Module)

  def lesson: Option[SearchPathElement] = find(AssetTypeId.Lesson)

  def content: Option[SearchPathElement] = find(AssetTypeId.LessonElementTypes)

  def find(typeId: AssetTypeId): Option[SearchPathElement] = path.find(_.typeId == typeId)

  def find(typeIds: Set[AssetTypeId]): Option[SearchPathElement] = path.find(e => typeIds.contains(e.typeId))
end SearchPath

object SearchPath:
  implicit val codec: CodecJson[SearchPath] = CodecJson
    .derived[List[SearchPathElement]]
    .xmap(SearchPath(_))(_.path)

final case class SearchPathElement(
  name: UUID,
  typeId: AssetTypeId,
  title: Option[String],
  href: String,
)

object SearchPathElement:
  implicit val codec: CodecJson[SearchPathElement] =
    CodecJson.casecodec4(SearchPathElement.apply, ArgoExtras.unapply)(
      "name",
      "typeId",
      "title",
      "href",
    )

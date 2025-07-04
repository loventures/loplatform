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

import enumeratum.EnumEntry.Uppercase
import enumeratum.{Enum, EnumEntry}
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.exchange.exprt.CourseStructureExportService

/** The prefixes that are used in H1 tags in HTML export/import. */
private[html] sealed class H1Prefix(val typeId: AssetTypeId, val shortForm: String) extends EnumEntry with Uppercase

private[html] object H1Prefix extends Enum[H1Prefix]:
  override def values: IndexedSeq[H1Prefix] = findValues

  def forTypeId(typeId: AssetTypeId): H1Prefix = values.find(_.typeId == typeId).getOrElse(TypeIdPrefix(typeId))

  case object Unit       extends H1Prefix(AssetTypeId.Unit, "U")
  case object Module     extends H1Prefix(AssetTypeId.Module, "M")
  case object Lesson     extends H1Prefix(AssetTypeId.Lesson, "L")
  case object Page       extends H1Prefix(AssetTypeId.Html, "P")
  case object ModulePage extends H1Prefix(AssetTypeId.Html, "MP")
  case object Discussion extends H1Prefix(AssetTypeId.Discussion, "DB")
  case object EndLesson  extends H1Prefix(AssetTypeId.Lesson, "/L") // not quite a lesson asset

  final case class TypeIdPrefix(override val typeId: AssetTypeId)
      extends H1Prefix(typeId, CourseStructureExportService.assetTypeNameMap(typeId))
end H1Prefix

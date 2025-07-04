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

package loi.authoring.asset.factory

import scala.compiletime.constValueTuple
import scala.deriving.Mirror

/** Indicates the special asset properties that a asset data class has. A data class can pick and choose which of these
  * properties it defines. Special properties have special behavior in various areas of the asset framework. For
  * example, asset search only searches `title`, `subtitle`, and `keywords`, so there is value in an asset's data class
  * defining those properties. Also, all special properties are stored specially, in their own column.
  *
  * @param title
  *   true if the data type has a `title` property.
  * @param subtitle
  *   true if the data type has a `subtitle` property.
  * @param desc
  *   true if the data type has a `description` property
  * @param keywords
  *   true if the data type has a `keywords` property
  * @param archived
  *   true if the data type has an 'archived' property
  * @param attachmentId
  *   true if the data type has an 'attachmentId' property
  */

case class SpecialPropsConfig[A] private (
  title: Boolean,
  subtitle: Boolean,
  desc: Boolean,
  keywords: Boolean,
  archived: Boolean,
  attachmentId: Boolean
)

object SpecialPropsConfig:
  def apply[A <: Product](using SPC: SpecialPropsConfig[A]): SpecialPropsConfig[A] = SPC

  inline given [A <: Product](using m: Mirror.ProductOf[A]): SpecialPropsConfig[A] =
    val labels       = constValueTuple[m.MirroredElemLabels].toList
    val title        = labels.contains("title")
    val subtitle     = labels.contains("subtitle")
    val desc         = labels.contains("description")
    val keywords     = labels.contains("keywords")
    val archived     = labels.contains("archived")
    val attachmentId = labels.contains("attachmentId")

    SpecialPropsConfig(title, subtitle, desc, keywords, archived, attachmentId)
  end given

  val names: Seq[String] = Seq("title", "subtitle", "description", "keywords", "archived", "attachmentId")
end SpecialPropsConfig

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

package loi.asset.competency.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import loi.asset.license.License
import loi.authoring.asset.Asset
import scaloi.json.ArgoExtras

import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
case class Competency(
  id: Long,
  guid: UUID,
  level: Int,
  title: String,
  description: String,
  keywords: String,
  archived: Boolean,
  license: Option[License]
)

object Competency:

  def level(ca: Asset[?]): Int =
    fromAsset(ca) match
      case Some(c) => c.level
      case _       => throw new RuntimeException(s"Unexpected non-competency asset type")

  def unapply(asset: Asset[?]): Option[(Long, UUID, Int, String, String, String, Boolean, Option[License])] =
    fromAsset(asset).flatMap(ArgoExtras.unapply)

  /** If `a` is a competency asset, return a [[Competency]] representing it, otherwise return [[scala.None None]].
    */
  def fromAsset(a: Asset[?]): Option[Competency] =
    a match
      case Level1Competency.Asset(l1) =>
        val d = l1.data
        Some(Competency(l1.info.id, l1.info.name, 1, d.title, d.description, d.keywords, d.archived, d.license))
      case Level2Competency.Asset(l2) =>
        val d = l2.data
        Some(Competency(l2.info.id, l2.info.name, 2, d.title, d.description, d.keywords, d.archived, d.license))
      case Level3Competency.Asset(l3) =>
        val d = l3.data
        Some(Competency(l3.info.id, l3.info.name, 3, d.title, d.description, d.keywords, d.archived, d.license))
      case _                          => None
end Competency

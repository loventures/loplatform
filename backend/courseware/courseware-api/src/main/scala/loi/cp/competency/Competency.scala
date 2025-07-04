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

package loi.cp.competency

import argonaut.Argonaut.*
import argonaut.*
import loi.asset.competency.model.{Level1Competency, Level2Competency, Level3Competency}
import loi.asset.util.Assex.*
import loi.authoring.asset.Asset
import scaloi.json.ArgoExtras

import java.util.UUID
import scala.PartialFunction.condOpt

sealed case class Competency(id: Long, nodeName: UUID, title: String, level: Int)

object Competency:
  // constructor is not public in an effort to ensure that id refers to a node with given name
  // constructor is package-private for tests
  private[competency] def apply(id: Long, name: UUID, title: String, level: Int): Competency =
    new Competency(id, name, title, level) {}

  def unapply(asset: Asset[?]): Option[Competency] = fromAsset(asset)

  def fromAsset(asset: Asset[?]): Option[Competency] = condOpt(asset) {
    case Level1Competency.Asset(l1) => Competency(l1.info.id, l1.info.name, l1.title.get, 1)
    case Level2Competency.Asset(l2) => Competency(l2.info.id, l2.info.name, l2.title.get, 2)
    case Level3Competency.Asset(l3) => Competency(l3.info.id, l3.info.name, l3.title.get, 3)
  }

  def fromAssets(assets: Seq[Asset[?]]): Seq[Competency] =
    assets.flatMap(fromAsset)

  implicit def competencyCodec: CodecJson[Competency] =
    casecodec4(Competency.apply, ArgoExtras.unapply)("id", "nodeName", "title", "level")
end Competency

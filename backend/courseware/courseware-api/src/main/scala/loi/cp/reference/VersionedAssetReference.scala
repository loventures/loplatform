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

package loi.cp.reference

import argonaut.Argonaut.*
import argonaut.*
import loi.authoring.asset.Asset

import java.util.UUID

/** A reference to a particular version of an asset.
  *
  * @param nodeName
  *   the name of the node
  * @param commit
  *   the id of the graph the asset is in
  */
case class VersionedAssetReference(nodeName: UUID, commit: Long):
  override def toString: String = s"$commit:$nodeName"

object VersionedAssetReference:

  def apply(asset: Asset[?], commitId: Long): VersionedAssetReference =
    VersionedAssetReference(asset.info.name, commitId)

  def apply(assets: Seq[Asset[?]], commitId: Long): Seq[VersionedAssetReference] =
    assets.map(VersionedAssetReference(_, commitId))

  def apply(assetsByCommit: Map[Long, Seq[Asset[?]]]): Seq[VersionedAssetReference] =
    assetsByCommit
      .flatMap({ case (commit, assets) =>
        VersionedAssetReference(assets, commit)
      })
      .toSeq

  def of(assets: Seq[UUID], commitId: Long): Seq[VersionedAssetReference] =
    assets.map(VersionedAssetReference(_, commitId))

  implicit def varDecoder: DecodeJson[VersionedAssetReference] = DecodeJson { cursor =>
    cursor.as[String] flatMap { strVal =>
      strVal.split(":") match
        case Array(possibleCommit, possibleNode) =>
          DecodeResult.ok(VersionedAssetReference(UUID.fromString(possibleNode), possibleCommit.toLong))
        case _                                   => DecodeResult.fail("Invalid reference format", cursor.history)
    }
  }

  implicit def varEncode: EncodeJson[VersionedAssetReference] = EncodeJson { reference =>
    Json.jString(reference.toString)
  }
end VersionedAssetReference

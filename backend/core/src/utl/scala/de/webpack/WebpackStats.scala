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

package de.webpack

import cats.effect.IO
import de.webpack.WebpackStats.{AssetName, WebpackAsset, WebpackModule}
import io.circe.*
import io.circe.generic.semiauto.*
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

case class WebpackStats(
  errors: List[String],
  warnings: List[String],
  version: String,
  hash: String,
  assetsByChunkName: Map[String, AssetName], // Json, // Map[String, String Either List[String]],
  assets: List[WebpackAsset],
  modules: List[WebpackModule]
)

object WebpackStats:

  def empty: WebpackStats = WebpackStats(List(), List(), "", "", Map(), List(), List())

  case class AssetName(identifier: String Either List[String])

  def eitherDecode[A: Decoder, B: Decoder]: Decoder[A Either B] = Decoder(using
    r =>
      r.as[A] match
        case Right(a) => Right(Left(a))
        case Left(_)  => r.as[B].map(b => Right(b))
  )

  implicit def assetNameDecode: Decoder[AssetName] =
    eitherDecode[String, List[String]].map(AssetName.apply)

  case class Chunk(c: BigInt Either String)
  implicit def chunksDecode: Decoder[Chunk] =
    eitherDecode[BigInt, String].map(Chunk.apply)

  case class WebpackAsset(
    name: String,
    size: BigInt,
    chunks: List[Chunk], // BigInt Either String
    chunkNames: List[String],
    emitted: Boolean
  )

  implicit val webpackAssetCodec: Decoder[WebpackAsset] = deriveDecoder

  case class WebpackModule(
    chunks: List[Chunk],
    id: Chunk,
    name: String
  )

  implicit val webpackModuleDecode: Decoder[WebpackModule] = deriveDecoder

//  CodecJson.casecodec5(WebpackAsset.apply, WebpackAsset.unapply)(
//    "name", "size", "chunks", "chunkNames", "emitted"
//  )

  implicit val codec: Decoder[WebpackStats] = deriveDecoder

//    CodecJson.casecodec6(WebpackStats.apply, WebpackStats.unapply)(
//    "errors", "warnings", "version", "hash", "assetsByChunkName", "assets"
//  )

  implicit val decoder: EntityDecoder[IO, WebpackStats] = jsonOf[IO, WebpackStats]
end WebpackStats

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

package loi.cp.lti

import argonaut.Argonaut.*
import argonaut.*
import loi.cp.bus.{FailureInformation, Request, Response}
import scaloi.json.{ArgoExtras, Derivation}

sealed trait LtiItemSyncError

case class InternalError(errorMessage: String) extends LtiItemSyncError
object InternalError:
  val typeField                                = "InternalError"
  implicit val codec: CodecJson[InternalError] =
    CodecJson.casecodec1(InternalError.apply, ArgoExtras.unapply1)("errorMessage")

case class HttpError(req: Request, resp: Response Either String) extends LtiItemSyncError
object HttpError:
  val typeField                              = "HttpError"
  def from(f: FailureInformation): HttpError = HttpError(f.req, f.resp.map(_.getMessage))

  implicit val codec: CodecJson[HttpError] = CodecJson.casecodec2(HttpError.apply, ArgoExtras.unapply)("req", "resp")

object LtiItemSyncError:

  implicit val decode: DecodeJson[LtiItemSyncError] = Derivation.sumDecode[LtiItemSyncError]("type") {
    case InternalError.typeField => InternalError.codec.Decoder
    case HttpError.typeField     => HttpError.codec.Decoder
  }

  implicit val encode: EncodeJson[LtiItemSyncError] = Derivation.sumEncode[LtiItemSyncError]("type") {
    case e: InternalError => (InternalError.typeField, InternalError.codec.Encoder.encode(e))
    case e: HttpError     => (HttpError.typeField, HttpError.codec.Encoder.encode(e))
  }

  implicit val codec: CodecJson[LtiItemSyncError] = CodecJson.apply(encode.encode, decode.decode)
end LtiItemSyncError

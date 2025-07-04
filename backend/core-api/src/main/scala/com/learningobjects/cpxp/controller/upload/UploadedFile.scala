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

package com.learningobjects.cpxp.controller.upload

import argonaut.*
import scaloi.json.ArgoExtras.*

/** A purely-deserializable representation of an [[UploadInfo]]. */
sealed abstract class UploadedFile extends Product with Serializable:
  val guid: String

object UploadedFile:

  final case class Guid(guid: String)                       extends UploadedFile
  final case class Image(guid: String, sizes: List[String]) extends UploadedFile

  implicit val codec: CodecJson[UploadedFile] =
    import Argonaut.*
    val imgEnc                                          = EncodeJson.derive[Image]
    val imgDec                                          = DecodeJson.derive[Image]
    def encode(uf: UploadedFile): Json                  = uf match
      case Guid(guid)      => jString(guid)
      case i @ Image(_, _) => imgEnc.encode(i)
    def decode(hc: HCursor): DecodeResult[UploadedFile] =
      val asGuid  = hc.as[String].map(Guid.apply)
      val asImage = hc.as(using imgDec)
      asGuid.widen ||| asImage.widen
    CodecJson(encode, decode)
  end codec
end UploadedFile

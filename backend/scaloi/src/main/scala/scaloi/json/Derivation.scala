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

package scaloi
package json

import argonaut.*
import argonaut.Argonaut.*

/** Slight boilerplate reduction for encoding/decoding sum types. */
object Derivation:

  /** Create an encoder for `Sum` which dispatches on `typeField`.
    *
    * @param typeField
    *   the string name of the field containing type information
    * @param doEncode
    *   a function from a `Sum` to the type field value and json. Usually this will be written with partial-function
    *   syntax
    * @return
    *   an encoder for `Sum`.
    */
  def sumEncode[Sum](typeField: String)(
    doEncode: Sum => (String, Json)
  ): EncodeJson[Sum] = EncodeJson { sum =>
    val (tpe, json) = doEncode(sum)
    json.withObject((typeField := tpe) +: _)
  }

  /** Create a decoder for `Sum` which dispatches on `typeField`.
    *
    * @param typeField
    *   the string name of the field containing type information
    * @param doDecode
    *   a (partial) function from the value of `typeField` to a decoder.
    */
  def sumDecode[Sum](typeField: String)(
    doDecode: PartialFunction[String, DecodeJson[? <: Sum]]
  ): DecodeJson[Sum] = DecodeJson { hc =>
    import ArgoExtras.*
    (hc --\ typeField).as[String].flatMap { tpe =>
      doDecode.lift(tpe) match
        case Some(decoder) => decoder.decode(hc).widen
        case None          =>
          DecodeResult.fail(s"Unknown $typeField value $tpe", hc.history)
    }
  }
end Derivation

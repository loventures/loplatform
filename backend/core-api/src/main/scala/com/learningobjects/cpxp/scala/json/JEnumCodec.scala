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

package com.learningobjects.cpxp.scala.json

import argonaut.*, Argonaut.*
import scaloi.syntax.ClassTagOps.*

trait JEnumCodec:

  implicit final def jenumCodec[E <: Enum[E]: reflect.ClassTag]: CodecJson[E] =
    def valueOf(hc: HCursor): DecodeResult[E] =
      hc.cursor.focus.string match
        case Some(str) =>
          try DecodeResult.ok(Enum.valueOf(classTagClass[E], str))
          catch
            case iae: IllegalArgumentException =>
              DecodeResult.fail(iae.getMessage, hc.history)
        case None      =>
          DecodeResult.fail(s"Expected a string, got ${hc.cursor.focus}", hc.history)
    CodecJson(_.toString.asJson, valueOf)
  end jenumCodec

  implicit final def jenumKeyEncode[E <: Enum[E]]: EncodeJsonKey[E] =
    EncodeJsonKey.from[E](_.name)
end JEnumCodec

object JEnumCodec extends JEnumCodec

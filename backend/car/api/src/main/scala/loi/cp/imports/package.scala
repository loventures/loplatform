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

package loi.cp

import argonaut.*
import argonaut.Argonaut.*
import com.learningobjects.cpxp.scala.json.JEnumCodec

import java.time.Instant
import java.util.UUID

package object imports extends JEnumCodec:

  type ViolationNel[T] = scalaz.ValidationNel[errors.Violation, T]

  /* === Codecs for things that make JSONing these work */

  private[imports] implicit val instantCodec: CodecJson[Instant] =
    scaloi.json.ArgoExtras.instantCodec

  private[imports] final val uuidComponentLengths          = Seq(8, 4, 4, 4, 12)
  private[imports] implicit val uuidCodec: CodecJson[UUID] =
    def valueOf(hc: HCursor): DecodeResult[UUID] =
      hc.cursor.focus.string match
        case Some(str) =>
          val componentsLengths = str split '-' map (_.length)
          if componentsLengths sameElements uuidComponentLengths then
            try DecodeResult.ok(UUID `fromString` str)
            catch
              case iae: IllegalArgumentException =>
                DecodeResult.fail(iae.getMessage, hc.history)
          else DecodeResult.fail(s"Invalid UUID string $str", hc.history)
        case None      =>
          DecodeResult.fail(s"Expected a UUID string, got ${hc.cursor.focus}", hc.history)
    CodecJson(_.toString.asJson, valueOf)
  end uuidCodec
end imports

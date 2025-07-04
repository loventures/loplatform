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

package loi.cp.path

import argonaut.*, Argonaut.*
import scalaz.{Ordering as _, *}

object `package`:

  implicit val `CodecJson[Path]`: CodecJson[Path] =
    def encodePath(path: Path): Json                = jString(path.toString)
    def decodePath(hc: HCursor): DecodeResult[Path] =
      hc.jdecode[String].flatMap { s =>
        try DecodeResult.ok(new Path(s))
        catch
          case iae: IllegalArgumentException =>
            DecodeResult.fail(iae.getMessage, hc.history)
      }
    CodecJson(encodePath, decodePath)
  end `CodecJson[Path]`

  implicit val `Show[Path]`: Show[Path] = Show.showFromToString

  implicit val `Ordering[Path]`: Ordering[Path] =
    Ordering.comparatorToOrdering(using PathComparators.LEXICOMPARATOR)
end `package`

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

package com.learningobjects.cpxp.util

import jakarta.servlet.http.HttpServletResponse
import scalaz.ImmutableArray

import scala.io.{Codec, Source}
import scala.util.Random

object AugurySupport:
  final val `X-Fortune` = "X-Fortune"
  final val forecast    = 5

  private final val bones   = ImmutableArray.fromArray {
    implicit val `the one true codec` = Codec.UTF8
    Source
      .fromInputStream(getClass.getResourceAsStream("fortunes.txt"))
      .getLines()
      .drop(1)
      .toArray
  }
  private final val entropy = new Random

  def augurize(rsp: HttpServletResponse): Unit =
    if entropy.nextInt(forecast) == 0 then bones.runWith(rsp.setHeader(`X-Fortune`, _))(entropy nextInt bones.length)
end AugurySupport

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

import java.net.URLEncoder

trait TextSanitizerUtils:

  /** Some nasty browsers have a bad habit of sending up %uXXXX escaped unicode characters instead of %-encoding their
    * UTF-8 bytes. This causes problems with less nasty browsers that aren't expecting that style of escape, so we find
    * and replace them with their proper UTF-8 encoding here.
    *
    * @param uri
    *   A %-encoded string, possibly containing unicode code points as %uXXXX
    * @return
    *   A %-encoded string, with all such code points are encoded as bytes in UTF-8
    */
  def normalizeUnicodeURI(uri: String): String =
    TextSanitizerUtils.nonstandardUnicodeEscape.replaceAllIn(
      uri,
      { code =>
        URLEncoder.encode(Integer.parseInt(code.group(1), 16).toChar.toString, "UTF-8")
      }
    )
end TextSanitizerUtils

object TextSanitizerUtils extends TextSanitizerUtils:
  private val nonstandardUnicodeEscape = "%u([0-9A-Fa-f]{4})".r

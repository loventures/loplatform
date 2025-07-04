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

package loi.cp.web.converter

import argonaut.*, Json.*

/** Support for bowdlerizing json to mask passwords and keep things short. */
object Bowdlerize:

  /** Returns a copy of a JSON structure with all long strings and arrays truncated, and all fields that look like a
    * password elided.
    */
  def apply(json: Json): Json =
    json.fold(jNull, jBool, jNumber, mapString, mapArray, mapObject)

  private[converter] final val MaxString = 1024

  private def mapString(s: String): Json =
    jString(if s.length > MaxString then s.take(MaxString - 1).concat(truncation(s.length)) else s)

  private[converter] final val MaxArray = 256

  private def mapArray(a: List[Json]): Json =
    jArray(
      if a.lengthCompare(MaxArray) > 0 then a.take(MaxArray - 1).map(apply) :+ jString(truncation(a.length))
      else a.map(apply)
    )

  /** Truncation marker for a given size. Appended to long strings and arrays. */
  private[converter] def truncation(size: Int): String = s"…[$size]"

  private def mapObject(o: JsonObject): Json =
    jObject(o.toList.foldLeft(JsonObject.empty) { case (map, (field, value)) =>
      map :+ (field -> mapField(field, value))
    })

  private[converter] final val Password = "<?>"

  private def mapField(field: JsonField, value: Json): Json =
    if smellsLikeAPassword(field, value) then jString(Password) else apply(value)

  private def smellsLikeAPassword(field: JsonField, value: Json): Boolean =
    value.isString && field.toLowerCase.contains("password")
end Bowdlerize

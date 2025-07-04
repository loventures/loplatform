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

package com.learningobjects.cpxp.scala.util

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer}
import enumeratum.{Enum, EnumEntry}

/** Base class for explicit enumeratum enumeration deserializers.
  *
  * Usage:
  * {{{
  * @JsonSerialize(`using` = classOf[ToStringSerializer])
  * @JsonDeserialize(`using` = classOf[GreetingDeserializer])
  * sealed trait Greeting extends EnumEntry
  *
  * object Greeting extends Enum[Greeting] {
  *   ...
  * }
  *
  * class GreetingDeserializer extends EnumDeserializer[Greeting](Greeting)
  * }}}
  *
  * @param enum
  *   the enum companion object
  * @tparam A
  *   the enum type
  */
abstract class EnumDeserializer[A <: EnumEntry](enumeratum: Enum[A]) extends JsonDeserializer[A]:
  import com.learningobjects.cpxp.scala.json.EnumeratumDeserializer.*

  override def deserialize(jp: JsonParser, ctx: DeserializationContext): A =
    jp.parseEnum(using enumeratum)

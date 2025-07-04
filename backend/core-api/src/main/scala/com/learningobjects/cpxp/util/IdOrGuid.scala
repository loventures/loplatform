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

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.learningobjects.cpxp.component.annotation.StringConvert
import com.learningobjects.cpxp.component.web.converter.StringConverter
import com.learningobjects.cpxp.util.IdOrGuid.{Guid, Id}

import scaloi.syntax.StringOps.*

/** For handling query parameters that can be db IDs or GUIDs. Somewhat duplicative of ApiIdentifier functionality, but
  * is a bit more scala-friendly.
  */
@JsonSerialize(`using` = classOf[IdOrGuidSerializer])
@JsonDeserialize(`using` = classOf[IdOrGuidDeserializer])
@StringConvert(`using` = classOf[IdOrGuidStringConverter])
sealed trait IdOrGuid

object IdOrGuid:

  case class Id(id: Long) extends IdOrGuid

  case class Guid(guid: String) extends IdOrGuid

  def apply(id: Long): IdOrGuid = Id(id)

  def apply(guid: String): IdOrGuid = Guid(guid)

private final class IdOrGuidDeserializer extends JsonDeserializer[IdOrGuid]:
  override def deserialize(
    p: JsonParser,
    ctxt: DeserializationContext
  ) = p.readValueAsTree[JsonNode] match
    case number if number.isIntegralNumber && number.longValue > 0 =>
      Id(number.longValue)
    case guid if guid.isTextual                                    =>
      Guid(guid.asText)
    case _                                                         => null
end IdOrGuidDeserializer

private final class IdOrGuidSerializer extends JsonSerializer[IdOrGuid]:
  override def serialize(
    value: IdOrGuid,
    gen: JsonGenerator,
    serializers: SerializerProvider
  ): Unit = value match
    case Id(id)     => gen.writeNumber(id)
    case Guid(guid) => gen.writeString(guid)

private class IdOrGuidStringConverter extends StringConverter[IdOrGuid]:
  override def apply(string: StringConverter.Raw[IdOrGuid]) = Some {
    string.value.toLong_?.map(Id.apply).getOrElse(Guid(string.value))
  }

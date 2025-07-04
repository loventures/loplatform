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

package com.learningobjects.cpxp.jackson

import org.apache.pekko.actor.ActorPath
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}

class ActorPathSerializer extends JsonSerializer[ActorPath]:
  override def serialize(value: ActorPath, jgen: JsonGenerator, provider: SerializerProvider): Unit =
    jgen.writeString(value.toSerializationFormat)

class ActorPathDeserializer extends JsonDeserializer[ActorPath]:
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): ActorPath =
    val address = jp.getCodec.readValue(jp, classOf[String])
    ActorPath.fromString(address)

class PekkoModule extends SimpleModule:
  addSerializer(classOf[ActorPath], new ActorPathSerializer)
  addDeserializer(classOf[ActorPath], new ActorPathDeserializer)

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

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.scala.JacksonModule

import scala.util.{Success, Try}

/** Jackson module to allow serialization and deserialization of Try values. Serialization will throw if the value is a
  * Failure. Deserialization returns a Success.
  */
trait TryModule extends TrySerializerModule with TryDeserializerModule

// serialization

trait TrySerializerModule extends JacksonModule:
  this += { ctx =>
    ctx `addSerializers` TrySerializerResolver
  }

object TrySerializerResolver extends Serializers.Base:
  override def findSerializer(config: SerializationConfig, jt: JavaType, beanDesc: BeanDescription): JsonSerializer[?] =
    if classOf[Try[?]].isAssignableFrom(jt.getRawClass) then TrySerializer
    else null

object TrySerializer extends StdSerializer[Try[?]](classOf[Try[?]]):
  override def serialize(tri: Try[?], jg: JsonGenerator, sp: SerializerProvider): Unit =
    jg `writeObject` tri.get

// deserialization

trait TryDeserializerModule extends JacksonModule:
  this += { ctx =>
    ctx `addDeserializers` TryDeserializerResolver
  }

object TryDeserializerResolver extends Deserializers.Base:
  override def findBeanDeserializer(
    jt: JavaType,
    config: DeserializationConfig,
    beanDesc: BeanDescription
  ): JsonDeserializer[?] =
    if classOf[Try[?]].isAssignableFrom(jt.getRawClass) then new TryDeserializer(jt)
    else null

class TryDeserializer(jt: JavaType) extends StdDeserializer[Try[?]](classOf[Try[?]]):
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): Try[?] =
    val contained =
      ctxt.findRootValueDeserializer(jt.containedType(0)).deserialize(jp, ctxt)
    Success(contained)

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

import argonaut.*
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser, JsonProcessingException, TreeNode}
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.{ArrayNode, ObjectNode, ValueNode}
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.scala.JacksonModule

import scala.jdk.CollectionConverters.*

trait ArgonautModule extends ArgonautSerializerModule with ArgonautDeserializerModule

// serialization

trait ArgonautSerializerModule extends JacksonModule:
  this += { ctx =>
    ctx `addSerializers` ArgonautSerializerResolver
  }

object ArgonautSerializerResolver extends Serializers.Base:
  override def findSerializer(config: SerializationConfig, jt: JavaType, beanDesc: BeanDescription): JsonSerializer[?] =
    if classOf[Json].isAssignableFrom(jt.getRawClass) then ArgonautSerializer else null

object ArgonautSerializer extends StdSerializer[Json](classOf[Json]):
  override def serialize(value: Json, jg: JsonGenerator, sp: SerializerProvider): Unit =
    def write(json: Json): Unit = json.fold(
      jg.writeNull(),
      b => jg.writeBoolean(b),
      n => jg.writeNumber(n.toBigDecimal.bigDecimal),
      s => jg.writeString(s),
      a =>
        jg.writeStartArray()
        a foreach write
        jg.writeEndArray()
      ,
      o =>
        jg.writeStartObject()
        o.toList foreach { case (k, v) =>
          jg.writeFieldName(k); write(v)
        }
        jg.writeEndObject()
    )
    write(value)
  end serialize
end ArgonautSerializer

// deserialization

trait ArgonautDeserializerModule extends JacksonModule:
  this += { ctx =>
    ctx `addDeserializers` ArgonautDeserializerResolver
  }

object ArgonautDeserializerResolver extends Deserializers.Base:
  override def findBeanDeserializer(
    jt: JavaType,
    config: DeserializationConfig,
    beanDesc: BeanDescription
  ): JsonDeserializer[?] =
    if classOf[Json].isAssignableFrom(jt.getRawClass) then ArgonautDeserializer else null

object ArgonautDeserializer extends StdDeserializer[Json](classOf[Json]):
  private def error(value: TreeNode): Nothing =
    throw new JsonProcessingException(s"Don't know what to do with ${value.getClass.getSimpleName}: $value") {}

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): Json =
    def read(tn: TreeNode): Json = tn match
      case arr: ArrayNode   =>
        Json.array(arr.elements().asScala.map(read).toSeq*)
      case obj: ObjectNode  =>
        Json.obj(obj.properties.asScala.map(e => e.getKey -> read(e.getValue)).toSeq*)
      case value: ValueNode =>
        if value.isBoolean then Json.jBool(value.booleanValue)
        else if value.isNull then Json.jNull
        else if value.isNumber then Json.jNumber(value.toString).get
        else if value.isTextual then Json.jString(value.textValue)
        else error(value)
      case unknown          => error(unknown)
    read(ctxt.readValue(jp, classOf[JsonNode]))
  end deserialize
end ArgonautDeserializer

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

/** Jackson module to allow serialization of Function0 values. The function is evaluated and the result is serialize.
  *
  * This is radically simpler than OptionModule, so is presumably significantly lacking in features.
  */
trait Function0Module extends Function0SerializerModule with Function0DeserializerModule

// serialization

trait Function0SerializerModule extends JacksonModule:
  this += { ctx =>
    ctx `addSerializers` Function0SerializerResolver
  }

object Function0SerializerResolver extends Serializers.Base:
  override def findSerializer(config: SerializationConfig, jt: JavaType, beanDesc: BeanDescription): JsonSerializer[?] =
    if classOf[() => Any].isAssignableFrom(jt.getRawClass) then Function0Serializer
    else null

object Function0Serializer extends StdSerializer[() => Any](classOf[() => Any]):
  override def serialize(f: () => Any, jg: JsonGenerator, sp: SerializerProvider): Unit = jg `writeObject` f()

// deserialization

trait Function0DeserializerModule extends JacksonModule:
  this += { ctx =>
    ctx `addDeserializers` Function0DeserializerResolver
  }

object Function0DeserializerResolver extends Deserializers.Base:
  override def findBeanDeserializer(
    jt: JavaType,
    config: DeserializationConfig,
    beanDesc: BeanDescription
  ): JsonDeserializer[?] =
    if classOf[() => Any].isAssignableFrom(jt.getRawClass) then new Function0Deserializer(jt)
    else null

class Function0Deserializer(jt: JavaType) extends StdDeserializer[() => Any](classOf[() => Any]):
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): () => Any =
    val contained =
      ctxt.findRootValueDeserializer(jt.containedType(0)).deserialize(jp, ctxt)
    () => contained

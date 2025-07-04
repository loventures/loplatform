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

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/** Jackson module to allow serialization and deserialization of Future values. Serialization will wait until the result
  * is ready. Deserialization returns a future result that is immediately available.
  *
  * This is radically simpler than OptionModule, so is presumably significantly lacking in features.
  */
trait FutureModule extends FutureSerializerModule with FutureDeserializerModule

// serialization

trait FutureSerializerModule extends JacksonModule:
  this += { ctx =>
    ctx `addSerializers` FutureSerializerResolver
  }

object FutureSerializerResolver extends Serializers.Base:
  override def findSerializer(config: SerializationConfig, jt: JavaType, beanDesc: BeanDescription): JsonSerializer[?] =
    if classOf[Future[?]].isAssignableFrom(jt.getRawClass) then FutureSerializer
    else null

object FutureSerializer extends StdSerializer[Future[?]](classOf[Future[?]]):
  override def serialize(future: Future[?], jg: JsonGenerator, sp: SerializerProvider): Unit =
    jg `writeObject` Await.result(future, Duration.Inf)

// deserialization

trait FutureDeserializerModule extends JacksonModule:
  this += { ctx =>
    ctx `addDeserializers` FutureDeserializerResolver
  }

object FutureDeserializerResolver extends Deserializers.Base:
  override def findBeanDeserializer(
    jt: JavaType,
    config: DeserializationConfig,
    beanDesc: BeanDescription
  ): JsonDeserializer[?] =
    if classOf[Future[?]].isAssignableFrom(jt.getRawClass) then new FutureDeserializer(jt)
    else null

class FutureDeserializer(jt: JavaType) extends StdDeserializer[Future[?]](classOf[Future[?]]):
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): Future[?] =
    val contained =
      ctxt.findRootValueDeserializer(jt.containedType(0)).deserialize(jp, ctxt)
    Future.successful(contained)

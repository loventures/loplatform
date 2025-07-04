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

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.{Deserializers, ResolvableDeserializer}

/** A deserializer which handles entities with type [[OptionalField]]
  *
  * Look at OptionalFieldDeserializerTest for usage examples
  */
class OptionalFieldDeserializer(javaType: JavaType)
    extends JsonDeserializer[OptionalField[?]]
    with ResolvableDeserializer:

  var elementDeserializer: JsonDeserializer[Object] = scala.compiletime.uninitialized

  override def resolve(ctxt: DeserializationContext): Unit =
    elementDeserializer = ctxt.findRootValueDeserializer(javaType)

  override def deserialize(p: JsonParser, ctxt: DeserializationContext): OptionalField[?] =
    p.getCurrentToken match
      case null                 => Absent()
      case JsonToken.VALUE_NULL => Null()
      case _                    =>
        Present(elementDeserializer.deserialize(p, ctxt))

  override def getNullValue = Null()
end OptionalFieldDeserializer

/** A "Deserializers" wrapper for the OptionDeserializer. This is necessary since Jackson does not include containing
  * type information for parametric types, i.e. it's necessary to handle things like OptionalField[Date]
  */
class OptionalFieldDeserializerWrappers extends Deserializers.Base:

  override def findBeanDeserializer(
    javaType: JavaType,
    config: DeserializationConfig,
    beanDesc: BeanDescription
  ): JsonDeserializer[?] =
    javaType.getRawClass match
      case klass if klass == classOf[OptionalField[?]] =>
        new OptionalFieldDeserializer(javaType.containedType(0))
      case _                                           => null
end OptionalFieldDeserializerWrappers

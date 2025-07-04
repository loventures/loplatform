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

package loi.cp.reference

import argonaut.Argonaut.*
import argonaut.*
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import com.learningobjects.cpxp.component.web.converter.StringConverter

import scala.reflect.ClassTag

class InvalidConversionException(msg: String) extends RuntimeException(msg)

class ArgonautStringConverter[R: DecodeJson] extends StringConverter[R]:
  override def apply(v1: StringConverter.Raw[R]): Option[R] =
    v1.value.asJson.as[R].toOption

class ArgonautReferenceSerializer[R: EncodeJson, S: DecodeJson](implicit tt: ClassTag[S]) extends JsonSerializer[R]:
  override def serialize(value: R, gen: JsonGenerator, serializers: SerializerProvider): Unit =
    val serializer: JsonSerializer[AnyRef] = serializers.findValueSerializer(tt.runtimeClass.asInstanceOf[Class[S]])
    value.asJson.as[S].toOption match
      case Some(s) => serializer.serialize(s.asInstanceOf[Object], gen, serializers)
      case None    => throw new InvalidConversionException(s"Cannot convert ${value}")

class ArgonautReferenceDeserializer[R: DecodeJson] extends JsonDeserializer[R]:
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): R =
    p.getValueAsString.asJson
      .as[R]
      .toOption
      .getOrElse(throw new InvalidConversionException(s"Cannot convert ${p.getValueAsString()}"))

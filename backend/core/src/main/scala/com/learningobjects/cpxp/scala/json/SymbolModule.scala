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
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.scala.JacksonModule

trait SymbolModule  extends SymbolDeserializerModule with SymbolSerializerModule
object SymbolModule extends SymbolModule

trait SymbolDeserializerModule extends JacksonModule:
  this += { _.addDeserializers(SymbolDeserializers) }

private object SymbolDeserializers extends Deserializers.Base:
  override def findBeanDeserializer(tpe: JavaType, cfg: DeserializationConfig, desc: BeanDescription) =
    if classOf[Symbol].isAssignableFrom(tpe.getRawClass) then SymbolDeserializer
    else null

private object SymbolDeserializer extends StdDeserializer[Symbol](classOf[Symbol]):
  override def deserialize(p: JsonParser, ctxt: DeserializationContext) =
    Symbol(p.getValueAsString)

trait SymbolSerializerModule extends JacksonModule:
  this += { _.addSerializers(SymbolSerializers) }

private object SymbolSerializers extends Serializers.Base:
  override def findSerializer(cfg: SerializationConfig, tpe: JavaType, desc: BeanDescription) =
    if classOf[Symbol].isAssignableFrom(tpe.getRawClass) then SymbolSerializer
    else null

private object SymbolSerializer extends StdSerializer[Symbol](classOf[Symbol]):
  override def serialize(value: Symbol, jgen: JsonGenerator, provider: SerializerProvider) =
    jgen.writeString(value.name)

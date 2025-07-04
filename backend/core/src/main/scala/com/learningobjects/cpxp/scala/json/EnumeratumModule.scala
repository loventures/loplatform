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

import com.fasterxml.jackson.core.{JsonGenerator, JsonParseException, JsonParser, JsonToken}
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.deser.{Deserializers, KeyDeserializers}
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.scala.JacksonModule
import com.learningobjects.cpxp.scala.json.JavaTypeOps.*
import enumeratum.{Enum, EnumEntry}
import scalaz.\/
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*

import scala.util.Try

/** Jackson module to allow serialization and deserialization of enumeratum enums. Deserialization assumes the standard
  * trait / object pattern has been followed.
  *
  * This is radically simpler than OptionModule, so is presumably significantly lacking in features.
  */
trait EnumeratumModule extends EnumeratumSerializerModule with EnumeratumDeserializerModule

// serialization

trait EnumeratumSerializerModule extends JacksonModule:
  this += { ctx =>
    ctx `addSerializers` new EnumSerializers((gen, entryName) => gen.writeObject(entryName))
    ctx `addKeySerializers` new EnumSerializers((gen, entryName) => gen.writeFieldName(entryName))
  }

private class EnumSerializers(f: (JsonGenerator, String) => Unit) extends Serializers.Base:

  private lazy val ser = new StdSerializer[EnumEntry](classOf[EnumEntry]):
    override def serialize(
      enumEntry: EnumEntry,
      gen: JsonGenerator,
      provider: SerializerProvider
    ): Unit = f(gen, enumEntry.entryName)

  override def findSerializer(
    config: SerializationConfig,
    jt: JavaType,
    beanDesc: BeanDescription
  ): JsonSerializer[?] =
    if jt.isEnumeratumEnum then ser else null
end EnumSerializers

// deserialization

trait EnumeratumDeserializerModule extends JacksonModule:
  this += { ctx =>
    ctx `addDeserializers` EnumeratumDeserializerResolver
    ctx `addKeyDeserializers` EnumeratumKeyDeserializerResolver
  }

// value deserialization

object EnumeratumDeserializerResolver extends Deserializers.Base:
  override def findBeanDeserializer(
    jt: JavaType,
    config: DeserializationConfig,
    beanDesc: BeanDescription
  ): JsonDeserializer[?] = jt.toEnumeratumEnum.map(new EnumeratumDeserializer()(using _)).orNull

class EnumeratumDeserializer[A <: EnumEntry](implicit Enum: Enum[A])
    extends StdDeserializer[EnumEntry](classOf[EnumEntry]):
  import EnumeratumDeserializer.*

  override def deserialize(jp: JsonParser, ctx: DeserializationContext): A =
    jp.parseEnum

object EnumeratumDeserializer:

  implicit class JsonParserOps(val jp: JsonParser) extends AnyVal:
    def parseEnum[A <: EnumEntry: Enum]: A =
      attemptParseEnum valueOr { s =>
        throw new JsonParseException(jp, s)
      }

    def attemptParseEnum[A <: EnumEntry: Enum]: String \/ A =
      val Enum = implicitly[Enum[A]]
      for
        string <- textValue(jp) \/> "Expected a string value"
        e      <- Enum.withNameOption(string) \/> s"""Invalid enum value: $string (expected one of ${Enum.values
                      .mkString(", ")})"""
      yield e

    private def textValue(jp: JsonParser): Option[String] =
      (jp.getCurrentToken == JsonToken.VALUE_STRING).option(jp.getText)
  end JsonParserOps
end EnumeratumDeserializer

// key deserialization

object EnumeratumKeyDeserializerResolver extends KeyDeserializers:

  override def findKeyDeserializer(
    jt: JavaType,
    config: DeserializationConfig,
    beanDesc: BeanDescription
  ): KeyDeserializer = jt.toEnumeratumEnum.map(new EnumeratumKeyDeserializer()(using _)).orNull

class EnumeratumKeyDeserializer[A <: EnumEntry](implicit Enum: Enum[A]) extends KeyDeserializer:

  override def deserializeKey(key: String, ctx: DeserializationContext): A =

    Try(Enum.withName(key))
      .recover({ case ex: NoSuchElementException =>
        throw new JsonParseException(ctx.getParser, ex.getMessage, ex)
      })
      .get

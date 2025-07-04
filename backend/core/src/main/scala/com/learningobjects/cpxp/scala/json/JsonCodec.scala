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

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}

import scala.annotation.implicitNotFound
import scala.reflect.ClassTag

/** A simple json typeclass
  */
object JsonCodec extends LowPriorityCodecs:
  implicit final class JsonEncodeOps[T](val t: T) extends AnyVal:
    def encode[J](implicit encode: Encode[T] { type JsonRepr = J }) =
      encode.toJson(t)

  implicit final class JsonDecodeOps[J](val j: J) extends AnyVal:
    def decode[T](implicit decoder: Decode[T] { type JsonRepr = J }): T =
      decoder.fromJson(j)

  /** Example usage: val json = """{a: 1, b: 2}""" val (a: Int, b: Int) = decode(json) XXX: Maybe a name that isn't
    * decode?
    */
  object decode:
    def apply[T] = new Decoder[T]
    class Decoder[T]:
      def apply[J](j: J)(implicit decoder: Decode[T] { type JsonRepr = J }): T =
        decoder.fromJson(j)
end JsonCodec

trait LowPriorityCodecs:
  import JacksonCodec.*

  implicit def productStringCodec[P <: Product: ClassTag](implicit
    mapper: ObjectMapper
  ): Encode[P] & Decode[P] { type JsonRepr = String } =
    jacksonStringCodec[P]

  implicit def productNodeCodec[P <: Product: ClassTag](implicit
    mapper: ObjectMapper
  ): Encode[P] & Decode[P] { type JsonRepr = JsonNode } =
    jacksonNodeCodec[P]
end LowPriorityCodecs

//TODO: A Codec is an Isomorphism
@implicitNotFound(msg = "No JsonCodec available for ${T}")
trait JsonCodec[T] extends Encode[T] with Decode[T]

@implicitNotFound(msg = "No JsonEncode available for ${T}")
trait Encode[T]:
  type JsonRepr
  def toJson(t: T): JsonRepr
object Encode:
  type Aux[T, Repr] = Encode[T] { type JsonRepr = Repr }

@implicitNotFound(msg = "No JsonDecode available for ${T}")
trait Decode[T]:
  type JsonRepr
  def fromJson(json: JsonRepr): T
object Decode:
  type Aux[T, Repr] = Decode[T] { type JsonRepr = Repr }

/** A collection of Simple JsonCodecs using Jackson
  */
object JacksonCodec:

  trait StringEncode[T] extends Encode[T]:

    def mapper: ObjectMapper

    override type JsonRepr = String

    override def toJson(t: T): JsonRepr = mapper.writeValueAsString(t)

  trait StringDecode[T] extends Decode[T]:

    def mapper: ObjectMapper

    def classTag: ClassTag[T]

    override type JsonRepr = String

    override def fromJson(json: JsonRepr): T =
      mapper.readValue(json, classTag.runtimeClass.asInstanceOf[Class[T]])
  end StringDecode

  trait NodeEncode[T] extends Encode[T]:
    def mapper: ObjectMapper

    override type JsonRepr = JsonNode

    override def toJson(t: T): JsonRepr =
      Option(t).fold(mapper.getNodeFactory.nullNode.asInstanceOf[JsonNode])(mapper.valueToTree[JsonNode](_))

  trait NodeDecode[T] extends Decode[T]:
    def mapper: ObjectMapper

    def classTag: ClassTag[T]
    override type JsonRepr = JsonNode

    lazy val reader =
      mapper.readerFor(classTag.runtimeClass.asInstanceOf[Class[T]])

    override def fromJson(json: JsonRepr): T = reader.readValue(json)
  end NodeDecode

  def jacksonStringEncode[T](implicit objectMapper: ObjectMapper): Encode[T] { type JsonRepr = String } =
    new StringEncode[T]:
      override val mapper: ObjectMapper = objectMapper

  def jacksonStringDecode[T: ClassTag](implicit objectMapper: ObjectMapper): Decode[T] { type JsonRepr = String } =
    new StringDecode[T]:
      override val mapper: ObjectMapper  = objectMapper
      override val classTag: ClassTag[T] = implicitly[ClassTag[T]]

  def jacksonStringCodec[T: ClassTag](implicit
    objectMapper: ObjectMapper
  ): Encode[T] & Decode[T] { type JsonRepr = String } =
    new StringEncode[T] with StringDecode[T]:
      override type JsonRepr = String
      override val mapper: ObjectMapper  = objectMapper
      override val classTag: ClassTag[T] = implicitly[ClassTag[T]]

  def jacksonNodeEncode[T](implicit objectMapper: ObjectMapper): Encode[T] { type JsonRepr = JsonNode } =
    new NodeEncode[T]:
      override val mapper: ObjectMapper = objectMapper

  def jacksonNodeDecode[T: ClassTag](implicit objectMapper: ObjectMapper): Decode[T] { type JsonRepr = JsonNode } =
    new NodeDecode[T]:
      override val mapper: ObjectMapper  = objectMapper
      override val classTag: ClassTag[T] = implicitly[ClassTag[T]]

  def jacksonNodeCodec[T: ClassTag](implicit
    objectMapper: ObjectMapper
  ): Encode[T] & Decode[T] { type JsonRepr = JsonNode } =
    new NodeEncode[T] with NodeDecode[T]:
      override type JsonRepr = JsonNode
      override val mapper: ObjectMapper  = objectMapper
      override val classTag: ClassTag[T] = implicitly[ClassTag[T]]
end JacksonCodec

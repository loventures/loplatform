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

import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer
import com.fasterxml.jackson.databind.util.StdConverter
import com.fasterxml.jackson.databind.{BeanDescription, JavaType, JsonSerializer, SerializationConfig}
import com.fasterxml.jackson.module.scala.JacksonModule
import scalaz.NonEmptyList
import scalaz.syntax.foldable.*

/** Jackson module to allow serialization and deserialization of Scalaz values.
  */
trait ScalazModule extends ScalazSerializationModule

trait ScalazSerializationModule extends JacksonModule:
  this += { ctx =>
    ctx.addSerializers(ScalazSerializerResolver)
  }

object ScalazSerializerResolver extends Serializers.Base:

  private val NelSerializer = new StdDelegatingSerializer(NonEmptyListConverter)

  override def findSerializer(
    config: SerializationConfig,
    jt: JavaType,
    beanDesc: BeanDescription
  ): JsonSerializer[?] =
    if classOf[NonEmptyList[?]].isAssignableFrom(jt.getRawClass) then NelSerializer
    else null
end ScalazSerializerResolver

object NonEmptyListConverter extends StdConverter[NonEmptyList[?], List[?]]:
  override def convert(value: NonEmptyList[?]): List[?] = value.toList

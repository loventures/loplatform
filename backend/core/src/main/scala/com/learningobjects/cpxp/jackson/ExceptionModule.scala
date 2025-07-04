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

package com.learningobjects.cpxp.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{JsonSerializer, SerializerProvider}
import org.apache.commons.lang3.exception.ExceptionUtils

class ExceptionModule(verbose: Boolean) extends SimpleModule:
  addSerializer(ExceptionSerializer)

  object ExceptionSerializer extends JsonSerializer[Throwable]:
    override def handledType(): Class[Throwable] = classOf[Throwable]

    override def serialize(th: Throwable, gen: JsonGenerator, serializers: SerializerProvider): Unit =
      if verbose then serializers.defaultSerializeValue(ExceptionWrapper(th), gen)
      else serializers.defaultSerializeValue(th.getClass.getSimpleName, gen)

case class ExceptionWrapper(
  `class`: String,
  message: Option[String],
  stackTrace: Array[String],
  @JsonInclude(JsonInclude.Include.NON_EMPTY) attributes: Map[String, Any]
)

object ExceptionWrapper:
  def apply(th: Throwable): ExceptionWrapper =
    ExceptionWrapper(
      th.getClass.getName,
      Option(th.getMessage),
      ExceptionUtils.getRootCauseStackTrace(th),
      attributes(th)
    )

  private def attributes(th: Throwable): Map[String, Any] = th match
    case product: Product =>
      product.productElementNames.zip(product.productIterator).toMap
    case _                => Map.empty
end ExceptionWrapper

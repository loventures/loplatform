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

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer}
import com.google.common.reflect.TypeToken
import com.learningobjects.cpxp.component.annotation.StringConvert
import com.learningobjects.cpxp.component.web.converter.StringConverter
import loi.cp.context.{ContextId, HasContextId}
import org.apache.commons.lang3.StringUtils

/** Wrapper for database-based identifiers and context/edgepath identifiers that can be used flexibly in place when
  * needing to work with both.
  */
@StringConvert(`using` = classOf[Identifier.IdentifierStringConverter])
@JsonSerialize(`using` = classOf[ToStringSerializer])
@JsonDeserialize(`using` = classOf[Identifier.IdentifierDeserializer])
sealed trait Identifier:
  override def toString: String = this match
    case PersistenceIdentifier(dbId)                     => dbId.toString
    case ContentIdentifierWrapper(id: ContentIdentifier) => id.toString

case class PersistenceIdentifier(id: Long)                 extends Identifier
case class ContentIdentifierWrapper(id: ContentIdentifier) extends Identifier with HasContextId:
  override val contextId: ContextId = id.contextId

object Identifier:
  class IdentifierStringConverter extends StringConverter[Identifier]:
    override def apply(input: StringConverter.Raw[Identifier]): Option[Identifier] =
      if input == null || StringUtils.isEmpty(input.value) then None
      else if input.value.forall(_.isDigit) then Option(PersistenceIdentifier(input.value.toLong))
      else ContentIdentifier(input.value).map(ContentIdentifierWrapper.apply)

  val converter: IdentifierStringConverter = new IdentifierStringConverter

  class IdentifierDeserializer extends JsonDeserializer[Identifier]:
    override def deserialize(p: JsonParser, ctxt: DeserializationContext): Identifier =
      converter(StringConverter.Raw(p.getValueAsString, TypeToken.of(classOf[Identifier]))).get
end Identifier

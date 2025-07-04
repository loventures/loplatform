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

package loi.cp.message

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser, JsonToken}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}

/** Captures a user identifier either by way of their PK or their handle. Serializes as the underlying value,
  * deserializes according to whether the value is a number or string.
  *
  * Arguably this should die a death and we only identify users by handle.
  */
@JsonSerialize(`using` = classOf[UserIdentifierSerializer])
@JsonDeserialize(`using` = classOf[UserIdentifierDeserializer])
sealed trait UserIdentifier:

  /** Fold on the user's identifier.
    *
    * @param idf
    *   function to run if the user is identified by PK
    * @param handlef
    *   function to run if the user is identified by handle
    * @tparam B
    *   the result type
    * @return
    *   the result
    */
  def fold[B](idf: Long => B, handlef: String => B): B

  /** Whether the user is identified by PK. */
  def isId: Boolean

  /** Whether the user is identified by handle. */
  def isHandle: Boolean = !isId
end UserIdentifier

/** Identify a user by PK. */
case class UserId(id: Long) extends UserIdentifier:
  override def isId: Boolean                                    = true
  override def fold[B](idf: Long => B, handlef: String => B): B = idf(id)

/** Identify a user by handle. */
case class UserHandle(handle: String) extends UserIdentifier:
  override def isId: Boolean                                    = false
  override def fold[B](idf: Long => B, handlef: String => B): B = handlef(handle)

/** Serialize a user identifier. */
class UserIdentifierSerializer extends JsonSerializer[UserIdentifier]:
  override def serialize(ioh: UserIdentifier, jgen: JsonGenerator, provider: SerializerProvider): Unit =
    ioh.fold(jgen.writeNumber, jgen.writeString)

/** Deserialize a user identifier. */
class UserIdentifierDeserializer extends JsonDeserializer[UserIdentifier]:
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): UserIdentifier =
    if jp.getCurrentToken == JsonToken.VALUE_STRING then UserHandle(jp.getText) else UserId(jp.getLongValue)

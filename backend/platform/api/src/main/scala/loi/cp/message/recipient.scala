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

import java.lang.Long as JLong

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}

/** Representation of a recipient of a message. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_type")
@JsonSubTypes(
  Array(
    new Type(name = "user", value = classOf[UserRecipient]),
    new Type(name = "context", value = classOf[ContextRecipient])
  )
)
@JsonSerialize
sealed trait Recipient

// NB: duplication of the _type property as an explicit field on these is necessary
// to work around a jackson bug that List(UserRecipient(1L)) does not get serialized
// with _type attribute but UserRecipient(1L) does. Sigh.

/** Identifies a single user recipient of a message. */
case class UserRecipient(
  user: UserIdentifier
) extends Recipient:
  val _type = "user"

object UserRecipient:

  /** Construct a user recipient from a PK. */
  def apply(id: Long): UserRecipient = UserRecipient(UserId(id))

  /** Construct a user recipient from a handle. */
  def apply(handle: String): UserRecipient = UserRecipient(UserHandle(handle))

/** Identifies the recipient of a message as users in a class. */
case class ContextRecipient(
  context: Long,
  @JsonDeserialize(contentAs = classOf[JLong]) role: Option[Long]
) extends Recipient:
  val _type = "context"

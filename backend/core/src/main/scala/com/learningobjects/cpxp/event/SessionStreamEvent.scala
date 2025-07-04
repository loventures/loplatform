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

package com.learningobjects.cpxp.event

import enumeratum.{Enum, EnumEntry}

/** Session event that is published to the pekko event stream.
  */
case class SessionStreamEvent(sessionId: String, message: SessionStreamEvent.Message)

/** Session event companion.
  */
object SessionStreamEvent:

  /** Session event messages.
    */
  sealed trait Message extends EnumEntry

  /** Session event message companion.
    */
  object Message extends Enum[Message]:

    /** Enumeration values. */
    override val values = findValues

    /** A login occurred. */
    case object Login extends Message

    /** A logout occurred. */
    case object Logout extends Message

    /** The session was destroyed. This may come hot on the heels of a login or logout message. */
    case object Destroyed extends Message
  end Message

  /** Java compatibility login message. */
  val login = Message.Login

  /** Java compatibility logout message. */
  val logout = Message.Logout
end SessionStreamEvent

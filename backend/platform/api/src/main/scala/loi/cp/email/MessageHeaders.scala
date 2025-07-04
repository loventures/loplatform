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

package loi.cp.email

/** Common message headers.
  */
object MessageHeaders:

  /** Message ID. */
  final val MessageID = "Message-ID"

  /** In reply to. */
  final val InReplyTo = "In-Reply-To"

  /** Auto submitted. */
  final val AutoSubmitted = "Auto-Submitted"

  /** Auto submitted: auto reply. */
  final val AutoSubmitted_AutoReplied = "auto-replied"

  /** Auto submitted: no. */
  final val AutoSubmitted_No = "no"

  /** Precedence. */
  final val Precedence = "Precedence"

  /** X-Precedence. */
  final val X_Precedence = "X-Precedence"

  /** Precedence. */
  final val Precedence_AutoReply = "auto_reply"

  /** X-Auto respond. */
  final val X_AutoRespond = "X-AutoRespond"
end MessageHeaders

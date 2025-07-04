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

/** Representation of a message identifier.
  * @param name
  *   the name part
  * @param domain
  *   the domain part
  */
case class MessageIdentifier(name: String, domain: String):

  /** Convert this to a message id.
    * @return
    *   a message id
    */
  def toMessageId: String = s"<$name@$domain>"

object MessageIdentifier:

  /** Matching regex. This is slightly slack in order to accommodate inconsistency in in-reply-to headers. */
  private val NameAtDomain = """[^<]*<([^@]+)@([^>]+)>.*""".r

  /** Pattern match a message identifier from an in-reply-to header.
    * @param id
    *   the identifier to match
    * @return
    *   the matched message identifier parts
    */
  def unapply(id: String): Option[(String, String)] = id match
    case NameAtDomain(name, domain) => Some((name, domain))
    case _                          => None
end MessageIdentifier

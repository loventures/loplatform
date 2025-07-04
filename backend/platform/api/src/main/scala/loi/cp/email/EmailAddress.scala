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

import javax.mail.Address
import javax.mail.internet.InternetAddress

/** Representation of an email address.
  * @param username
  *   the username
  * @param domain
  *   the domain name
  */
case class EmailAddress(
  username: String,
  domain: String
):

  /** Convert this to an email address.
    * @return
    *   an email address
    */
  def toAddress: String = s"$username@$domain"
end EmailAddress

object EmailAddress:

  /** Matching regex. */
  private val EmailAddressRe = """^([^+@]+)@(.+)$""".r

  /** Pattern match a mail address.
    * @param a
    *   the address to match
    * @return
    *   the matched email address parts
    */
  def unapply(a: Address): Option[(String, String)] = a match
    case ia: InternetAddress =>
      ia.getAddress match
        case EmailAddressRe(username, domain) => Some((username, domain))
        case _                                => None
    case _                   => None

  /** Pattern match an email address.
    * @param a
    *   the address to match
    * @return
    *   the matched email address parts
    */
  def unapply(a: String): Option[(String, String)] = a match
    case EmailAddressRe(username, domain) => Some((username, domain))
    case _                                => None
end EmailAddress

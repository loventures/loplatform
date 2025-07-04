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

/** Representation of an email address containing a plus part.
  * @param username
  *   the username
  * @param plus
  *   the plus part
  * @param domain
  *   the domain name
  */
case class PlusEmailAddress(
  username: String,
  plus: String,
  domain: String
):

  /** Convert this to an email address.
    * @return
    *   an email address
    */
  def toAddress: String = s"$username+$plus@$domain"
end PlusEmailAddress

object PlusEmailAddress:

  /** Matching regex. */
  private val PlusEmailAddressRe = """^([^+@]+)\+([^@]+)@(.+)$""".r

  /** Pattern match a mail address.
    * @param a
    *   the address to match
    * @return
    *   the matched email address parts
    */
  def unapply(a: Address): Option[(String, String, String)] = a match
    case ia: InternetAddress =>
      ia.getAddress match
        case PlusEmailAddressRe(username, plus, domain) =>
          Some((username, plus, domain))
        case _                                          => None
    case _                   => None
end PlusEmailAddress

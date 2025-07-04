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

package loi.cp.ip

import org.apache.commons.net.util.SubnetUtils
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*

import scala.util.Try

final case class IpMatch(ips: Set[String], subnets: Set[SubnetUtils#SubnetInfo]):
  // Only run the IP through the masks if it is IPv4 (SubnetUtils break with IPv6).
  // Look for periods to check IPv4.
  def matches(ip: String): Boolean =
    ips.contains(ip) || (ip.contains('.') && subnets.exists(_.isInRange(ip)))

  def emptyOrMatches(ip: String): Boolean =
    (ips.isEmpty && subnets.isEmpty) || matches(ip)

object IpMatch:
  def parse(fa: Set[String]): IpMatch =
    val (subnets, ips) = fa.partition(isSubnet)
    IpMatch(ips, subnets.map(parseSubnet))

  def empty: IpMatch = IpMatch(Set.empty, Set.empty)

  /** Looks at a string that may be an IP or may be a subnet (an IP/Mask pair in CIDR notation) and returns true if it
    * is a subnet.
    */
  def isSubnet(str: String): Boolean = str.contains('/')

  /** Parse a string into a SubnetInfo object
    */
  def parseSubnet(str: String): SubnetUtils#SubnetInfo =
    new SubnetUtils(str).getInfo

  // Validate that this is a valid IP or IP/mask
  def validateIp(ip: String): Boolean =
    Try(new SubnetUtils(ip + (isSubnet(ip) !? "/32"))).isSuccess
end IpMatch

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

package loi.cp.network

import com.learningobjects.cpxp.component.annotation.{Controller, PathVariable, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.AdminRight

import scalaz.\/

/** The root web API for interacting with social networks.
  */
@Controller(value = "networks", root = true)
@RequestMapping(path = "networks")
trait NetworkRootApi extends ApiRootComponent:
  import NetworkRootApi.*

  /** Query all the social networks. */
  @RequestMapping(method = Method.GET)
  def get(apiQuery: ApiQuery): ApiQueryResults[Network]

  /** Get a particular social network. */
  @RequestMapping(path = "{id}", method = Method.GET)
  def get(@PathVariable("id") id: Long): Option[Network]

  /** Create a new social network. This will fail if the network id is already in use. */
  @RequestMapping(method = Method.POST)
  @Secured(Array(classOf[AdminRight]))
  def create(@RequestBody network: Network): ErrorResponse \/ Network

  /** Get a particular social network. */
  @RequestMapping(path = "{id}", method = Method.PUT)
  @Secured(Array(classOf[AdminRight]))
  def update(@PathVariable("id") id: Long, @RequestBody network: Network): ErrorResponse \/ Network

  /** Delete a social network. */
  @RequestMapping(path = "{id}", method = Method.DELETE)
  @Secured(Array(classOf[AdminRight]))
  def delete(@PathVariable("id") id: Long): WebResponse

  /** Peer one network to another. This will fail if either network is already peered. */
  @RequestMapping(path = "{id}/peer", method = Method.POST)
  @Secured(Array(classOf[AdminRight]))
  def peer(@PathVariable("id") id: Long, @RequestBody peerRequest: PeerRequest): ErrorResponse \/ Network
end NetworkRootApi

/** Network root API companion.
  */
object NetworkRootApi:

  /** A peering request.
    *
    * @param network
    *   the network to which to peer
    */
  case class PeerRequest(network: Long)

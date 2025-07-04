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

import java.lang.Long as JLong

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.component.annotation.{Controller, PathVariable, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ErrorResponse, Method}
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.AdminRight
import loi.cp.user.Profile

import scalaz.\/

/** The root web API for interacting with social network connections.
  */
@Controller(value = "connections", root = true)
@RequestMapping(path = "connections")
trait ConnectionRootApi extends ApiRootComponent:
  import ConnectionRootApi.*

  /** Query your connections. */
  @RequestMapping(method = Method.GET)
  def get(q: ApiQuery): ApiQueryResults[Connection]

  /** Get a single connection. */
  @RequestMapping(path = "{id}", method = Method.GET)
  def get(@PathVariable("id") id: Long): Option[Connection]

  /** Query connected users by network */
  @RequestMapping(path = "usersByNetwork/{network}", method = Method.GET)
  def usersByNetwork(@PathVariable("network") networkId: String, q: ApiQuery): ErrorResponse \/ ApiQueryResults[Profile]

  /** Request a social connection. */
  @RequestMapping(path = "connect", method = Method.POST)
  def connect(@RequestBody request: ConnectionRequest): ErrorResponse \/ Connection

  /** Query a user's connections. */
  @RequestMapping(path = "forUser/{id}", method = Method.GET)
  @Secured(Array(classOf[AdminRight]))
  def forUser(@PathVariable("id") id: Long, q: ApiQuery): ErrorResponse \/ ApiQueryResults[Connection]

  /** Get a user's single connection. */
  @RequestMapping(path = "forUser/{id}/{cid}", method = Method.GET)
  @Secured(Array(classOf[AdminRight]))
  def forUser(@PathVariable("id") id: Long, @PathVariable("cid") cid: Long): Option[Connection]
end ConnectionRootApi

/** Connection root API companion.
  */
object ConnectionRootApi:

  /** A connection request. Either network or networkId should be specified.
    *
    * @param user
    *   the user to which you wish to connect
    * @param network
    *   the network on which you wish to connect
    * @param networkId
    *   the identifier of the network on which you wish to connect
    */
  case class ConnectionRequest(
    user: Long,
    @JsonDeserialize(contentAs = classOf[JLong]) network: Option[Long],
    networkId: Option[String]
  )
end ConnectionRootApi

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

import java.util.Date

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.{RequestMapping, Schema}
import com.learningobjects.cpxp.component.web.Method
import com.learningobjects.de.web.{Queryable, QueryableId}
import loi.cp.user.Profile

/** A social connection.
  */
@Schema("connection")
trait Connection extends ComponentInterface with QueryableId:
  import Connection.*

  /** The network on which this connection exists. */
  @RequestMapping(path = NetworkProperty, method = Method.GET)
  @Queryable(joinComponent = classOf[Network])
  def getNetwork: Network

  /** The PK of the network on which this connection exists. */
  @JsonProperty(NetworkIdProperty)
  @Queryable
  def getNetworkId: Long

  /** The user to which this connection connects. */
  @RequestMapping(path = UserProperty, method = Method.GET)
  @Queryable(joinComponent = classOf[Profile])
  def getUser: Profile

  /** The PK of the user to which this connection connects. */
  @JsonProperty(UserIdProperty)
  @Queryable
  def getUserId: Long

  /** When this connection was created. */
  @JsonProperty
  @Queryable
  def getCreated: Date

  /** Whether this connection is active. Inactive connections are typically unacknowledged reciprocal connections that
    * will be activated upon acceptance.
    */
  @JsonProperty
  @Queryable
  def isActive: Boolean

  /** Set whether this connection is active. */
  def setActive(active: Boolean): Unit

  /** The owner of this connection; i.e. the user on which it is defined. */
  def getOwner: Long

  /** Delete this connection. This does not automatically delete a reciprocal connection.
    */
  def delete(): Unit
end Connection

/** Social connection companion.
  */
object Connection:

  /** Initialization info for a social network. */
  case class Init(network: Long, user: Long, active: Boolean)

  /* The various JSON property names. */
  final val NetworkProperty   = "network"
  final val NetworkIdProperty = "network_id"
  final val UserProperty      = "user"
  final val UserIdProperty    = "user_id"
  final val CreatedProperty   = "created"
  final val ActiveProperty    = "active"
end Connection

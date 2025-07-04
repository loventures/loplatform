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

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.service.network.{ConnectionFinder, NetworkFinder}
import com.learningobjects.cpxp.service.query.{Comparison, QueryBuilder, QueryService, Function as QBFunction}
import loi.cp.notification.NotificationService
import loi.cp.user.UserComponent
import scaloi.GetOrCreate

/** Network service implementation.
  */
@Service
class NetworkServiceImpl(implicit
  domain: () => DomainDTO,
  cs: ComponentService,
  ns: NotificationService,
  qs: QueryService
) extends NetworkService:
  import NetworkServiceImpl.*

  // There are plausible race conditions and deadlocks in this code.
  // Pedantically we would have to pessimistically lock all users on
  // both sides of the relationships in ascending PK order etc.
  // The default case of just adding some relationships is safe
  // enough to suffice.

  /** Make a connection per the connection model. */
  override def makeConnection(user: UserComponent, network: Network, to: UserComponent): Option[Connection] =
    network.getConnectionModel match
      case ConnectionModel.User =>
        Some(connect(user, network, to))

      case ConnectionModel.Request if network.getPeerNetworkId.isDefined =>
        Some(requestConnection(user, network, to).result)

      case _ => None

  /** If a peer connection request exists, create a reciprocal connection and activate the two. Else create an inactive
    * connection and send a connection request notification.
    */
  private def requestConnection(user: UserComponent, network: Network, to: UserComponent): GetOrCreate[Connection] =
    queryConnection(user, network, to)
      .getOrCreate[Connection](Connection.Init(network.getId, to.getId, active = false)) init { c =>
      // If I have created this side of the connection
      network.getPeerNetwork foreach { peer =>
        // Look for an existing reciprocal connection
        queryConnection(to, peer, user)
          .getComponent[Connection]
          .fold(notifyConnection(c)) { d =>
            // Activate both connections
            c.setActive(true)
            d.setActive(true)
          }
      }
    }

  /** Send a connection request notification. */
  private def notifyConnection(c: Connection): Unit =
    ns.nοtify[ConnectionRequestNotification](c.getUser, c)

  /** Get a network by PK. */
  override def getNetwork(id: Long): Option[Network] =
    queryNetworks.setId(id).getComponent[Network]

  /** Get a network by identifier. */
  override def getNetwork(networkId: String): Option[Network] =
    queryNetworkByNetworkId(networkId)
      .getComponent[Network]

  /** Set the connections for a user on a particular network. */
  override def setConnections(user: UserComponent, network: Network, users: Seq[UserComponent]): Unit =
    val connections    = user
      .queryChildren[ConnectionFinder]
      .addCondition(ConnectionFinder.DATA_TYPE_CONNECTION_NETWORK, Comparison.eq, network)
      .getComponents[Connection]
    val userIds        = users.map(_.getId.longValue).toSet
    val (retain, drop) = connections partition { c =>
      userIds.contains(c.getUserId)
    }
    val retainIds      = retain.map(_.getUserId).toSet
    val add            = users.filterNot(u => retainIds.contains(u.getId.longValue))
    drop foreach { c =>
      disconnect(user, network, c.getUser)
    }
    add foreach { u =>
      connect(user, network, u)
    }
  end setConnections

  /** Make a bilateral connection. */
  private def connect(user: Id, network: Network, to: Id): Connection =
    network.getPeerNetwork foreach { peer =>
      // create the peer connection, activate it if already present
      queryConnection(to, peer, user)
        .getOrCreate[Connection](Connection.Init(peer.getId, user.getId, active = true))
        .update(_.setActive(true))
    }
    queryConnection(user, network, to)
      .getOrCreate[Connection](Connection.Init(network.getId, to.getId, active = true))
      .update(_.setActive(true))
      .result
  end connect

  /** Drop a bilateral connection. */
  private def disconnect(user: Id, network: Network, to: Id): Unit =
    dropConnections(user, network, to)
    network.getPeerNetwork foreach { peer =>
      dropConnections(to, peer, user)
    }

  /** Drop all connections from one user to another on a network. */
  private def dropConnections(user: Id, network: Network, to: Id): Unit =
    queryConnection(user, network, to)
      .getComponents[Connection]
      .foreach { c =>
        c.delete()
      }
end NetworkServiceImpl

object NetworkServiceImpl:

  /** The social network folder type. */
  val NetworkFolderType = "network"

  /** Get the network folder. */
  def networkFolder(implicit qs: QueryService, domain: DomainDTO): Item =
    domain.getFolderByType(NetworkFolderType)

  /** Query the social networks. */
  def queryNetworks(implicit qs: QueryService, domain: DomainDTO): QueryBuilder =
    networkFolder.queryChildren[NetworkFinder]

  /** Query for a social network by network id. */
  def queryNetworkByNetworkId(networkId: String)(implicit qs: QueryService, domain: DomainDTO): QueryBuilder =
    queryNetworks
      .addCondition(NetworkFinder.DATA_TYPE_NETWORK_NETWORK_ID, Comparison.eq, networkId, QBFunction.LOWER)

  /** Query for a connection. */
  private def queryConnection(from: Id, network: Network, to: Id)(implicit qs: QueryService): QueryBuilder =
    from
      .queryChildren[ConnectionFinder]
      .addCondition(ConnectionFinder.DATA_TYPE_CONNECTION_USER, Comparison.eq, to)
      .addCondition(ConnectionFinder.DATA_TYPE_CONNECTION_NETWORK, Comparison.eq, network)
end NetworkServiceImpl

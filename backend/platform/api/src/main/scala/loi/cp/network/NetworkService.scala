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

import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.user.UserComponent

/** Social networking service.
  */
@Service
trait NetworkService:

  /** Makes a connection from one user to another on a social network. If the network requires reciprocal approval, a
    * connection request notification will be sent.
    *
    * @param from
    *   the user from which a connection is to be made
    * @param network
    *   the social network
    * @param to
    *   the user to whom a connection is to be made
    * @return
    *   the resulting connection, if appropriate
    */
  def makeConnection(from: UserComponent, network: Network, to: UserComponent): Option[Connection]

  /** Get a social network by its PK.
    *
    * @param id
    *   the network id
    * @return
    *   the social network, if found
    */
  def getNetwork(id: Long): Option[Network]

  /** Get a social network by its identifier.
    *
    * @param networkId
    *   the network id
    * @return
    *   the social network, if found
    */
  def getNetwork(networkId: String): Option[Network]

  /** Sets the connections for a particular user on a social network. This establishes active bilateral connections
    * between the user and the other specified users, removing any existing connections the user has on the network.
    *
    * @param user
    *   the user whose connections to set
    * @param network
    *   the social network
    * @param connections
    *   the users to which to connect
    */
  def setConnections(user: UserComponent, network: Network, connections: Seq[UserComponent]): Unit
end NetworkService

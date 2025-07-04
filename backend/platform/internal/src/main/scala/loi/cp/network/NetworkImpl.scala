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

import com.learningobjects.cpxp.component.annotation.{Component, PostCreate}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.network.NetworkFinder
import com.learningobjects.cpxp.service.query.QueryService

/** Implementation of the social network component.
  */
@Component
class NetworkImpl(val componentInstance: ComponentInstance, self: NetworkFinder)(implicit
  is: ItemService,
  qs: QueryService,
  cs: ComponentService
) extends Network
    with ComponentImplementation:

  import NetworkImpl.*

  @PostCreate
  private def init(network: Network): Unit =
    update(network)

  override def update(network: Network): Unit =
    self.networkId = network.getNetworkId
    self.name = network.getName
    self.connectionModel = network.getConnectionModel.entryName

  override def getId = componentInstance.getId

  override def getNetworkId: String = self.networkId

  override def getName: String = self.name

  override def getConnectionModel: ConnectionModel = ConnectionModel.withName(self.connectionModel)

  override def getPeerNetworkId: Option[Long] = getPeerNetwork.map(_.getId.longValue)

  override def getPeerNetwork: Option[Network] = Option(self.peerNetwork).flatMap(_.component_?[Network])

  override def setPeerNetwork(network: Option[Network]): Unit =
    self.peerNetwork = network.flatMap(_.finder_?[NetworkFinder]).orNull

  override def delete(): Unit =
    val guid    = Current.deleteGuid()
    val deleted =
      jpaql"UPDATE ConnectionFinder SET del = $guid WHERE del IS NULL AND network.id = $getId".executeUpdate()
    logger.debug(s"Deleted $deleted connections")
    is.delete(self)
end NetworkImpl

object NetworkImpl:
  private val logger = org.log4s.getLogger

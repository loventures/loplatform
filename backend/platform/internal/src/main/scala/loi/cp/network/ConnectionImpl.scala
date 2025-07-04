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

import com.learningobjects.cpxp.component.annotation.{Component, PostCreate}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.network.{ConnectionFinder, NetworkFinder}
import loi.cp.user.Profile

/** Social connection component implementation.
  */
@Component
class ConnectionImpl(
  val componentInstance: ComponentInstance,
  self: ConnectionFinder
)(implicit is: ItemService, cs: ComponentService)
    extends Connection
    with ComponentImplementation:
  import Connection.*

  @PostCreate
  private def init(init: Init): Unit =
    self.active = init.active
    self.created = Current.getTime
    self.user = init.user.item
    self.network = init.network.finder[NetworkFinder]

  override def getId = componentInstance.getId

  override def getNetwork: Network = self.network.component[Network]

  override def getNetworkId: Long = getNetwork.getId.longValue

  override def getUser: Profile = self.user.component[Profile]

  override def getUserId: Long = getUser.getId.longValue

  override def getCreated: Date = self.created

  override def isActive: Boolean = self.active

  override def setActive(active: Boolean): Unit = self.active = active

  override def getOwner: Long = self.getParent.getId.longValue

  override def delete(): Unit = is.delete(self)
end ConnectionImpl

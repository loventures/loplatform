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
import com.learningobjects.cpxp.component.annotation.{Component, PostCreate}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import scaloi.syntax.CollectionBoxOps.*
import loi.cp.notification.{Interest, NotificationFacade, NotificationImplementation}

/** Connection request notification implementation. This is a request from the sender for the recipient to establish a
  * reciprocal connection and identifies the network which the sender wishes that connection on. That is to say, a
  * student connecting to their advisor would send a request for a reciprocal advisee connection.
  */
@Component
class ConnectionRequestNotificationImpl(
  val componentInstance: ComponentInstance,
  val self: NotificationFacade
)(implicit cs: ComponentService)
    extends ConnectionRequestNotification
    with NotificationImplementation
    with ComponentImplementation:

  @SuppressWarnings(Array("unused"))
  @PostCreate
  private def init(connection: Connection): Unit =
    self.setTime(connection.getCreated)
    self.setSender(Some(connection.getOwner))
    self.setData(ConnectionRequestData(connection.getNetwork.getPeerNetworkId))

  override def audience: Iterable[Long] = Option(self.getParentId).toSeq.unboxInside()

  override def interest: Interest = Interest.Alert

  override def getNetwork: Option[Network] = getData.network.flatMap(_.tryComponent[Network])

  private def getData: ConnectionRequestData = self.getData(classOf[ConnectionRequestData])
end ConnectionRequestNotificationImpl

case class ConnectionRequestData(@JsonDeserialize(contentAs = classOf[JLong]) network: Option[Long])

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
import javax.validation.groups.Default

import com.fasterxml.jackson.annotation.{JsonProperty, JsonView}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.{RequestMapping, Schema}
import com.learningobjects.cpxp.component.web.Method
import com.learningobjects.de.web.{Queryable, QueryableId}

/** A social network.
  */
@Schema("network")
trait Network extends ComponentInterface with QueryableId:
  import Network.*

  /** The network identifier. This is principally used to identify the network in integration operations (LTI / import).
    */
  @JsonProperty
  @Queryable
  def getNetworkId: String

  /** The human-readable network name. */
  @JsonProperty
  @Queryable(traits = Array(Queryable.Trait.CASE_INSENSITIVE))
  def getName: String

  /** The connection model for this network. */
  @JsonProperty
  @Queryable
  def getConnectionModel: ConnectionModel

  /** The peer to this network; e.g. advisor <-> advisee. */
  @RequestMapping(path = PeerNetworkProperty, method = Method.GET)
  def getPeerNetwork: Option[Network]

  /** The PK of the peer to this network; e.g. advisor <-> advisee. */
  @JsonView(Array(classOf[Default]))
  @JsonProperty(PeerNetworkIdProperty)
  @JsonDeserialize(contentAs = classOf[JLong])
  @Queryable
  def getPeerNetworkId: Option[Long]

  /** Update this network. Only affects the simple properties, not the peer. */
  def update(network: Network): Unit

  /** Delete this network. */
  def delete(): Unit

  /** Set the peer network. */
  def setPeerNetwork(network: Option[Network]): Unit
end Network

/** Social network companion.
  */
object Network:
  /* The various JSON property names. */
  final val NetworkIdProperty       = "networkId"
  final val NameProperty            = "name"
  final val ConnectionModelProperty = "connectionModel"
  final val PeerNetworkIdProperty   = "peerNetwork_id"
  final val PeerNetworkProperty     = "peerNetwork"

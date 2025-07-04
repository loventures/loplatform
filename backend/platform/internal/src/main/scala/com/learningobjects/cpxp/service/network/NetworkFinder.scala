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

package com.learningobjects.cpxp.service.network

import jakarta.persistence.*

import com.learningobjects.cpxp.entity.annotation.{FriendlyName, FunctionalIndex}
import com.learningobjects.cpxp.entity.{IndexType, PeerEntity}
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

@Entity
@HCache(usage = READ_WRITE)
class NetworkFinder extends PeerEntity:
  @Column
  var name: String = scala.compiletime.uninitialized

  @Column
  @FriendlyName
  @FunctionalIndex(function = IndexType.LCASE, byParent = true, nonDeleted = true)
  var networkId: String = scala.compiletime.uninitialized

  @Column(length = 32)
  var connectionModel: String = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var peerNetwork: NetworkFinder = scala.compiletime.uninitialized
end NetworkFinder

object NetworkFinder:
  final val ITEM_TYPE_NETWORK = "Network"

  final val DATA_TYPE_NETWORK_NAME = "Network.name"

  final val DATA_TYPE_NETWORK_NETWORK_ID = "Network.networkId"

  final val DATA_TYPE_NETWORK_CONNECTION_MODEL = "Network.connectionModel"

  final val DATA_TYPE_NETWORK_PEER = "Network.peerNetwork"
end NetworkFinder

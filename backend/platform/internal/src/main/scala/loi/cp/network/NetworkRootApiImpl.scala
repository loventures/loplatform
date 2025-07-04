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

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.{ApiQueries, ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ErrorResponse, NoContentResponse, WebResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import scaloi.syntax.AnyOps.*
import scaloi.syntax.BooleanOps.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.query.QueryService

import scalaz.\/
import scalaz.syntax.std.option.*

// How does one model who has permission to use a particular network?
// How should we handle user deletion wrt connections...

/** Network root API implementation.
  */
@Component
class NetworkRootApiImpl(val componentInstance: ComponentInstance)(implicit
  domain: DomainDTO,
  cs: ComponentService,
  is: ItemService,
  ns: NetworkService,
  qs: QueryService
) extends NetworkRootApi
    with ComponentImplementation:
  import NetworkRootApi.*
  import NetworkServiceImpl.*

  override def get(id: Long): Option[Network] =
    get(ApiQuery.byId(id)).asOption

  override def get(apiQuery: ApiQuery): ApiQueryResults[Network] =
    ApiQueries.query[Network](queryNetworks, apiQuery)

  override def create(network: Network): ErrorResponse \/ Network =
    queryNetworkByNetworkId(network.getNetworkId)
      .getOrCreate[Network](network)
      .createdOr(duplicateNetworkIdError(network))

  override def update(id: Long, network: Network): ErrorResponse \/ Network =
    for
      // does the network exist
      existing <- get(id) \/> ErrorResponse.notFound
      // can i lock the folder
      _        <- is.pessimisticLock(networkFolder) \/> ErrorResponse.serverError
      // is the network id available
      _        <- networkIdAvailable(id, network.getNetworkId) \/> duplicateNetworkIdError(network)
    yield existing <| { _.update(network) }

  // Is this network id available
  private def networkIdAvailable(id: Long, networkId: String): Boolean =
    ns.getNetwork(networkId).forall(_.getId.longValue == id)

  private def duplicateNetworkIdError(network: Network): ErrorResponse =
    ErrorResponse.validationError(Network.NetworkIdProperty, network.getNetworkId)("Duplicate network id")

  override def delete(id: Long): WebResponse =
    get(id).fold[WebResponse](ErrorResponse.notFound) { network =>
      network.delete()
      NoContentResponse
    }

  override def peer(id: Long, request: PeerRequest): ErrorResponse \/ Network =
    for
      // does the network exist
      network <- get(id) \/> ErrorResponse.notFound
      // is the network peerable
      _       <- validatePeerNetwork(id).leftMap(ErrorResponse.validationError("id", id))
      // is the peer network peerable
      peer    <- validatePeerNetwork(request.network).leftMap(ErrorResponse.validationError("network", request.network))
    yield
      // connect the networks
      network.setPeerNetwork(Some(peer))
      peer.setPeerNetwork(Some(network))
      network

  /** Validate a peer network. Returns an error if the network is unpeerable. */
  private def validatePeerNetwork(id: Long): String \/ Network =
    for
      network <- get(id) \/> "Network not found"                                      // if it is not found, error
      _       <- network.getPeerNetworkId.map(_ => "Network already peered") <\/ (()) // if it has a peer, error
    yield network
end NetworkRootApiImpl

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
import com.learningobjects.cpxp.component.query.*
import com.learningobjects.cpxp.component.web.ErrorResponse
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.network.ConnectionFinder
import com.learningobjects.cpxp.service.query.{Comparison, QueryBuilder, QueryService}
import com.learningobjects.cpxp.service.user.{UserConstants, UserDTO, UserState}
import loi.cp.user.{Profile, UserComponent}
import scalaz.\/
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scaloi.\|/
import scaloi.syntax.BooleanOps.*

/** Connection root API implementation.
  */
@Component
class ConnectionRootApiImpl(val componentInstance: ComponentInstance)(implicit
  domain: DomainDTO,
  is: ItemService,
  myself: UserDTO,
  ns: NetworkService,
  qs: QueryService,
  cs: ComponentService
) extends ConnectionRootApi
    with ComponentImplementation:
  import ConnectionRootApi.*

  /** Get a connection by PK. */
  override def get(id: Long): Option[Connection] =
    get(ApiQuery.byId(id)).asOption

  /** Query connections. */
  override def get(q: ApiQuery): ApiQueryResults[Connection] =
    ApiQueries.query[Connection](queryMyConnections, q)

  /** Query connected profiles. */
  override def usersByNetwork(networkId: String, q: ApiQuery): ErrorResponse \/ ApiQueryResults[Profile] =
    for network <- ns.getNetwork(networkId) \/> ErrorResponse.notFound
    yield
      // query for my active connection on this network
      val iqb = queryMyConnections
        .addCondition(ConnectionFinder.DATA_TYPE_CONNECTION_NETWORK, Comparison.eq, network)
        .addCondition(ConnectionFinder.DATA_TYPE_CONNECTION_ACTIVE, Comparison.eq, true)
        .setDataProjection(ConnectionFinder.DATA_TYPE_CONNECTION_USER)
      // query users in my network
      val qb  = queryUsers.addInitialQuery(iqb)
      // add profile property mappings
      val pq  = new ApiQuery.Builder(q).addPropertyMappings(classOf[Profile]).build
      ApiQuerySupport.query(qb, pq, classOf[Profile])

  /** Query my connections. */
  private def queryMyConnections: QueryBuilder =
    myself.queryChildren[ConnectionFinder]

  /** Connect to a user. Depending on the configuration, this may result in a connection request.
    */
  override def connect(request: ConnectionRequest): ErrorResponse \/ Connection =
    for
      network    <- validateNetwork(\|/(request.network, request.networkId))
      to         <- validateUser(request.user).leftMap(ErrorResponse.validationError("user", request.user))
      connection <- ns.makeConnection(myself.component[UserComponent], network, to) \/> ErrorResponse.badRequest
    yield connection

  /** Get a user's connection by PK. */
  override def forUser(id: Long, cid: Long): Option[Connection] =
    queryConnections(id).flatMap(_.setId(cid).getComponent[Connection])

  /** Query a user's connections. */
  override def forUser(id: Long, q: ApiQuery): ErrorResponse \/ ApiQueryResults[Connection] =
    queryConnections(id).map(ApiQueries.query[Connection](_, q)) \/> ErrorResponse.notFound

  private def queryConnections(id: Long): Option[QueryBuilder] =
    Option(is.get(id, UserConstants.ITEM_TYPE_USER)).map(_.queryChildren[ConnectionFinder])

  import \|/.*

  /** Validate the network in a connection request.
    * @param network
    *   the requested network
    * @return
    *   the network, or an error
    */
  private def validateNetwork(network: Long \|/ String): ErrorResponse \/ Network = network match
    case This(id)           =>
      validateNetwork(ns.getNetwork(id)).leftMap(ErrorResponse.validationError("network", id))
    case That(networkId)    =>
      validateNetwork(ns.getNetwork(networkId)).leftMap(ErrorResponse.validationError("networkId", networkId))
    case Both(_, networkId) =>
      ErrorResponse.validationError("networkId", networkId)("Duplicate network specification").left
    case Neither()          =>
      ErrorResponse.validationError("network", null)("Missing value").left

  /** Validate that a network is valid and supports connection.
    * @param network
    *   the network in question
    * @return
    *   the network, or an error message
    */
  private def validateNetwork(network: Option[Network]): String \/ Network =
    for
      n <- network \/> "Unknown network"
      _ <- validateConnectionModel(n.getConnectionModel)
    yield n

  /** Validate that a connection model supports connections.
    * @param cm
    *   the connection model
    * @return
    *   nothing, or an error message
    */
  private def validateConnectionModel(cm: ConnectionModel): String \/ Unit =
    (cm == ConnectionModel.System) `thenLeft` s"Invalid connection model: $cm"

  /** Validate that a user supports connections.
    * @param id
    *   the user id
    * @return
    *   the user, or an error message
    */
  private def validateUser(id: Long): String \/ UserComponent =
    for
      _    <- (id == myself.getId.longValue) `thenLeft` "Cannot connect to yourself"
      user <- getUser(id) \/> "Unknown user"
      _    <- validateUserState(user.getUserState)
    yield user

  /** Validate a user's state.
    * @param us
    *   the user state
    * @return
    *   nothing, or an error message
    */
  private def validateUserState(us: UserState): String \/ Unit =
    (us == UserState.Active) \/> s"Invalid user state $us"

  /** The users folder. */
  private def queryUsers: QueryBuilder =
    domain
      .getFolderById(UserConstants.ID_FOLDER_USERS)
      .queryChildren(UserConstants.ITEM_TYPE_USER)

  /** Get a user by PK. */
  private def getUser(id: Long): Option[UserComponent] =
    queryUsers.setId(id).getComponent[UserComponent]
end ConnectionRootApiImpl

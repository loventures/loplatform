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

package loi.cp.imports
package importers

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.integration.IntegrationWebService
import com.learningobjects.cpxp.service.query.QueryService
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.imports.errors.*
import loi.cp.network.NetworkService
import scalaz.\/
import scalaz.syntax.apply.*
import scalaz.syntax.std.option.*

/** Social network connection importer. All imports are treated as a 'set' operation, setting the relation of the 'from'
  * user on the network to the 'to' user, so existing connections for the user on the specified network are replaced. If
  * the 'to' side of the connection is absent, the user's connections on the network are removed. A subsequent revision
  * might add an 'action' verb to allow set/add/remove.
  */
@Component(name = "$$name=Connections")
@ImportBinding(
  value = classOf[ConnectionImportItem],
  label = "loi.cp.imports.ConnectionImportItem.label"
)
class ConnectionImporter(val componentInstance: ComponentInstance)(implicit
  val facadeService: FacadeService,
  val integrationWebService: IntegrationWebService,
  val networkService: NetworkService,
  val queryService: QueryService,
  val componentService: ComponentService
) extends ValidatingImporter[ConnectionImportItem]
    with ImporterWithIntegration
    with ImporterWithUser
    with DeserializingCsvImporter
    with ImporterConverters
    with ComponentImplementation:
  import ConnectionImporter.*
  import Importer.*

  override val log = org.log4s.getLogger

  override def requiredHeaders: Set[String] = Set(NetworkId)

  override def allHeaders: Set[String] = Set(
    FromIntegrationConnectorId,
    FromIntegrationUniqueId,
    FromExternalId,
    FromUserName,
    NetworkId,
    ToIntegrationConnectorId,
    ToIntegrationUniqueId,
    ToExternalId,
    ToUserName
  )

  protected def validateItem(ii: ConnectionImportItem) =
    validateNonEmpty(ii, "networkId", _.networkId.getOrElse(""))

  private val deserializeFromIds =
    getIds(FromUserName, FromExternalId, FromIntegrationUniqueId, FromIntegrationConnectorId)

  private val deserializeToIds =
    getIds(ToUserName, ToExternalId, ToIntegrationUniqueId, ToIntegrationConnectorId)

  override def deserializeCsvRow(headers: Seq[String], values: Seq[String]): ValidationError \/ ConnectionImportItem =
    ifHeadersMatchValues(headers, values) { columns =>
      val (fromUserName, fromExternalId, fromIntg) = deserializeFromIds(columns)
      val validFromIds                             = areIdsValid("from", fromUserName, fromExternalId, fromIntg)

      val networkId = columns.failIfNone(NetworkId)

      val (toUserName, toExternalId, toIntg) = deserializeToIds(columns)
      val validToIds                         = areOptionalIdsValid("to", toUserName, toExternalId, toIntg)

      val validated =
        (fromIntg |@| validFromIds |@| networkId |@| toIntg |@| validToIds) { (fromIntg, _, network, toIntg, _) =>
          ConnectionImportItem(fromUserName, fromExternalId, fromIntg, Some(network), toUserName, toExternalId, toIntg)
        }

      validated.leftMap(violations => ValidationError(violations)).toDisjunction
    }

  override def execute(invoker: UserDTO, validated: Validated[ConnectionImportItem]): PersistError \/ ImportSuccess =
    val connection = validated.item
    log.info(s"Importing: $connection")

    for
      network <- networkService.getNetwork(connection.networkId.get) \/> PersistError(
                   s"Network with id ${connection.networkId.get} does not exist."
                 )
      from    <- getUser(connection.fromUserName.toOption, connection.fromExternalId.toOption, connection.fromIntegration)
      to      <- getUserOption(connection.toUserName.toOption, connection.toExternalId.toOption, connection.toIntegration)
    yield
      networkService.setConnections(from, network, to.toSeq)
      ImportSuccess(Some(connection))
  end execute
end ConnectionImporter

object ConnectionImporter:
  final val FromIntegrationConnectorId = "FromIntegrationConnectorId"
  final val FromIntegrationUniqueId    = "fromIntegrationUniqueId"
  final val FromExternalId             = "fromExternalId"
  final val FromUserName               = "fromUserName"
  final val NetworkId                  = "networkId"
  final val ToIntegrationConnectorId   = "toIntegrationConnectorId"
  final val ToIntegrationUniqueId      = "toIntegrationUniqueId"
  final val ToExternalId               = "toExternalId"
  final val ToUserName                 = "toUserName"
end ConnectionImporter

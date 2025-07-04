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

import com.learningobjects.cpxp.component.{ComponentInterface, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.json.OptionalField
import com.learningobjects.cpxp.service.integration.IntegrationWebService
import loi.cp.imports.errors.{FieldViolation, PersistError, Violation}
import loi.cp.integration.SystemComponent
import scalaz.*
import scalaz.std.option.*
import scalaz.syntax.either.*
import scalaz.syntax.functor.*
import scalaz.syntax.std.option.*
import scalaz.syntax.validation.*

import scala.reflect.ClassTag

trait ImporterWithIntegration:
  importer: DeserializingCsvImporter =>

  import ImporterWithIntegration.*

  val integrationWebService: IntegrationWebService
  implicit val componentService: ComponentService

  def getIntegrationFromColumns(
    uniqueIdColumn: Option[String],
    connectorIdColumn: Option[String],
  ): ViolationNel[Option[IntegrationImportItem]] =
    (uniqueIdColumn, connectorIdColumn) match
      case (None, Some(connectorId))           =>
        FieldViolation("integrationUniqueId", "", EmptyUniqueIdMessage)
          .failureNel[Option[IntegrationImportItem]]
      case (Some(uniqueId), None)              =>
        FieldViolation("integrationConnectorId", "", EmptyConnectorIdMessage)
          .failureNel[Option[IntegrationImportItem]]
      case (Some(uniqueId), Some(connectorId)) =>
        IntegrationImportItem(uniqueId, connectorId).some
          .successNel[Violation]
      case (None, None)                        =>
        Option.empty[IntegrationImportItem].successNel[Violation]

  def getSystem(
    integration: Option[IntegrationImportItem]
  )(implicit iws: IntegrationWebService): PersistError \/ Option[SystemComponent[?]] =
    integration.fold(Option.empty[SystemComponent[?]].right[PersistError]) { i =>
      getSystemForConnectorId(i.connectorId) match
        case None       =>
          PersistError(s"Integration Connector with id: ${i.connectorId} not found").left
        case someSystem => someSystem.right[PersistError]
    }

  def getSystemForConnectorId(connectorId: String): Option[SystemComponent[?]] =
    Option(integrationWebService.getById(connectorId)).map(_.component[SystemComponent[?]])

  def findComponentByConnector[T <: ComponentInterface: ClassTag](
    c: IntegrationImportItem,
    itemType: String
  ): PersistError \/ T =
    getSystemForConnectorId(c.connectorId) match
      case Some(connector) =>
        val componentId = integrationWebService
          .findByUniqueId(connector.getId, c.uniqueId, itemType)
        Option(componentId.component[T]) match
          case Some(c) => c.right
          case None    =>
            PersistError(s"${itemType.capitalize} with id: ${c.uniqueId} doesn't exist").left
      case None            =>
        PersistError(s"Connector with id: ${c.connectorId} doesn't exist").left

  def getIds(idField: String, externalIdField: String, integrationIdField: String, connectorIdField: String)(
    columns: CsvRow
  ) = (
    columns.getOptionalField(idField),
    columns.getOptionalField(externalIdField),
    getIntegrationFromColumns(
      columns.getOptional(integrationIdField).flatten,
      columns.getOptional(connectorIdField).flatten
    )
  )

  def areIdsValid(
    idType: String,
    id: OptionalField[String],
    externalId: OptionalField[String],
    integration: ViolationNel[Option[IntegrationImportItem]]
  ) =
    ValidationUtils
      .exactlyOneWhen(idType, integration.isSuccess)(toValidations(id, externalId, integration))

  def areOptionalIdsValid(
    idType: String,
    id: OptionalField[String],
    externalId: OptionalField[String],
    integration: ViolationNel[Option[IntegrationImportItem]]
  ) =
    ValidationUtils
      .atMostOneWhen(idType, integration.isSuccess)(toValidations(id, externalId, integration))

  /* this method is deeply suspicious --hh */
  private def toValidations(
    id: OptionalField[String],
    externalId: OptionalField[String],
    integration: ViolationNel[Option[IntegrationImportItem]]
  ): List[ViolationNel[Option[Unit]]] =
    List(
      id.toOption.void.successNel[Violation],
      externalId.toOption.void.successNel[Violation],
      integration.map(_.void),
    )
end ImporterWithIntegration

object ImporterWithIntegration:

  final val EmptyUniqueIdMessage    =
    "integrationUniqueId cannot be empty if integrationConnectorId is non-empty"
  final val EmptyConnectorIdMessage =
    "integrationConnectorId cannot be empty if integrationUniqueId is non-empty"

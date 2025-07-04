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

package loi.cp.learnertransfer
package importers

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{AbstractComponent, ComponentService}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.integration.IntegrationWebService
import com.learningobjects.cpxp.service.query.QueryService
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.ManagedUtils
import loi.cp.imports.*
import loi.cp.imports.errors.{PersistError, ValidationError}
import loi.cp.imports.importers.*
import loi.cp.role.RoleService
import scalaz.*
import scalaz.syntax.applicative.*
import scalaz.syntax.either.*
import scalaz.syntax.foldable.*

@Component(name = "$$name=Learner Transfer Importer")
@ImportBinding(
  value = classOf[LearnerTransferImportItem],
  label = "loi.cp.imports.LearnerTransferImportItem.label"
)
class LearnerTransferImporter(implicit
  val facadeService: FacadeService,
  val integrationWebService: IntegrationWebService,
  val learnerTransferService: LearnerTransferService,
  val queryService: QueryService,
  val roleService: RoleService,
  val componentService: ComponentService
) extends AbstractComponent
    with Importer[LearnerTransferImportItem]
    with ImporterWithEnrollment
    with ImporterWithUser
    with ImporterConverters:
  import Importer.*
  import LearnerTransferImporter.*

  override val log: org.log4s.Logger = org.log4s.getLogger

  override def requiredHeaders: Set[String] = Set()

  override def allHeaders: Set[String] = Set(
    SourceCourseId,
    SourceCourseIntegrationConnectorId,
    SourceCourseIntegrationUniqueId,
    DestinationCourseIntegrationUniqueId,
    UserIntegrationConnectorId,
    DestinationCourseId,
    SourceCourseExternalId,
    DestinationCourseIntegrationConnectorId,
    UserIntegrationUniqueId,
    DestinationCourseExternalId,
    UserExternalId,
    UserName,
  )

  private val deserializeUser        =
    getIds(UserName, UserExternalId, UserIntegrationUniqueId, UserIntegrationConnectorId)
  private val deserializeSource      =
    getIds(
      SourceCourseId,
      SourceCourseExternalId,
      SourceCourseIntegrationUniqueId,
      SourceCourseIntegrationConnectorId
    )
  private val deserializeDestination =
    getIds(
      DestinationCourseId,
      DestinationCourseExternalId,
      DestinationCourseIntegrationUniqueId,
      DestinationCourseIntegrationConnectorId
    )

  override def deserializeCsvRow(
    headers: Seq[String],
    values: Seq[String]
  ): ValidationError \/ LearnerTransferImportItem =
    ifHeadersMatchValues(headers, values) { columns =>
      val (userName, userExternalId, userIntg) = deserializeUser(columns)
      val validUserIds                         = areIdsValid("user", userName, userExternalId, userIntg)

      val (sourceCourseId, sourceCourseExternalId, sourceCourseIntg) = deserializeSource(columns)
      val validSourceCourseIds                                       = areIdsValid("sourceCourse", sourceCourseId, sourceCourseExternalId, sourceCourseIntg)

      val (destinationCourseId, destinationCourseExternalId, destinationCourseIntg) = deserializeDestination(columns)
      val validDestinationCourseIds                                                 =
        areIdsValid("destinationCourse", destinationCourseId, destinationCourseExternalId, destinationCourseIntg)

      val validated = (userIntg |@| validUserIds |@| sourceCourseIntg |@| validSourceCourseIds
        |@| destinationCourseIntg |@| validDestinationCourseIds)(
        (userIntg, _, sourceCourseIntg, _, destinationCourseIntg, _) =>
          LearnerTransferImportItem(
            userName,
            userExternalId,
            userIntg,
            sourceCourseId,
            sourceCourseExternalId,
            sourceCourseIntg,
            destinationCourseId,
            destinationCourseExternalId,
            destinationCourseIntg,
          )
      )

      validated
        .leftMap(violations => ValidationError(violations))
        .toDisjunction
    }

  def performTransfer(
    transfer: LearnerTransferImportItem,
    userId: Long,
    sourceSectionId: Long,
    destSectionId: Long
  ): \/[PersistError, ImportSuccess] =

    val result  = learnerTransferService.transferLearner(userId, sourceSectionId, destSectionId)
    val details = s"transfer user<$userId> from course<$sourceSectionId> to course<$destSectionId>"
    result match
      case -\/(errors) =>
        ManagedUtils.rollback() // rollback and start a new TX.. what a shit
        log.info(s"""[FAILURE] $details, errors: ${errors.toStream.mkString("; ")}""")
        PersistError(errors).left
      case \/-(_)      =>
        log.info(s"[SUCCESS] $details")
        ImportSuccess(Some(transfer)).right
  end performTransfer

  override def execute(
    invoker: UserDTO,
    validated: Validated[LearnerTransferImportItem]
  ): \/[PersistError, ImportSuccess] =
    val transfer = validated.item
    log.info(s"Importing: $transfer")
    for
      user              <- getUser(transfer.userName.toOption, transfer.userExternalId.toOption, transfer.userIntegration)
      sourceCourse      <- getCourse(
                             transfer.sourceCourseId.toOption,
                             transfer.sourceCourseExternalId.toOption,
                             transfer.sourceCourseIntegration
                           )
      destinationCourse <- getCourse(
                             transfer.destinationCourseId.toOption,
                             transfer.destinationCourseExternalId.toOption,
                             transfer.destinationCourseIntegration
                           )
      success           <- performTransfer(transfer, user.getId, sourceCourse.getId, destinationCourse.getId)
    yield success
    end for
  end execute
end LearnerTransferImporter

object LearnerTransferImporter:

  final val UserName                   = "userName"
  final val UserExternalId             = "userExternalId"
  final val UserIntegrationUniqueId    = "userIntegrationUniqueId"
  final val UserIntegrationConnectorId = "userIntegrationConnectorId"

  final val SourceCourseId                     = "sourceCourseId"
  final val SourceCourseExternalId             = "sourceCourseExternalId"
  final val SourceCourseIntegrationUniqueId    = "sourceCourseIntegrationUniqueId"
  final val SourceCourseIntegrationConnectorId = "sourceCourseIntegrationConnectorId"

  final val DestinationCourseId                     = "destinationCourseId"
  final val DestinationCourseExternalId             = "destinationCourseExternalId"
  final val DestinationCourseIntegrationUniqueId    = "destinationCourseIntegrationUniqueId"
  final val DestinationCourseIntegrationConnectorId = "destinationCourseIntegrationConnectorId"
end LearnerTransferImporter

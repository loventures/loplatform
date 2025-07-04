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
import com.learningobjects.cpxp.component.{AbstractComponent, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.json.OptionalField
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.EnrollmentType
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.integration.IntegrationWebService
import com.learningobjects.cpxp.service.query.QueryService
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.group.GroupComponent
import loi.cp.enrollment.{EnrollmentComponent, EnrollmentService}
import loi.cp.imports.errors.*
import loi.cp.role.{RoleComponent, RoleService, RoleType}
import loi.cp.user.UserComponent
import scalaz.*
import scalaz.syntax.applicative.*
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scalaz.syntax.validation.*

import java.util.Date

@Component(name = "$$name=Enrollments")
@ImportBinding(
  value = classOf[EnrollmentImportItem],
  label = "loi.cp.imports.EnrollmentImportItem.label"
)
class EnrollmentImporter(
  enrollmentService: EnrollmentService,
  val integrationWebService: IntegrationWebService,
  val queryService: QueryService,
)(implicit
  facadeService: FacadeService,
  val componentService: ComponentService,
  roleService: RoleService,
) extends AbstractComponent
    with ValidatingImporter[EnrollmentImportItem]
    with ImporterWithEnrollment
    with ImporterWithUser
    with ImporterConverters:
  import EnrollmentImporter.*
  import Importer.*

  override val log = org.log4s.getLogger

  override def requiredHeaders: Set[String] = Set("role", "status")

  override def allHeaders: Set[String] = Set(
    UserName,
    UserExternalId,
    UserIntegrationUniqueId,
    UserIntegrationConnectorId,
    /* deprecated */
    ContextId,
    CourseExternalId,
    CourseIntegrationUniqueId,
    CourseIntegrationConnectorId,
    GroupId,
    GroupExternalId,
    GroupIntegrationUniqueId,
    GroupIntegrationConnectorId,
    Role,
    DepartmentUniqueId,
    StartTime,
    EndTime,
    Status,
  )

  private val getUserIds  =
    getIds(UserName, UserExternalId, UserIntegrationUniqueId, UserIntegrationConnectorId)
  private val getGroupIds =
    getIds(GroupId, GroupExternalId, GroupIntegrationUniqueId, GroupIntegrationConnectorId)

  override def validateItem(item: EnrollmentImportItem): ViolationNel[EnrollmentImportItem] =
    validateNonEmpty(item, "role", _.role)

  override def deserializeCsvRow(headers: Seq[String], values: Seq[String]): ValidationError \/ EnrollmentImportItem =
    ifHeadersMatchValues(headers, values) { columns0 =>
      val columns = columns0.mapHeaders(EnrollmentImportItem.deprecatedNameMap)

      val (userName, userExternalId, userIntg) = getUserIds(columns)
      val validUserIds                         = areIdsValid("user", userName, userExternalId, userIntg)

      val (groupId, groupExternalId, groupIntg) = getGroupIds(columns)
      val validGroupIds                         = areIdsValid("group", groupId, groupExternalId, groupIntg)

      // .......... Enrollment ..........
      val role               = columns.failIfNone("role")
      val departmentUniqueId =
        columns.getOptionalField("departmentUniqueId").successNel[Violation]
      val startTime          = columns.getOptionalDate("startTime")
      val endTime            = columns.getOptionalDate("endTime")
      val status             = getStatus(columns, "status")

      val validated =
        (userIntg |@| validUserIds |@| groupIntg |@| validGroupIds |@|
          role |@| departmentUniqueId |@| startTime |@| endTime |@| status)(
          (userIntg, _, groupIntg, _, role, departmentUniqueId, startTime, endTime, status) =>
            EnrollmentImportItem(
              userName = userName,
              userExternalId = userExternalId,
              userIntegration = userIntg,
              groupId = groupId,
              groupExternalId = groupExternalId,
              groupIntegration = groupIntg,
              role = role,
              departmentUniqueId = departmentUniqueId,
              startTime = startTime.map(dateToString),
              endTime = endTime.map(dateToString),
              status = status,
            )
        )

      validated
        .leftMap(violations => ValidationError(violations))
        .toDisjunction
    }

  override def execute(invoker: UserDTO, validated: Validated[EnrollmentImportItem]): PersistError \/ ImportSuccess =
    val item = validated.item
    log.info(s"Importing: $item")

    def createEnrollment(
      group: GroupComponent,
      user: UserComponent,
      role: RoleComponent
    ): PersistError \/ ImportSuccess =

      val created = for
        startTime <- Traverse[OptionalField].traverse(item.startTime)(parseDate("startTime"))
        endTime   <- Traverse[OptionalField].traverse(item.endTime)(parseDate("endTime"))
      yield enrollmentService
        .setEnrollment(
          user.userId,
          group,
          RoleType(role),
          Some(BatchImportDataSource),
          startTime.toOption,
          endTime.toOption,
          item.status == EnrollmentImportItemStatus.disabled
        )

      created.as(ImportSuccess(Some(item)))
    end createEnrollment

    def updateEnrollment(
      enrollment: EnrollmentComponent,
      role: RoleComponent,
    ): PersistError \/ ImportSuccess =
      for
        startTime <- Traverse[OptionalField].traverse(item.startTime)(parseDate("startTime"))
        endTime   <- Traverse[OptionalField].traverse(item.endTime)(parseDate("endTime"))
      yield
        enrollmentService.updateEnrollment(
          enrollment,
          RoleType(role),
          enrollment.getDataSource,
          startTime.toOption(Option(enrollment.getStartTime)),
          endTime.toOption(Option(enrollment.getStopTime)),
          item.status == EnrollmentImportItemStatus.disabled
        )
        ImportSuccess(Some(item))

    def deleteEnrollment(enrollment: EnrollmentComponent): PersistError \/ ImportSuccess =
      enrollmentService.deleteEnrollment(enrollment.getId)
      ImportSuccess(None).right

    // execute
    for
      role               <- getRole(item.role)
      user               <- getUser(
                              item.userName.toOption,
                              item.userExternalId.toOption,
                              item.userIntegration
                            )
      group              <-
        import item.*
        getCourse(groupId.toOption, groupExternalId.toOption, groupIntegration)
      supported          <- group.getSupportedRole(role) \/> PersistError(s"'${role.getRoleId}' is not a valid role for $group")
      existingEnrollments = enrollmentService.loadEnrollments(user.getId, group.getId, EnrollmentType.ALL)
      delete              = item.status eq EnrollmentImportItemStatus.deleted
      res                <- existingEnrollments match
                              case Nil                =>
                                if delete then
                                  // As designed, but is this reasonable behavior?
                                  PersistError(s"Cannot delete enrollment, does not exist").left
                                else createEnrollment(group, user, role)
                              case e :: Nil           =>
                                if delete then deleteEnrollment(e.component[EnrollmentComponent])
                                else updateEnrollment(e.component[EnrollmentComponent], role)
                              case es @ (_ :: _ :: _) =>
                                // When will this happen? Should this be an assertion?
                                PersistError(s"Matched multiple enrollments: ${es.map(_.getId).mkString(", ")}").left
    yield res
    end for
  end execute

  private def parseDate(label: String)(value: String): PersistError \/ Date = stringToDate(value) match
    case scala.util.Success(date) => date.right
    case scala.util.Failure(_)    => PersistError(s"Invalid date format for field: $label").left
end EnrollmentImporter

object EnrollmentImporter:
  final val BatchImportDataSource = "BatchImport" // for the enrollment

  final val UserName                   = "userName"
  final val UserExternalId             = "userExternalId"
  final val UserIntegrationUniqueId    = "userIntegrationUniqueId"
  final val UserIntegrationConnectorId = "userIntegrationConnectorId"

  /* deprecated */
  final val ContextId                    = "courseId"
  final val CourseExternalId             = "courseExternalId"
  final val CourseIntegrationUniqueId    = "courseIntegrationUniqueId"
  final val CourseIntegrationConnectorId = "courseIntegrationConnectorId"

  final val GroupId                     = "groupId"
  final val GroupExternalId             = "groupExternalId"
  final val GroupIntegrationUniqueId    = "groupIntegrationUniqueId"
  final val GroupIntegrationConnectorId = "groupIntegrationConnectorId"

  final val Role               = "role"
  final val DepartmentUniqueId = "departmentUniqueId" // wtf?
  final val StartTime          = "startTime"
  final val EndTime            = "endTime"
  final val Status             = "status"
end EnrollmentImporter

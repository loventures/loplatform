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

package loi.cp.job.custom

import java.lang
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.TimeZone

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.ComponentInstance
import com.learningobjects.cpxp.component.annotation.{Component, Schema}
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.service.enrollment.{EnrollmentConstants, EnrollmentWebService}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.group.GroupConstants
import com.learningobjects.cpxp.service.integration.{IntegrationConstants, IntegrationWebService}
import com.learningobjects.cpxp.service.item.ItemWebService
import com.learningobjects.cpxp.service.query.{Comparison, Direction, Projection, QueryService, Function as Func}
import com.learningobjects.cpxp.service.user.UserConstants
import loi.cp.course.CourseFacade
import loi.cp.job.*
import loi.cp.user.UserComponent
import org.log4s.*
import scalaz.std.string.*
import scalaz.std.tuple.*
import scaloi.syntax.any.*
import scaloi.syntax.option.*

@Schema("ltiUsageStatisticsJob")
trait LtiUsageStatisticsJob extends EmailJob[LtiUsageStatisticsJob]:
  @JsonProperty
  def getSystemId: String
  def setSystemId(id: String): Unit

@Component
private[custom] final class LtiUsageStatisticsJobImpl(
  val componentInstance: ComponentInstance,
  val self: EmailJobFacade,
  val es: EmailService,
  val fs: FacadeService,
  intws: IntegrationWebService,
  tz: TimeZone,
)(implicit iws: ItemWebService, qs: QueryService)
    extends AbstractEmailJob[LtiUsageStatisticsJob]
    with LtiUsageStatisticsJob:

  override protected val logger: Logger = getLogger

  override def update(job: LtiUsageStatisticsJob): LtiUsageStatisticsJob =
    setSystemId(job.getSystemId)
    super.update(job)

  override def getSystemId: String = attributes.systemId[String]

  override def setSystemId(id: String): Unit = attributes.systemId = id

  override protected def generateReport(): GeneratedReport =
    val ltiSystem      = Option(intws.getSystemBySystemId(getSystemId)).getOrElse(throw UnknownSystem(getSystemId))
    val instructorRole = iws.getById(EnrollmentWebService.ROLE_INSTRUCTOR_NAME)
    val studentRole    = iws.getById(EnrollmentWebService.ROLE_STUDENT_NAME)

    val ltiIntegratedQuery                = qs
      .queryRoot(IntegrationConstants.ITEM_TYPE_INTEGRATION)
      .addCondition(IntegrationConstants.DATA_TYPE_EXTERNAL_SYSTEM, Comparison.eq, ltiSystem)
      .setProjection(Projection.PARENT_ID)
    val courses                           = GroupConstants.ID_FOLDER_COURSES
      .queryChildren(GroupConstants.ITEM_TYPE_GROUP)
      .addInitialQuery(ltiIntegratedQuery)
      .addCondition(GroupConstants.DATA_TYPE_GROUP_SUBTENANT, Comparison.eq, null)
      .setOrder(DataTypes.DATA_TYPE_CREATE_TIME, Direction.DESC)
      .getFacades[CourseFacade]
    def summaryRoleQuery(role: lang.Long) = qs
      .queryRoot(EnrollmentConstants.ITEM_TYPE_ENROLLMENT)
      .addCondition(EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP, Comparison.in, courses)
      .addCondition(EnrollmentConstants.DATA_TYPE_ENROLLMENT_ROLE, Comparison.eq, role)
      .setProjection(Projection.PARENT_ID)
    def summaryRoleCount(role: lang.Long) =
      UserConstants.ID_FOLDER_USERS
        .queryChildren(UserConstants.ITEM_TYPE_USER)
        .addCondition(DataTypes.META_DATA_TYPE_ID, Comparison.in, ltiIntegratedQuery)
        .addCondition(DataTypes.META_DATA_TYPE_ID, Comparison.in, summaryRoleQuery(role))
        .getAggregateResult(Func.COUNT)
    val instructorsCount                  = summaryRoleCount(instructorRole)
    val studentsCount                     = summaryRoleCount(studentRole)

    val when    = Instant.now
    val title   = "LTI statistics"
    val dateFmt = new SimpleDateFormat("yyyy/MM/dd") <| { _.setTimeZone(tz) }

    val summaryCsv = JobUtils.csv(s"${title}_$when.csv") { csv =>
      csv.writeRow("sections" :: "instructors" :: "students" :: Nil)
      csv.writeRow(courses.size :: instructorsCount :: studentsCount :: Nil)
    }

    val courseDetailCsv = JobUtils.csv(s"LTI sections_$when.csv") { csv =>
      csv.writeRow(
        "Section Id" :: "Name" :: "Instructor Name" :: "Instructor Email" :: "Students" :: "Section Created" :: "Project Name" :: Nil
      )
      courses foreach { course =>
        def courseRoleQuery(role: lang.Long)  = qs
          .queryRoot(EnrollmentConstants.ITEM_TYPE_ENROLLMENT)
          .addCondition(EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP, Comparison.eq, course)
          .addCondition(EnrollmentConstants.DATA_TYPE_ENROLLMENT_ROLE, Comparison.eq, role)
          .setProjection(Projection.PARENT_ID)
        val studentsCount                     =
          UserConstants.ID_FOLDER_USERS
            .queryChildren(UserConstants.ITEM_TYPE_USER)
            .addCondition(DataTypes.META_DATA_TYPE_ID, Comparison.in, ltiIntegratedQuery)
            .addCondition(DataTypes.META_DATA_TYPE_ID, Comparison.in, courseRoleQuery(studentRole))
            .getAggregateResult(Func.COUNT)
        val instructors                       =
          UserConstants.ID_FOLDER_USERS
            .queryChildren(UserConstants.ITEM_TYPE_USER)
            .addCondition(DataTypes.META_DATA_TYPE_ID, Comparison.in, ltiIntegratedQuery)
            .addCondition(DataTypes.META_DATA_TYPE_ID, Comparison.in, courseRoleQuery(instructorRole))
            .setOrder(UserConstants.DATA_TYPE_FAMILY_NAME, Direction.ASC)
            .setOrder(UserConstants.DATA_TYPE_GIVEN_NAME, Direction.ASC)
            .getComponents[UserComponent]
        val (instructorName, instructorEmail) = instructors.headOption.foldZ(i => i.getFullName -> i.getEmailAddress)
        val courseCreated                     = Option(course.getCreateTime).foldZ(dateFmt.format)
        val projectName                       = Option(course.getMasterCourse).foldZ(_.getName)
        csv.writeRow(
          course.getGroupId :: course.getName :: instructorName :: instructorEmail :: studentsCount :: courseCreated :: projectName :: Nil
        )
        instructors.drop(1) foreach { instructor =>
          csv.writeRow("" :: "" :: instructor.getFullName :: instructor.getEmailAddress :: "" :: "" :: Nil)
        }
      }
    }

    GeneratedReport(s"$title: $when", "See attached.", html = false, summaryCsv :: courseDetailCsv :: Nil)
  end generateReport
end LtiUsageStatisticsJobImpl

private[custom] final case class UnknownSystem(systemId: String) extends IllegalArgumentException(systemId)

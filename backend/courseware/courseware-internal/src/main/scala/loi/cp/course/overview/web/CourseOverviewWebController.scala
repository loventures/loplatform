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

package loi.cp.course
package overview
package web

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ArgoBody, ErrorResponse, Method, RedirectResponse}
import com.learningobjects.cpxp.component.{
  ComponentImplementation,
  ComponentInstance,
  ComponentInterface,
  ComponentService
}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.component.ComponentConstants
import com.learningobjects.cpxp.service.component.misc.CourseConstants
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.EnrollmentType as EnrolmentType
import com.learningobjects.cpxp.service.enrollment.{
  EnrollmentConstants as EnrolmentConstants,
  EnrollmentWebService as EnrolmentWebService
}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.group.{GroupConstants, GroupFolderFacade}
import com.learningobjects.cpxp.service.query.*
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.authorization.Secured
import loi.cp.analytics.CoursewareAnalyticsService
import loi.cp.config.ConfigurationService
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.enrollment.EnrollmentComponent as EnrolmentComponent
import org.apache.commons.lang3.time.DateUtils
import scalaz.\/
import scalaz.syntax.either.*
import scaloi.misc.InstantInstances.instantOrder
import scaloi.syntax.boolean.*
import scaloi.syntax.classTag.*
import scaloi.syntax.collection.*
import scaloi.syntax.option.*

import java.util as ju
import scala.jdk.CollectionConverters.*

/** A web controller for displaying a user's course overview.
  */
@Component
@Controller(value = "course-overview", root = true)
class CourseOverviewWebController(val componentInstance: ComponentInstance)(implicit
  configurationService: ConfigurationService,
  coursewareAnalyticsService: CoursewareAnalyticsService,
  enrolmentWebService: EnrolmentWebService,
  cs: ComponentService,
  facadeService: FacadeService,
  overviewService: CourseOverviewService,
  queryService: QueryService,
  user: UserDTO,
) extends ApiRootComponent
    with ComponentImplementation:
  import BaseCondition.getInstance as cond
  import Comparison.{eq as eql, ne as neq, *}
  import ComponentConstants.*
  import CourseConstants.*
  import CourseOverviewWebController.*
  import EnrolmentConstants.*
  import GroupConstants.*

  @Secured // only exposes data on the courses you're actually enrolled in
  @RequestMapping(path = "courseOverview", method = Method.GET)
  def courseOverview(aq0: ApiQuery): ArgoBody[ApiQueryResults[EnrolledCourseOverview]] =
    val courseQuery = queryService
      .queryRoot(ITEM_TYPE_GROUP)
      .addCondition(DATA_TYPE_COMPONENT_IDENTIFIER, eql, LightweightCourse.Identifier)

    Option(ID_FOLDER_PREVIEW_SECTIONS.facade[GroupFolderFacade]) foreach { folder =>
      courseQuery.addCondition(DataTypes.META_DATA_TYPE_PARENT_ID, neq, folder)
    }

    def enrolmentQuery = enrolmentWebService
      .getUserEnrollmentsQuery(user.id, EnrolmentType.ACTIVE_ONLY)

    val totalEnrolments = enrolmentQuery
      .addJoinQuery(DATA_TYPE_ENROLLMENT_GROUP, courseQuery)
      .count()

    // mutatious courseQuery works differently from here on out

    val aq = aq0
      .withPropertyMappings[CourseComponent]
      .withPropertyMapping("createTime", DataTypes.DATA_TYPE_CREATE_TIME)
      .withoutFilter("includeShutdownCourses")

    if !aq0.includeShutdownCourses then courseQuery.addDisjunction(excludeShutdownCoursesConditions)

    ApiQuerySupport.getQueryBuilder(courseQuery, aq)

    val enrolments = enrolmentQuery
      .addJoinQuery(DATA_TYPE_ENROLLMENT_GROUP, courseQuery)
      .getComponents[EnrolmentComponent]
      .toList

    // hopefully cache 'em all
    enrolments.map(_.getContextId).components[CourseComponent]

    val instructorRoleIds = overviewService.instructorRoleIds
    val studentRoleIds    = overviewService.studentRoleIds

    val (instructorCourses, studentCourses) = enrolments.partitionCollect {
      case ie if instructorRoleIds contains ie.getRoleId =>
        ie.getContextId.component[CourseComponent].left[CourseComponent]
      case se if studentRoleIds contains se.getRoleId    =>
        se.getContextId.component[CourseComponent].right[CourseComponent]
    }

    val progresses    = overviewService.progresses(user, studentCourses)
    val nextUp        = overviewService.nextUps(user, studentCourses)
    val studentCounts = overviewService.studentCounts(instructorCourses)

    def enrolmentToOverview(enrolment: EnrolmentComponent): EnrolledCourseOverview =
      val course           = enrolment.getContextId.component[CourseComponent]
      val isInstructor     = instructorRoleIds contains enrolment.getRoleId
      val enrolmentStart   = Option(enrolment.getStartTime).map(_.toInstant)
      val enrolmentStop    = Option(enrolment.getStopTime).map(_.toInstant)
      val enrolmentCreated = Option(enrolment.getCreatedOn)
      val courseStart      = course.getStartDate
      val courseEnd        = course.getEndDate
      val effectiveStart   = enrolmentStart.orElse(enrolmentCreated).max(courseStart)
      val effectiveStop    = enrolmentStop.min(courseEnd)
      val project          = course.branch.requireProject
      val projectName      =
        project.code.filterNot(project.name.startsWith).fold(project.name)(code => s"$code: ${project.name}")
      EnrolledCourseOverview(
        course = EnrolledCourseOverview.Course(
          id = course.getId,
          courseName = course.getName,
          courseGuid = course.getGroupId,
          projectName = projectName,
          url = course.getUrl,
          startDate = courseStart,
          endDate = courseEnd,
          shutdownDate = course.getShutdownDate,
          configuredShutdownDate = course.getConfiguredShutdownDate
        ),
        roleId = enrolment.getRole.getRoleId,
        startTime = effectiveStart,
        stopTime = effectiveStop,
        overallProgress = !isInstructor ?-? progresses(course.getId),
        nextUp = !isInstructor ?-? nextUp(course.getId),
        enrolledStudents = isInstructor ?-? studentCounts(course.getId),
        overallGrade = None
      )
    end enrolmentToOverview

    val deduplicatedEnrolments = enrolments
      .groupBy(_.getContextId)
      .valuesIterator
      .map(_.max(using RoleBasedEnrolmentDeduplication)) // meh
      .map(enrolmentToOverview)
      .toSeq
      .sortBy(_.startTime)
      .reverse

    ArgoBody {
      new ApiQueryResults(deduplicatedEnrolments.asJava, totalEnrolments, totalEnrolments)
    }
  end courseOverview

  def excludeShutdownCoursesConditions: ju.List[ju.List[Condition]] =
    val offset                        = CoursePreferences.getDomain.reviewPeriodOffset
    // Make the offset negative so that can compare the db end date correctly.
    // Usually we would add the offset to the end date to get the shutdown date,
    // but it is easier to subtract from current time for database queries.
    val currentTime                   = Current.getTime
    val currentTimeWithShutdownOffset =
      DateUtils.addHours(currentTime, -Math.toIntExact(offset))

    import GroupConstants.*
    List(
      List(cond(DATA_TYPE_END_DATE, eql, null)),
      List(cond(DATA_TYPE_COURSE_SHUTDOWN_DATE, gt, currentTime)),
      List(
        cond(DATA_TYPE_COURSE_SHUTDOWN_DATE, eql, null),
        cond(DATA_TYPE_END_DATE, gt, currentTimeWithShutdownOffset),
      ),
    ).map(_.asJava).asJava
  end excludeShutdownCoursesConditions

  // So that the domain app can emit a SectionEntryEvent1, which is otherwise only emitted
  // via LTI launch, before navigating to the group. It is far too much work to make
  // NameServlet know that a request for an Item is a section _entry_, especially for a
  // something that may only rarely be used by an LO Venturer.
  @Secured // it checks enrollment
  @RequestMapping(path = "courseOverview/section/{sectionId}", method = Method.GET)
  def enterSection(
    @PathVariable("sectionId") sectionId: Long,
    @QueryParam(value = "continue", required = false) continue: Option[String]
  ): ErrorResponse \/ RedirectResponse =

    val enrolments =
      enrolmentWebService
        .getUserEnrollments(user.id, sectionId, EnrolmentType.ACTIVE_ONLY)
        .asScala
        .components[EnrolmentComponent]

    for _ <- enrolments.isEmpty.thenLeft(ErrorResponse.unprocessable)
    yield
      val role    = enrolments.max(using RoleBasedEnrolmentDeduplication).getRole.getRoleId
      val section = sectionId.component[CourseComponent]
      coursewareAnalyticsService.emitSectionEntryEvent(sectionId, role, None)

      // we can enter the home page of the section or go directly to a path inside the section
      val location = continue match
        case None    => section.getUrl
        case Some(c) => s"${section.getUrl}/#/$c"

      RedirectResponse.temporary(location)
    end for
  end enterSection

  private implicit class AproposApiQueryProperties(aq: ApiQuery):
    // noinspection MapGetOrElseBoolean
    def includeShutdownCourses: Boolean =
      aq.getFilters.asScala
        .find(_.getProperty == "includeShutdownCourses")
        .filter(_.getOperator == PredicateOperator.EQUALS)
        .map(f => f.getValue.toBoolean)
        .getOrElse(true) // only don't include if specifically requested not to

    def withPropertyMapping(property: String, dataType: String) =
      new ApiQuery.Builder(aq).addPropertyMapping(property, dataType).build()

    def withPropertyMappings[C <: ComponentInterface: reflect.ClassTag] =
      new ApiQuery.Builder(aq).addPropertyMappings(classTagClass[C]).build()

    def withoutFilter(property: String) =
      new ApiQuery.Builder(aq)
        .setFilters {
          aq.getFilters.asScala.filterNot(_.getProperty == property).asJava
        }
        .build()
  end AproposApiQueryProperties
end CourseOverviewWebController

object CourseOverviewWebController:

  object RoleBasedEnrolmentDeduplication extends Ordering[EnrolmentComponent]:
    import EnrolmentWebService.*
    val priority = List( // low to high
      TRIAL_LEARNER_ROLE_ID,
      STUDENT_ROLE_ID,
      ADVISOR_ROLE_ID,
      INSTRUCTOR_ROLE_ID,
    )

    def compare(x: EnrolmentComponent, y: EnrolmentComponent) =
      priority.indexOf(x.getRole.getRoleId) - priority.indexOf(y.getRole.getRoleId)
  end RoleBasedEnrolmentDeduplication
end CourseOverviewWebController

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

package loi.cp.lti

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.group.GroupConstants
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType
import com.learningobjects.cpxp.service.user.UserDTO
import jakarta.servlet.http.HttpServletRequest
import loi.cp.analytics.CoursewareAnalyticsService
import loi.cp.course.lightweight.{LightweightCourse, LightweightCourseService}
import loi.cp.course.{CourseComponent, CourseConfigurationService, CourseFolderFacade, CoursePreferences}
import loi.cp.gatedate.GateDateSchedulingService
import loi.cp.integration.{BasicLtiSystemComponent, IntegrationService}
import loi.cp.subtenant.Subtenant
import loi.cp.user.UserComponent
import scalaz.\/
import scalaz.std.list.*
import scalaz.std.option.*
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.syntax.`try`.*
import scaloi.syntax.boolean.*
import scaloi.syntax.cobind.*
import scaloi.syntax.collection.*
import scaloi.syntax.option.*
import scaloi.{GetOrCreate, Gotten}

import java.time.Instant
import scala.compat.java8.OptionConverters.*
import scala.util.{Failure, Success}

/** Responsible for locating, provisioning and updating LTI launch course sections.
  */
@Service
final class LtiCourseService(
  coursewareAnalyticsService: CoursewareAnalyticsService,
  courseConfigurationService: CourseConfigurationService,
  integrationService: IntegrationService,
  lightweightCourseService: LightweightCourseService,
  userDto: => UserDTO,
  gateDateService: GateDateSchedulingService,
  enrollmentWebService: EnrollmentWebService,
)(implicit
  componentService: ComponentService,
  facadeService: FacadeService,
  mapper: ObjectMapper,
):
  import GroupConstants.{ID_FOLDER_COURSES as SectionFolder, ID_FOLDER_COURSE_OFFERINGS as OfferingFolder}
  import LtiCourseService.*
  import jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN as NOPE

  /** Launch into an existing course section. */
  def findSection(folderId: String, externalId: String)(implicit
    request: HttpServletRequest,
    system: BasicLtiSystemComponent
  ): LtiError \/ GetOrCreate[LightweightCourse] =
    for
      course <- findGroup(folderId, externalId) \/> GenericLtiError("lti_unknown_section", externalId).widen
      _      <- course.getDisabled \/>! FriendlyLtiError("lti_section_suspended", externalId, NOPE).widen
      lwc    <- course.component_![LightweightCourse] \/>| GenericLtiError("lti_invalid_section", externalId).widen
    yield Gotten(lwc)

  /** Find a prior section that a learner may have transferred from. */
  def findTransferSection(contextId: String, user: UserComponent)(implicit
    request: HttpServletRequest,
    system: BasicLtiSystemComponent
  ): Option[LightweightCourse] = findSection(contextId)
    .filter(section => enrollmentWebService.isCurrentOrFormerMember(user.id, section.id))
    .flatMap(_.component_?[LightweightCourse])

  /** Prepare the target course given an offering id. */
  def provisionContext(offeringId: String, subtenantOpt: Option[Subtenant])(implicit
    request: HttpServletRequest,
    system: BasicLtiSystemComponent
  ): LtiError \/ GetOrCreate[LightweightCourse] =
    for
      offering        <- loadOffering(offeringId)
      contextId       <- ltiParam_!(ContextIdParameter)
      contextHistory  <- ltiParam(CustomContextIdHistoryParameter)
      contextLabelOpt <- ltiParam(ContextLabelParameter)
      contextTitleOpt <- ltiParam(ContextTitleParameter)
      startDateOpt    <- ltiParamT[Instant](CustomSectionStartDateParameter)
      endDateOpt      <- ltiParamT[Instant](CustomSectionEndDateParameter)
      reviewDateOpt   <- ltiParamT[Instant](CustomReviewEndDateParameter)
      _               <- (reviewDateOpt.isDefined && endDateOpt.isEmpty) \/>! GenericLtiError("lti_review_without_end").widen
      init             = sectionInit(contextId, contextTitleOpt, contextLabelOpt, offering)
      courseGoc       <- getOrCreateCourse(contextId, offering.id, init).traverse(validateCourse(offering, _))
      course           = courseGoc.result
      historyOpt       = contextHistory.flatMap(_.split(HistorySeparator).toSeq.findMap(findSection))
      origin          <- historyOpt.traverse(validateCourse(offering, _))
      _               <- course.getDisabled \/>! FriendlyLtiError("lti_section_suspended", contextId, NOPE).widen
    yield
      if system.getUseExternalIdentifier then course.setExternalId(contextId.jome)
      contextLabelOpt foreach course.setGroupId
      contextTitleOpt foreach course.setName
      val startDateChange    = startDateOpt.exists(start => !course.getStartDate.contains(start))
      val dateChange         = startDateChange || endDateOpt.exists(end => !course.getEndDate.contains(end))
      startDateOpt `coflatForeach` course.setStartDate
      endDateOpt `coflatForeach` course.setEndDate
      reviewDateOpt `coflatForeach` course.setShutdownDate
      val changedIntegration = integrationService.integrate(course, system, contextId)
      subtenantOpt foreach { subtenant =>
        course.setSubtenant(subtenant.getId)
      }
      if courseGoc.isCreated then
        lightweightCourseService.initializeSection(course, (origin | offering).some)
        system.getBasicLtiConfiguration.preferences.filter(_.nonEmpty) foreach { prefs =>
          val json = mapper.valueToTree[ObjectNode](prefs)
          courseConfigurationService.patchGroupConfig(CoursePreferences, course, json)
        }
      else if dateChange || changedIntegration.isDefined then
        coursewareAnalyticsService.emitSectionUpdateEvent(course)
        if startDateChange then
          lightweightCourseService.updateSection(course)
          gateDateService.scheduleGateDateEvents(course)
      end if

      courseGoc

  def getMostRecentContext(implicit
    request: HttpServletRequest,
    system: BasicLtiSystemComponent,
    user: UserComponent,
  ): LtiError \/ Option[LightweightCourse] =
    for
      contextId       <- ltiParam_!(ContextIdParameter)
      transferHistory <- ltiParam(LtiEnrolmentService.CustomTransferHistoryParameter)
      contextIds       = contextId :: transferHistory.foldZ(_.split(',').toList)
      course           = contextIds.findMap(findTransferSection(_, user))
    yield course

  /** Load the offering. */
  private def loadOffering(groupId: String): LtiError \/ LightweightCourse =
    for
      course <- findGroup(OfferingFolder, groupId) \/> GenericLtiError("lti_invalid_offering", groupId).widen
      lwc    <- course.component_?[LightweightCourse] \/> GenericLtiError("lti_invalid_offering", groupId).widen
      _      <- (lwc.getDisabled || lwc.isArchived) \/>! GenericLtiError("lti_offering_suspended", groupId).widen
    yield lwc

  /** Create section initialization data. */
  private def sectionInit(
    contextId: String,
    name: Option[String],
    groupId: Option[String],
    offering: LightweightCourse
  ): CourseComponent.Init =
    new CourseComponent.Init(
      name | offering.course.data.title,
      groupId | contextId,
      GroupType.CourseSection,
      null, // no longer record student as creator
      offering.left
    )

  /** Get or create a course section. */
  private def getOrCreateCourse(contextId: String, offering: Long, init: CourseComponent.Init)(implicit
    system: BasicLtiSystemComponent
  ): GetOrCreate[CourseComponent] =
    if system.getUseExternalIdentifier then
      if system.getBasicLtiConfiguration.sectionPerOffering.isTrue then
        sectionFolder.getOrCreateCourseByOfferingAndExternalId(offering, contextId, LightweightCourse.Identifier, init)
      else sectionFolder.getOrCreateCourseByExternalId(contextId, LightweightCourse.Identifier, init)
    else if system.getBasicLtiConfiguration.sectionPerOffering.isTrue then
      sectionFolder.getOrCreateCourseByOfferingAndSubquery(
        offering,
        integrationService.queryIntegrated(system, contextId),
        LightweightCourse.Identifier,
        init
      )
    else
      sectionFolder.getOrCreateCourseBySubquery(
        integrationService.queryIntegrated(system, contextId),
        LightweightCourse.Identifier,
        init
      )

  /** Find a course section. */
  private def findSection(
    contextId: String
  )(implicit system: BasicLtiSystemComponent): Option[CourseComponent] =
    if system.getUseExternalIdentifier then sectionFolder.findCourseByExternalId(contextId).asScala
    else sectionFolder.findCourseBySubquery(integrationService.queryIntegrated(system, contextId)).asScala

  /** Validate a course section against an expected offering. */
  private def validateCourse(
    offering: LightweightCourse,
    course: CourseComponent
  ): LtiError \/ LightweightCourse =
    course.component_![LightweightCourse] match
      case Success(c) =>
        (c.getOffering.id == offering.id) either c or MismatchedOffering(c.getOffering.getGroupId, offering.getGroupId)
      case Failure(_) =>
        GenericLtiError("lti_mismatched_course_content", offering.getGroupId).left

  /** Find a group by external id. */
  private def findGroup(folderId: String, identifier: String): Option[CourseComponent] =
    if folderId == OfferingFolder then folderId.facade[CourseFolderFacade].findCourseByGroupId(identifier).asScala
    else folderId.facade[CourseFolderFacade].findCourseByExternalId(identifier).asScala

  /** The section folder. */
  private def sectionFolder: CourseFolderFacade =
    SectionFolder.facade[CourseFolderFacade]
end LtiCourseService

object LtiCourseService:
  private final val ContextIdParameter    = "context_id"
  private final val ContextLabelParameter = "context_label"
  private final val ContextTitleParameter = "context_title"

  private final val CustomContextIdHistoryParameter = "custom_context_id_history"
  private final val CustomSectionStartDateParameter = "custom_section_start_date"
  private final val CustomSectionEndDateParameter   = "custom_section_end_date"
  private final val CustomReviewEndDateParameter    = "custom_review_end_date"

  private final val HistorySeparator = ","
end LtiCourseService

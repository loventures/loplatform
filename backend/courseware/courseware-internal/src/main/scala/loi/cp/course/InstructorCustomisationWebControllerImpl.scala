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

import argonaut.Argonaut.*
import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.ErrorResponseOps.*
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.component.web.{ArgoBody, ErrorResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.json.OptionalField
import loi.cp.assessment.attempt.AssessmentAttemptService
import loi.cp.assessment.{AssessmentService, CourseAssessmentPolicy}
import loi.cp.config.ConfigurationService
import loi.cp.content.CourseWebUtils
import loi.cp.context.ContextId
import loi.cp.course.InstructorCustomisationWebController.{
  DueDateAccommodationDto,
  DueDateAccommodationRequestDto,
  DueDatePolicyDto,
  ExemptLearnerDto
}
import loi.cp.course.lightweight.LightweightCourseService
import loi.cp.customisation.{ContentOverlay, ContentOverlayUpdate, CourseCustomisationService}
import loi.cp.duedate.StoragedDueDateExemptions
import loi.cp.policies.CourseAssessmentPoliciesService
import loi.cp.progress.LightweightProgressService
import loi.cp.reference.EdgePath
import loi.cp.storage.CourseStorageService
import loi.cp.user.LightweightUserService
import scalaz.\/
import scalaz.syntax.std.boolean.*

import java.math.BigDecimal as BD

@Component
class InstructorCustomisationWebControllerImpl(val componentInstance: ComponentInstance)(
  courseWebUtils: CourseWebUtils,
  configurationService: ConfigurationService,
  courseAssessmentPoliciesService: CourseAssessmentPoliciesService,
  customisationService: CourseCustomisationService,
  assessmentService: AssessmentService,
  assessmentAttemptService: AssessmentAttemptService,
  lwcService: LightweightCourseService,
  progressService: LightweightProgressService,
  userService: LightweightUserService,
  storageService: CourseStorageService,
  courseWorkspaceService: CourseWorkspaceService,
) extends InstructorCustomisationWebController
    with ComponentImplementation:

  override def setPointsPossible(
    contextId: Long,
    path: EdgePath,
    pointConfig: ArgoBody[InstructorCustomisationWebController.PointsPossibleDto]
  ): ErrorResponse \/ ArgoBody[ContentOverlay] =
    for
      cfg                <- pointConfig.decodeOrMessage.leftMap(_.to422)
      (section, content) <- courseWebUtils
                              .loadCourseSectionContents(contextId, path)
                              .leftMap(_.to404)
      ws                  = courseWorkspaceService.loadReadWorkspace(section)
      policies            = courseAssessmentPoliciesService.getPolicies(section)
      assessments         = assessmentService.getAssessments(section, Seq(content), policies, ws)
      validAttempts       = assessmentAttemptService.countValidAttempts(section, assessments)
      _                  <- validateNoValidAttempts(validAttempts).leftMap(_.to422)
    yield
      val customisation = customisationService.updateCustomisation(
        section.lwc,
        path,
        CourseCustomisationService.UpdateOverlay(
          ContentOverlayUpdate(pointsPossible = OptionalField(Some(BD.valueOf(cfg.pointsPossible))))
        )
      )
      lwcService.incrementGeneration(section.lwc)
      progressService.scheduleProgressUpdate(section.id)
      ArgoBody(customisation.overlays(path))

  private def validateNoValidAttempts(validAttempts: Int): String \/ Unit =
    (validAttempts == 0) either (()) or "Cannot update points with existing valid attempts"

  override def updateDueDatePolicy(
    contextId: ContextId,
    preferences: ArgoBody[DueDatePolicyDto]
  ): ErrorResponse \/ Unit =
    for request <- preferences.decodeOrMessage.leftMap(_.to400)
    yield
      val prefs = JacksonUtils.getMapper.valueToTree[JsonNode](request)
      configurationService.patchItem(CoursePreferences)(contextId, prefs)

  override def loadDueDateAccommodation(
    contextId: ContextId
  ): ArgoBody[DueDateAccommodationDto] =
    val exemptions: StoragedDueDateExemptions  = storageService.get[StoragedDueDateExemptions](contextId)
    val exemptLearners: List[ExemptLearnerDto] = userService
      .getUsersById(exemptions.value.toSeq)
      .map(user => ExemptLearnerDto(user.id, user.givenName, user.familyName))
      .toList
    ArgoBody(DueDateAccommodationDto(exemptLearners))

  override def updateDueDateAccommodation(
    contextId: ContextId,
    accommodations: ArgoBody[DueDateAccommodationRequestDto]
  ): ErrorResponse \/ Unit =
    for request <- accommodations.decodeOrMessage.leftMap(_.to400)
    yield storageService
      .modify[StoragedDueDateExemptions](contextId)(_ => StoragedDueDateExemptions(request.exemptLearners))
      .value

  override def updateCourseAssessmentPolicies(
    contextId: Long,
    policies: ArgoBody[List[CourseAssessmentPolicy]]
  ): ErrorResponse \/ Unit =
    for
      request <- policies.decodeOrMessage.leftMap(_.to422)
      section <- courseWebUtils.loadCourseSection(contextId).leftMap(_.to404)
    yield
      courseAssessmentPoliciesService.setPolicies(section, request)
      lwcService.incrementGeneration(section.lwc)

  override def getCourseAssessmentPolicies(
    contextId: Long
  ): ErrorResponse \/ List[CourseAssessmentPolicy] =
    for section <- courseWebUtils.loadCourseSection(contextId).leftMap(_.to404)
    yield loadAssessmentPolicies(section)

  private def loadAssessmentPolicies(section: CourseSection): List[CourseAssessmentPolicy] =
    val policies = courseAssessmentPoliciesService.getPolicies(section)
    if policies.nonEmpty then policies
    else
      // Note: We're using defaults for now as there is pending work to push
      // this concept down to the authored content.  In the meantime, we will
      // return the defaults
      CourseAssessmentPolicy.defaults
end InstructorCustomisationWebControllerImpl

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

package loi.cp.section

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.ErrorResponseOps.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentDescriptor, ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType
import com.learningobjects.cpxp.service.group.GroupWebService
import com.learningobjects.cpxp.service.user.{UserDTO, UserType}
import com.learningobjects.de.authorization.Secured
import loi.cp.content.CourseWebUtils
import loi.cp.content.gate.ContentGateOverrideService
import loi.cp.content.gate.GateOverrides.Changes
import loi.cp.course.CourseAccessService
import loi.cp.lwgrade.GradeService
import loi.cp.mastery.MasteryService
import loi.cp.progress.LightweightProgressService
import loi.cp.quiz.attempt.QuizAttemptService
import loi.cp.storage.CourseStorageService
import loi.cp.submissionassessment.attempt.SubmissionAttemptService
import scalaz.\/
import scaloi.data.SetDelta
import scaloi.syntax.boolean.*

@Component
@Controller(root = true)
class ResetLearnerWebController(
  val componentInstance: ComponentInstance,
  courseAccessService: CourseAccessService,
  courseWebUtils: CourseWebUtils,
  gradeService: GradeService,
  progressService: LightweightProgressService,
  masteryService: MasteryService,
  quizAttemptService: QuizAttemptService,
  submissionAttemptService: SubmissionAttemptService,
  courseStorageService: CourseStorageService,
  contentGateOverrideService: ContentGateOverrideService,
  groupWebService: GroupWebService,
  user: UserDTO,
)(implicit
  cd: ComponentDescriptor,
) extends ApiRootComponent
    with ComponentImplementation:

  @Secured
  @RequestMapping(path = "lwc/{sectionId}/resetLearner", method = Method.POST)
  def resetLearner(
    @PathVariable("sectionId") sectionId: Long,
  ): ErrorResponse \/ Unit =
    for
      section  <- courseWebUtils.loadCourseSection(sectionId).leftMap(_.to404)
      _        <- courseAccessService.hasLearnerAccess(section) \/> ErrorResponse.forbidden
      groupType = groupWebService.getGroup(section.id).getGroupType
      _        <- (user.userType == UserType.Preview || groupType == GroupType.TestSection) \/> ErrorResponse.forbidden
    yield
      gradeService.deleteGradebook(section, user)
      progressService.deleteProgress(section, user)
      masteryService.deleteMastery(user, section)
      courseStorageService.reset(section, user)
      // learner transfer neglects gate overrides...
      val gateOverrides = contentGateOverrideService.loadOverride(user, section.lwc).get
      if gateOverrides.nonEmpty then
        contentGateOverrideService.updateOverrides(section.lwc)(
          Changes(perUser = Map(user.id -> SetDelta.remove(gateOverrides)))
        )
      val edgePaths     = section.contents.nonRootElements.map(_.edgePath)
      quizAttemptService.invalidateAttempts(section, edgePaths, user)
      submissionAttemptService.invalidateAttempts(section, edgePaths, user)
      // Not touching:
      // . discussion posts
      // . notifications, alerts
      // . q&a
      // . bookmarks
      // . other things I forget
end ResetLearnerWebController

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

package loi.cp.grade

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.ErrorResponseOps.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ArgoBody, ErrorResponse, Method}
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import com.learningobjects.de.authorization.{Secured, SecuredAdvice}
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.admin.right.CourseAdminRight
import loi.cp.content.CourseWebUtils
import loi.cp.course.right.EditCourseGradeRight
import loi.cp.course.{CourseEnrollmentService, CourseSection, CourseWorkspaceService}
import loi.cp.customisation.CourseCustomisationService
import loi.cp.lwgrade.*
import loi.cp.notification.NotificationService
import loi.cp.policies.CourseAssessmentPoliciesService
import loi.cp.progress.LightweightProgressService
import loi.cp.quiz.Quiz
import loi.cp.quiz.question.{LinearQuestionSet, Question, QuestionPool, QuestionService}
import loi.cp.reference.EdgePath
import loi.cp.user.web.UserWebUtils
import org.apache.commons.math3.util.Precision
import scalaz.\/
import scalaz.syntax.std.option.*
import scaloi.misc.TimeSource
import scaloi.syntax.boolean.*

// our integration test framework demands an interface exist, not any production code
@Controller(value = "GradeWebController", root = true)
trait GradeWebController extends ApiRootComponent:

  // for setting a grade manually via the gradebook or assignments UIs
  @RequestMapping(path = "lwgrade2/{sectionId}/gradebook/grades/grade", method = Method.POST)
  @Secured(Array(classOf[EditCourseGradeRight], classOf[CourseAdminRight]))
  def addGrade(
    @SecuredAdvice @PathVariable("sectionId") sectionId: Long,
    @RequestBody gradeOverride: ArgoBody[GradeOverrideDto],
  ): ErrorResponse \/ ArgoBody[GradeComponentDto]
end GradeWebController

@Component
class GradeWebControllerImpl(
  componentInstance: ComponentInstance,
  courseAssessmentPoliciesService: CourseAssessmentPoliciesService,
  courseCustomisationService: CourseCustomisationService,
  courseEnrollmentService: CourseEnrollmentService,
  courseWebUtils: CourseWebUtils,
  gradeService: GradeService,
  notificationService: NotificationService,
  progressService: LightweightProgressService,
  questionService: QuestionService,
  courseWorkspaceService: CourseWorkspaceService,
  ts: TimeSource,
  userDto: UserDTO,
  userWebUtils: UserWebUtils
) extends BaseComponent(componentInstance)
    with GradeWebController:

  def addGrade(
    sectionId: Long,
    gradeOverride: ArgoBody[GradeOverrideDto]
  ): ErrorResponse \/ ArgoBody[GradeComponentDto] =

    for
      section  <- courseWebUtils.loadCourseSection(sectionId, None).leftMap(_.to404)
      go       <- gradeOverride.decodeOrMessage.leftMap(_.to422)
      learner  <- userWebUtils.loadUser(go.studentId).leftMap(_.to422)
      _        <- checkLearnerEnrollment(section, learner).leftMap(_.to422)
      structure = GradeStructure(section.contents)
      content  <- section.contents
                    .get(go.columnId)
                    .toRightDisjunction(s"No such content: ${go.columnId}; course $sectionId")
                    .leftMap(_.to422)
      column   <- findColumn(section, structure, go.columnId).leftMap(_.to422)
    yield
      val grade = gradeService.setGradePercent(
        learner,
        section,
        content,
        structure,
        column,
        go.grade,
        ts.instant,
      )

      progressService
        .updateProgress(section, learner, gradeService.getGradebook(section, learner), Nil)
        .valueOr(err => throw new RuntimeException(err.msg))

      val init = GradeNotification.Init(column, go.studentId, section.id)
      notificationService.nοtify[GradeNotification](section.id, init)

      val awarded  = Grade.grade(grade)
      val possible = Grade.max(grade)
      val percent  = awarded.map(a => if possible <= Precision.EPSILON then 0.0 else a / possible)

      ArgoBody(
        GradeComponentDto.empty.copy(
          grade = awarded,
          max = possible,
          raw_grade = percent,
          column_id = go.columnId,
          info = GradeInfoDto.empty.copy(grade = awarded, submissionDate = ts.instant),
          user_id = UserId(go.studentId)
        )
      )

  private def loadQuestions(ws: => AttachedReadWorkspace)(quiz: Quiz): List[Question] =
    val customisation = courseCustomisationService.loadCustomisation(quiz.section.lwc)
    questionService.getQuestions(quiz, Set.empty, customisation, ws) match
      case lqs: LinearQuestionSet => lqs.questions.toList
      case qp: QuestionPool       => qp.candidateQuestions.toList

  private def checkLearnerEnrollment(section: CourseSection, learner: UserDTO): String \/ Unit =
    courseEnrollmentService
      .areAllStudentsEnrolled(section.id, Set(learner.id))
      .elseLeft(s"User ${learner.id} is not a learner in course ${section.id}")

  private def findColumn(
    section: CourseSection,
    structure: GradeStructure,
    edgePath: EdgePath
  ): String \/ GradeColumn =
    structure
      .findColumnForEdgePath(edgePath)
      .toRightDisjunction(s"No column for path $edgePath in course ${section.id}")
end GradeWebControllerImpl

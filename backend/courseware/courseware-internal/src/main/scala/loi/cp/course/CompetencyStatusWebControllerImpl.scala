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

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.*
import com.learningobjects.cpxp.component.web.ErrorResponseOps.*
import com.learningobjects.cpxp.component.web.{ArgoBody, ErrorResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import loi.cp.assessment.AssessmentService
import loi.cp.assessment.attempt.AttemptState.Finalized
import loi.cp.competency.CompetentCompetencyService
import loi.cp.content.CourseWebUtils
import loi.cp.context.ContextId
import loi.cp.mastery.MasteryService
import loi.cp.policies.CourseAssessmentPoliciesService
import loi.cp.quiz.Quiz
import loi.cp.quiz.attempt.QuizAttemptService
import loi.cp.reference.ContentIdentifier
import loi.cp.user.ImpersonationService
import loi.cp.user.web.UserWebUtils
import scalaz.syntax.std.option.*
import scalaz.{\/, \/-}

import java.lang as jl
import scala.jdk.CollectionConverters.*

@Component
class CompetencyStatusWebControllerImpl(val componentInstance: ComponentInstance)(
  courseWebUtils: CourseWebUtils,
  currentUser: => UserDTO,
  impersonationService: ImpersonationService,
  assessmentService: AssessmentService,
  quizAttemptService: QuizAttemptService,
  competentCompetencyService: CompetentCompetencyService,
  courseAssessmentPoliciesService: CourseAssessmentPoliciesService,
  userWebUtils: UserWebUtils,
  courseWorkspaceService: CourseWorkspaceService,
  masteryService: MasteryService,
) extends CompetencyStatusWebController
    with ComponentImplementation:
  import CompetencyStatusWebController.*

  /** This is only for quizzes, and just wants a breakdown of "mastery" by attempt. And, in a fit of jealous rage, the
    * front-end does all this and then only displays the breakdown for the latest attempt.
    */
  override def getCompetencyBreakdown(
    identifier: ContentIdentifier,
    viewAsId: Option[jl.Long],
  ): ErrorResponse \/ ArgoBody[ApiQueryResults[CompetencyBreakdownDto]] =
    val viewAs: UserId = viewAsId.cata(UserId(_), currentUser)
    for
      viewAs             <- loadUserDto(viewAsId)
      _                  <- impersonationService.checkImpersonation(identifier.contextId, viewAs).leftMap(_.to404)
      (section, content) <- courseWebUtils
                              .loadCourseSectionContents(identifier.contextId.value, identifier.edgePath)
                              .leftMap(_.to404)
      policies            = courseAssessmentPoliciesService.getPolicies(section)
      quiz               <- Quiz.fromContent(content, section, policies) \/> ErrorResponse.notFound
    yield
      val ws           = courseWorkspaceService.loadReadWorkspace(section)
      val competencies = competentCompetencyService.getCompetencyMap(ws)

      // The front end displays competency summary of the last by create date so I'm
      // just going to filter down to that.
      val attempts =
        quizAttemptService
          .getUserAttempts(section, Seq(quiz), viewAs)
          .filter(_.state == Finalized)
          .sortBy(_.createTime)
          .lastOption
          .toList

      val breakdownDtos = attempts map { attempt =>
        val mastered    = masteryService.computeMasteryForQuizAttempt(ws, attempt)
        val masteredIds = mastered.flatMap(name => competencies.get(name).map(_.id))
        CompetencyBreakdownDto(attempt.id, masteredIds)
      }

      ArgoBody(
        new ApiQueryResults[CompetencyBreakdownDto](
          breakdownDtos.asJava,
          breakdownDtos.size.toLong,
          breakdownDtos.size.toLong
        )
      )
    end for
  end getCompetencyBreakdown

  private def loadUserDto(viewAsId: Option[jl.Long]): ErrorResponse \/ UserDTO =
    viewAsId match
      case None     => \/-(currentUser)
      case Some(id) => userWebUtils.loadUser(id).leftMap(_.to422)

  override def getCompetenciesForContext(context: ContextId): ArgoBody[CompetencyStructureDto] =
    val (section, ws) = courseWebUtils.sectionWsOrThrow404(context.value)

    val competenciesTree = competentCompetencyService.getCompetencyForest(ws)
    val allCompetencies  = competenciesTree.flatMap(_.flatten)
    val allCompetencyIds = allCompetencies.map(competency => competency.nodeName -> competency.id).toMap

    val contentsByCompetency = competentCompetencyService.getContentsByCompetency(ws, section)

    val edgePathsByCompetency = allCompetencies.map(competency =>
      EdgePathsByCompetency(competency, contentsByCompetency.getOrElse(competency.nodeName, Set.empty))
    )

    ArgoBody(CompetencyStructureDto(competenciesTree.map(_.map(_.id)), edgePathsByCompetency))
  end getCompetenciesForContext
end CompetencyStatusWebControllerImpl

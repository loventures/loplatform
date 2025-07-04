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

package loi.cp.content

import cats.syntax.option.*
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.exception.InvalidRequestException
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.exception.AccessForbiddenException
import com.learningobjects.cpxp.service.user.{UserDTO, UserId}
import loi.cp.competency.{Competency, CompetentCompetencyService}
import loi.cp.content.gate.*
import loi.cp.context.ContextId
import loi.cp.course.{CourseAccessService, CourseConfigurationService, CourseWorkspaceService}
import loi.cp.duedate.StoragedDueDateExemptions
import loi.cp.gatedate.GateStartDate
import loi.cp.lwgrade.{GradeService, StudentGradebook}
import loi.cp.progress.LightweightProgressService
import loi.cp.reference.EdgePath
import loi.cp.storage.CourseStorageService
import loi.cp.survey.SurveySubmissionReceiptStorage.*
import loi.cp.user.ImpersonationService
import scalaz.std.list.*
import scalaz.syntax.functor.*
import scaloi.misc.TimeSource
import scaloi.syntax.collection.*
import scaloi.syntax.disjunction.*
import scaloi.syntax.option.*

import java.lang as jl
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Try

@Controller(value = "lwc-content", root = true)
trait CourseContentWebController extends ApiRootComponent:

  /** Loads a tree of content.
    *
    * The id of the children is the `path` of the parent, with the edge added at the end. The content is returned in
    * pre-order, which means that every node's parent has been visited before the node itself.
    *
    * Assume you have this:
    *
    * @startuml
    *   (root) --> (m1) : a\n(0) (root) --> (m2) : b\n(1) (m1) --> (l1) : d\n(0) (m1) --> (l2) : e\n(1) (m2) --> (l3) :
    *   f\n(0) (m2) --> (l1) : g\n(1) (l1) --> (r1) : i\n(0) (l2) --> (r2) : j\n(0)
    *
    * usecase root as "root\ncourse.1" usecase m1 as "m1\nmodule.1" usecase m2 as "m2\nmodule.1" usecase l1 as
    * "l1\nlesson.1" usecase l2 as "l2\nlesson.1" usecase l3 as "l3\nlesson.1" usecase r1 as "r1\nhtml.1" usecase r2 as
    * "r2\nhtml.1"
    * @enduml
    *
    * then, you get this (for the following paths)
    *
    * "": root[""], m1["a"], m2["b"], m3["c"] "a": m1["a"], l1["a.d"], l2["a.e"] "a.e": l2["a.e"], r2["a.e.j"] "b":
    * m2["b"], l3["b.f"], l1["b.g"]
    */
  @RequestMapping(path = "lwc/{context}/contents", method = Method.GET)
  def getCourseContents(
    @PathVariable("context") context: Long,
    @QueryParam(value = "user", required = false) targetUser: Option[jl.Long],
  ): Try[Seq[CourseContentDto]]
end CourseContentWebController

@Component
class CourseContentWebControllerImpl(val componentInstance: ComponentInstance)(
  competentCompetencyService: CompetentCompetencyService,
  contentDtoUtils: ContentDtoUtils,
  courseAccessService: CourseAccessService,
  courseConfigService: CourseConfigurationService,
  storageService: CourseStorageService,
  courseWebUtils: CourseWebUtils,
  courseWorkspaceService: CourseWorkspaceService,
  gateCustomisationService: ContentGateOverrideService,
  gradeService: GradeService,
  performanceRuleService: PerformanceRuleStructureService,
  progressService: LightweightProgressService,
  enrollmentWebService: EnrollmentWebService,
  impersonationService: ImpersonationService,
  now: TimeSource,
  currentUser: UserDTO,
  domain: => DomainDTO,
) extends CourseContentWebController
    with ComponentImplementation:

  override def getCourseContents(
    context: Long,
    targetUser: Option[jl.Long],
  ): Try[Seq[CourseContentDto]] =

    val contextId                                     = ContextId(context)
    val user                                          = targetUser.fold(currentUser.userId)(id => UserId(id))
    val dueDateExemptUsers: StoragedDueDateExemptions = storageService.get[StoragedDueDateExemptions](contextId)

    for
      rights        <- courseAccessService.actualRights(contextId, user) <@~* new AccessForbiddenException("No access")
      section       <-
        courseWebUtils.loadCourseSection(context, rights.some).toTry(msg => new InvalidRequestException(msg))
      _             <- impersonationService.checkImpersonation(section.lwc, user).toTry
      overrides     <- gateCustomisationService.loadOverrides(section.lwc)
      ws             = courseWorkspaceService.loadReadWorkspace(section)
      perfStructure <- performanceRuleService.computePerformanceRuleStructure(ws, section.lwc)
    yield
      val rawGradebook        = gradeService.getGradebook(section, user)
      val gradebook           =
        StudentGradebook.applyRollupGradeViewConfig(courseConfigService, section.contents, section.lwc, rawGradebook)
      val progressMap         = progressService.loadProgress(section, user, rawGradebook)
      val policyStatus        = PolicyRule.evaluatePolicyRules(section.contents, rights)
      val isUserDueDateExempt = rights.likeInstructor || dueDateExemptUsers.value.contains(user.id)

      val allCompetencies    = competentCompetencyService.getCompetencyMap(ws)
      val competencyContents = competentCompetencyService.getContentsByCompetency(ws, section)

      val allCompetenciesByEdgePath: Map[EdgePath, Seq[Competency]] =
        competencyContents.toList
          .flatMap({ case (competency, edgePaths) =>
            allCompetencies.get(competency).toList.flatMap(edgePaths.toList.strengthR)
          })
          .groupToMap

      // 1: get contents and parents of contents
      val contentList = section.contents.tree.flatten
      val parentMap   = TreeStructure.getParentStructure(section.contents.tree)

      // 2. collect gate information
      // This need availability relative to the user, not the course in general, so calculate it out
      val enrollments                          = enrollmentWebService.getUserEnrollments(user.id, section.id).asScala.toList
      val gateStartDate: Option[GateStartDate] =
        GateStartDate.forUser(section.lwc, enrollments, rights.viewAllContent, domain.timeZoneId)
      val userDates: ContentDates              =
        gateStartDate
          .flatMap(_.value)
          .map(value => ContentDateUtils.contentDates(section.contents.tree, value))
          .getOrElse(ContentDates.empty)

      // 3. evaluate gates and calculate total gate status
      val gateData             = GateSummary.calculateGateData(
        contentList,
        userDates.gateDates,
        perfStructure,
        policyStatus,
        now.instant,
        gradebook
      )
      val initialGateSummaries = GateSummary.collectGateSummary(gateData, rights.viewAllContent, overrides(user))

      // 4. gate the children of gated content
      val gateSummaries = {
        import scalaz.syntax.order.*
        val summaries = mutable.HashMap.empty[EdgePath, GateSummary] ++ initialGateSummaries

        // minor mutable crimes to accomplish this, forgive me
        section.contents.tree.toTree.scanr[CourseContent] { (content, children) =>
          children.toList.map(_.rootLabel).foreach { child =>
            val summary      = summaries(content.edgePath)
            val childSummary = summaries(child.edgePath)
            summaries(child.edgePath) = childSummary
              .copy(status = childSummary.status max summary.status)
          }
          content
        }
        summaries
      }.toMap

      val surveySubmissionReceipts = storageService.get[SurveySubmissionReceipts](section, currentUser)

      val categoryMap = section.contents.categories.groupUniqBy(_.asset.info.name)
      val edgePaths   = contentList.map(c => c.asset.info.name -> c.edgePath).toMap

      contentList.map { content =>
        val competencies: Seq[Competency] = allCompetenciesByEdgePath.getOrElse(content.edgePath, Nil)

        val (index, parents) = parentMap(content.edgePath)
        val gateSummary      = gateSummaries(content.edgePath)
        val grade            = gradebook.grades.get(content.edgePath)
        val dueDate          = userDates.dueDates.get(content.edgePath)
        val dueDateExempt    = dueDate.map(_ =>
          isUserDueDateExempt
        ) // an option, will be Some(true) if there is a due date, and we are exempt
        val progress = Option(progressMap.map.get(content.edgePath))
        // horridly hide submitted surveys
        val survey   = content.survey.unless(surveySubmissionReceipts.get(content.edgePath).exists(_.nonEmpty))

        contentDtoUtils.toDto(
          content,
          section.lwc,
          index,
          parents,
          gateSummary,
          dueDate,
          dueDateExempt,
          progress,
          grade,
          competencies,
          survey.isDefined,
          categoryMap,
          edgePaths,
        )
      }
    end for
  end getCourseContents
end CourseContentWebControllerImpl

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

package loi.cp
package course

import argonaut.Argonaut.*
import argonaut.*
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.ApiQueryResults
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ArgoBody, ErrorResponse, Method}
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import com.learningobjects.de.authorization.{Secured, SecuredAdvice}
import loi.cp.admin.right.CourseAdminRight
import loi.cp.assessment.AttemptId
import loi.cp.competency.Competency
import loi.cp.context.ContextId
import loi.cp.course.right.{LearnCourseRight, TeachCourseRight}
import loi.cp.reference.{ContentIdentifier, EdgePath}
import scalaz.\/
import scaloi.data.ListTree
import scaloi.json.ArgoExtras
import scaloi.json.ArgoExtras.*

@Controller(root = true, value = "competencyStatus")
@RequestMapping(path = "competencyStatus")
trait CompetencyStatusWebController extends ApiRootComponent:
  import CompetencyStatusWebController.*

  @RequestMapping(path = "{identifier}", method = Method.GET)
  @Secured(Array(classOf[CourseAdminRight], classOf[TeachCourseRight], classOf[LearnCourseRight]))
  def getCompetencyBreakdown(
    @SecuredAdvice @PathVariable("identifier") identifier: ContentIdentifier,
    @QueryParam(value = "viewAs", required = false) viewAs: Option[JLong]
  ): ErrorResponse \/ ArgoBody[ApiQueryResults[CompetencyBreakdownDto]]

  @RequestMapping(path = "competencies", method = Method.GET)
  @Secured(Array(classOf[CourseAdminRight], classOf[TeachCourseRight], classOf[LearnCourseRight]))
  def getCompetenciesForContext(
    @SecuredAdvice @MatrixParam("context") context: ContextId
  ): ArgoBody[CompetencyStructureDto]
end CompetencyStatusWebController

object CompetencyStatusWebController:
  type CompetencyId = Long

  case class CompetencyStructureDto(
    competencyStructure: List[ListTree[Long]],
    competencies: List[EdgePathsByCompetency]
  )
  implicit val competencyStructureDtoCodec: CodecJson[CompetencyStructureDto] =
    casecodec2(CompetencyStructureDto.apply, ArgoExtras.unapply)("competencyStructure", "competencies")

  case class EdgePathsByCompetency(competency: Competency, relations: Set[EdgePath])
  implicit def competencyDtoCodec: CodecJson[EdgePathsByCompetency] =
    casecodec2(EdgePathsByCompetency.apply, ArgoExtras.unapply)("competency", "relations")

  case class CompetencyBreakdownDto(attemptId: AttemptId, mastered: Set[CompetencyId])
  implicit def competencyBreakdownCodec: CodecJson[CompetencyBreakdownDto] =
    casecodec2(CompetencyBreakdownDto.apply, ArgoExtras.unapply)("attemptId", "mastered")
end CompetencyStatusWebController

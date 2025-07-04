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

package loi.cp.mastery

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.ErrorResponseOps.*
import com.learningobjects.cpxp.component.web.{ArgoBody, ErrorResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.competency.CompetentCompetencyService
import loi.cp.content.CourseWebUtils
import loi.cp.context.ContextId
import loi.cp.mastery.LightweightCourseMasteryWebController.*
import loi.cp.reference.ContentIdentifier
import loi.cp.user.{ImpersonationService, LightweightUserService}
import scalaz.\/
import scalaz.std.option.*
import scalaz.syntax.functor.*
import scalaz.syntax.std.option.*

@Component
class LightweightCourseMasteryWebControllerImpl(val componentInstance: ComponentInstance)(
  courseWebUtils: CourseWebUtils,
  impersonationService: ImpersonationService,
  competentCompetencyService: CompetentCompetencyService,
  recomputeMasteryService: RecomputeMasteryService,
  lightweightUserService: LightweightUserService,
  currentUser: => UserDTO,
) extends LightweightCourseMasteryWebController
    with ComponentImplementation:
  override def getLearnerMastery(
    context: ContextId,
    viewAsId: Option[JLong]
  ): ErrorResponse \/ ArgoBody[CourseMasteryDTO] =
    val viewAs        = viewAsId.cata(lightweightUserService.getUserById(_).get, currentUser)
    val (section, ws) = courseWebUtils.sectionWsOrThrow404(context.id)

    for _ <- impersonationService.checkImpersonation(context, viewAs).leftMap(_.to404)
    yield
      // The weirdness here is to exclude competencies that a) aren't used by the content
      // and then b) aren't actually in the course competency set anymore
      val allCompetencies    = competentCompetencyService.getCompetencyForest(ws).flatMap(_.flatten)
      val competencyContents = competentCompetencyService.getContentsByCompetency(ws, section)
      val usedCompetencies   = allCompetencies.filter(competency => competencyContents.contains(competency.nodeName))
      val usedCompetencyIds  = usedCompetencies.map(competency => competency.nodeName -> competency.id).toMap

      val mastery               = recomputeMasteryService.getRecomputedMasteredCompetencies(ws, viewAs, section)
      val masteredCompetencyIds = mastery.flatMap(usedCompetencyIds.get)

      val contentsByCompetency = competencyContents.flatMap({ case (name, edgePaths) =>
        usedCompetencyIds.get(name).strengthR(edgePaths.map(ContentIdentifier(context, _)))
      })

      ArgoBody(CourseMasteryDTO(usedCompetencies, masteredCompetencyIds, contentsByCompetency))
    end for
  end getLearnerMastery
end LightweightCourseMasteryWebControllerImpl

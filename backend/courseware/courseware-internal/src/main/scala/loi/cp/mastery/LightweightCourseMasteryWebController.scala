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
package mastery

import argonaut.Argonaut.*
import argonaut.*
import com.learningobjects.cpxp.component.annotation.{Controller, MatrixParam, QueryParam, RequestMapping}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ArgoBody, ErrorResponse, Method}
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import com.learningobjects.de.authorization.{Secured, SecuredAdvice}
import loi.cp.admin.right.CourseAdminRight
import loi.cp.competency.Competency
import loi.cp.context.ContextId
import loi.cp.course.right.{InteractCourseRight, TeachCourseRight}
import loi.cp.mastery.LightweightCourseMasteryWebController.CourseMasteryDTO
import loi.cp.reference.ContentIdentifier
import scalaz.\/
import scaloi.json.ArgoExtras
import scaloi.json.ArgoExtras.*

@Controller(root = true, value = "courseMastery")
@RequestMapping(path = "courseMastery")
trait LightweightCourseMasteryWebController extends ApiRootComponent:

  /** Get learner mastery information for the current user for all competencies associated with the given course
    * context.
    *
    * @param context
    *   The course context.
    * @param impersonatedUser
    *   View mastery of this specified user.
    * @return
    *   All competencies for the given course, content relations, and the current learner's mastery status.
    */
  @RequestMapping(method = Method.GET)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight]))
  def getLearnerMastery(
    @SecuredAdvice @MatrixParam("context") context: ContextId,
    @QueryParam(value = "viewAs", required = false) impersonatedUser: Option[JLong]
  ): ErrorResponse \/ ArgoBody[CourseMasteryDTO]
end LightweightCourseMasteryWebController

object LightweightCourseMasteryWebController:

  case class CourseMasteryDTO(
    competencies: List[Competency],
    mastered: Set[Long],
    relations: Map[Long, Set[ContentIdentifier]]
  )

  implicit def courseMasteryCodec: CodecJson[CourseMasteryDTO] =
    casecodec3(CourseMasteryDTO.apply, ArgoExtras.unapply)("competencies", "mastered", "relations")
end LightweightCourseMasteryWebController

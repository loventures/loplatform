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

import argonaut.Argonaut.*
import argonaut.CodecJson
import com.learningobjects.cpxp.component.annotation.{Controller, PathVariable, RequestMapping}
import com.learningobjects.cpxp.component.query.ApiPage
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ArgoBody, ErrorResponse, Method}
import com.learningobjects.de.authorization.{Secured, SecuredAdvice}
import loi.cp.admin.right.CourseAdminRight
import loi.cp.assessment.attempt.AssessmentParticipationData
import loi.cp.context.ContextId
import loi.cp.course.right.{TeachCourseRight, ViewCourseGradeRight}
import loi.cp.reference.{ContentIdentifier, EdgePath}
import scalaz.\/
import scaloi.json.ArgoExtras
import scaloi.json.ArgoExtras.instantCodec

import java.time.Instant

/** A web controller for informing instructor of which content has interactions that need attention.
  */
@Controller(value = "lwc-gradingQueue", root = true)
@RequestMapping(path = "gradingQueue")
trait GradingQueueWebController extends ApiRootComponent:

  /** Returns keys and statistics of content that needs instructor attention.
    *
    * @param contextId
    *   the context id
    * @param page
    *   any paging that should be applied to the results
    * @return
    *   the items that need instructor attention
    */
  @RequestMapping(path = "{contextId}", method = Method.GET)
  @Secured(value = Array(classOf[TeachCourseRight], classOf[ViewCourseGradeRight], classOf[CourseAdminRight]))
  def getGradingQueue(
    @SecuredAdvice @PathVariable("contextId") contextId: ContextId,
    page: ApiPage
  ): ErrorResponse \/ ArgoBody[List[GradingQueueDto]]
end GradingQueueWebController

case class GradingQueueDto(context: ContextId, edgePath: EdgePath, overview: ParticipationOverview)

object GradingQueueDto:
  implicit def gradingQueueDtoCodec: CodecJson[GradingQueueDto] =
    casecodec3(GradingQueueDto.apply, ArgoExtras.unapply)("context", "edgePath", "overview")

/** An overview for instructors for participation statistics in a thing
  *
  * @param contentId
  *   The interactable identifier.
  * @param participantCount
  *   The number of learners who have taken part in the interactable content.
  * @param actionItemCount
  *   The number of items associated with this interactable that are waiting on some instructor action.
  * @param mostRecentInteraction
  *   The latest time an item has been interacted with.
  */
case class ParticipationOverview(
  contentId: ContentIdentifier,
  participantCount: Int,
  actionItemCount: Int,
  mostRecentInteraction: Option[Instant]
)

object ParticipationOverview:
  def apply(data: AssessmentParticipationData): ParticipationOverview =
    ParticipationOverview(data.identifier, data.participantCount, data.awaitingInstructorInput, data.latestUpdate)

  implicit def participationOverviewCodec: CodecJson[ParticipationOverview] =
    casecodec4(ParticipationOverview.apply, ArgoExtras.unapply)(
      "contextId",
      "participantCount",
      "actionItemCount",
      "mostRecentInteraction"
    )
end ParticipationOverview

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

package loi.authoring.feedback

import argonaut.Argonaut.*
import com.learningobjects.cpxp.component.*
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.ApiQueries.ApiQueryOps
import com.learningobjects.cpxp.component.query.{
  ApiQueries,
  ApiQuery,
  ApiQueryResults,
  BaseApiFilter,
  PredicateOperator
}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.dto.Ontology
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.feedback.{AssetFeedbackFinder, FeedbackActivityFinder}
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.query.{Comparison, QueryBuilder, QueryService}
import com.learningobjects.cpxp.service.user.UserDTO
import loi.authoring.configuration.AuthoringConfigurationService
import loi.authoring.security.right.ViewAllProjectsRight
import loi.authoring.web.AuthoringApiWebUtils
import loi.cp.attachment.AttachmentComponent
import loi.cp.presence.PresenceService
import loi.cp.presence.SceneActor.InBranch
import loi.cp.web.HandleService
import scaloi.misc.TimeSource

//noinspection ScalaUnusedSymbol
@Component
@Controller(root = true)
@RequestMapping(path = "feedback")
class FeedbackActivityWebController(
  val componentInstance: ComponentInstance,
  domain: DomainDTO,
  user: UserDTO,
  now: TimeSource,
  feedbackService: FeedbackService,
)(implicit
  is: ItemService,
  fs: FacadeService,
  qs: QueryService,
  hs: HandleService,
  cs: ComponentService,
  ontology: Ontology,
  webUtils: AuthoringApiWebUtils,
  configurationService: AuthoringConfigurationService,
  presenceService: PresenceService,
) extends ApiRootComponent
    with ComponentImplementation:

  @RequestMapping(path = "{id}/attachments/{att}", method = Method.GET)
  def getAttachment(@PathVariable("id") id: Long, @PathVariable("att") att: Long): WebResponse =
    checkFeedbackAccess(id)
    att.component[AttachmentComponent].view(false, false, null)

  @RequestMapping(path = "{id}/activity", method = Method.GET)
  def getActivities(@PathVariable("id") id: Long, query: ApiQuery): ApiQueryResults[FeedbackActivityDto] =
    checkFeedbackAccess(id)
    ApiQueries
      .queryFinder[FeedbackActivityFinder](
        queryActivities,
        query.withPrefilter(new BaseApiFilter("feedback", PredicateOperator.EQUALS, id.toString))
      )
      .map(FeedbackActivityDto.apply)

  @RequestMapping(path = "{id}/transition", method = Method.POST)
  def transition(@PathVariable("id") id: Long, @RequestBody update: Transition): FeedbackDto =
    val feedback = checkFeedbackAccess(id)
    feedbackService.transition(feedback, update.status, update.closed)
    presentMessage(feedback)
    FeedbackDto(feedback)

  @RequestMapping(path = "{id}/assign", method = Method.POST)
  def assign(@PathVariable("id") id: Long, @RequestBody update: Assign): FeedbackDto =
    val feedback = checkFeedbackAccess(id)
    feedbackService.assign(feedback, update.assignee)
    presentMessage(feedback)
    FeedbackDto(feedback)

  @RequestMapping(path = "{id}/relocate", method = Method.POST)
  def relocate(@PathVariable("id") id: Long, @RequestBody update: Relocate): FeedbackDto =
    val feedback = checkFeedbackAccess(id)
    feedback.unitName = update.unitName.orNull
    feedback.moduleName = update.moduleName.orNull
    feedback.lessonName = update.lessonName.orNull
    feedback.contentName = update.contentName.orNull
    presentMessage(feedback)
    FeedbackDto(feedback)

  @RequestMapping(path = "{id}/reply", method = Method.POST)
  def reply(@PathVariable("id") id: Long, @RequestBody dto: NewReply): FeedbackActivityDto =
    val feedback    = checkFeedbackAccess(id)
    val folder      = FeedbackServiceImpl.projectFeedbackFolder
    val attachments = dto.attachments.map(upload => folder.addAttachment(upload).getId)
    val finder      = feedbackService.addReply(feedback, dto.value, attachments)
    presentMessage(feedback)
    FeedbackActivityDto(finder)

  @RequestMapping(path = "{id}/reply/{rid}", method = Method.PUT)
  def editReply(
    @PathVariable("id") id: Long,
    @PathVariable("rid") rid: Long,
    @RequestBody dto: EditReply
  ): FeedbackActivityDto =
    val feedback = checkFeedbackAccess(id)
    val activity = getActivityForEdit(id, rid, FeedbackEvent.Reply)
    activity.value = dto.value.asJson
    activity.edited = now.date
    feedback.modified = now.date
    presentMessage(feedback)
    FeedbackActivityDto(activity)
  end editReply

  @RequestMapping(path = "{id}/reply/{rid}", method = Method.DELETE)
  def deleteReply(
    @PathVariable("id") id: Long,
    @PathVariable("rid") rid: Long
  ): Unit =
    val feedback = checkFeedbackAccess(id)
    val activity = getActivityForEdit(id, rid, FeedbackEvent.Reply)
    is.delete(activity)
    feedback.modified = now.date
    presentMessage(feedback)
  end deleteReply

  private def presentMessage(feedback: AssetFeedbackFinder): Unit =
    if configurationService.getConfig.realTime then
      presenceService.deliverToScene(BranchFeedback("activity", user.id, Some(feedback.id)))(
        InBranch(feedback.branch, None)
      )

  private def checkFeedbackAccess(id: Long): AssetFeedbackFinder =
    val feedback = id.finder[AssetFeedbackFinder]
    val branch   = webUtils.branchOrFakeBranchOrThrow404(feedback.branch) // not really a 404 but
    webUtils.throw403ForNonProjectUserWithout[ViewAllProjectsRight](branch.project.get)
    feedback

  private def getActivityForEdit(id: Long, rid: Long, event: FeedbackEvent): FeedbackActivityFinder =
    queryActivities
      .addCondition(DataTypes.META_DATA_TYPE_ID, Comparison.eq, rid)              // id matches
      .addCondition(FeedbackActivityFinder.Feedback, Comparison.eq, id)           // feedback id matches
      .addCondition(FeedbackActivityFinder.Creator, Comparison.eq, user.id)       // created by me
      .addCondition(FeedbackActivityFinder.Event, Comparison.eq, event.entryName) // and is a reply/..
      .getFinder[FeedbackActivityFinder]
      .getOrElse(throw new ResourceNotFoundException())

  private def queryActivities: QueryBuilder = domain.queryAll[FeedbackActivityFinder]
end FeedbackActivityWebController

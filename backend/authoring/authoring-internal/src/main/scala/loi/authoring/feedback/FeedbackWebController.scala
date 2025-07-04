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

import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.*
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQueries, ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.dto.Ontology
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.exception.{AccessForbiddenException, ResourceNotFoundException}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.feedback.{AssetFeedbackFinder, FeedbackActivityFinder}
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.presence.EventType
import com.learningobjects.cpxp.service.query.{
  BaseCondition,
  BaseDataProjection,
  Comparison,
  Direction,
  Projection,
  QueryBuilder,
  QueryService,
  Function as QFunction
}
import com.learningobjects.cpxp.service.user.{UserDTO, UserFinder}
import com.learningobjects.cpxp.util.FileOps.*
import com.learningobjects.cpxp.util.HtmlUtils
import com.learningobjects.cpxp.web.ExportFile
import kantan.codecs.Encoder
import kantan.csv.CellEncoder
import loi.asset.util.Assex.*
import loi.authoring.branch.Branch
import loi.authoring.configuration.AuthoringConfigurationService
import loi.authoring.edge.EdgeService
import loi.authoring.node.AssetNodeService
import loi.authoring.project.web.ProjectsResponse
import loi.authoring.project.{AccessRestriction, ProjectService}
import loi.authoring.security.right.{EditSettingsAnyProjectRight, ViewAllProjectsRight}
import loi.authoring.web.AuthoringApiWebUtils
import loi.authoring.workspace.AttachedReadWorkspace
import loi.authoring.workspace.service.ReadWorkspaceService
import loi.cp.presence.PresenceService
import loi.cp.presence.SceneActor.InBranch
import loi.cp.right.RightService
import loi.cp.user.UserService
import loi.cp.web.HandleService
import scalaz.Memo
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.misc.TimeSource
import scaloi.syntax.boxes.*
import scaloi.syntax.option.*
import scaloi.syntax.ʈry.*

import java.lang
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone, UUID}
import scala.jdk.CollectionConverters.*

//noinspection ScalaUnusedSymbol
@Component
@Controller(root = true)
@RequestMapping(path = "feedback")
class FeedbackWebController(
  val componentInstance: ComponentInstance,
  domain: DomainDTO,
  user: UserDTO,
  now: TimeSource,
)(implicit
  is: ItemService,
  fs: FacadeService,
  qs: QueryService,
  hs: HandleService,
  ontology: Ontology,
  feedbackService: FeedbackService,
  projectService: ProjectService,
  workspaceService: ReadWorkspaceService,
  nodeService: AssetNodeService,
  edgeService: EdgeService,
  rightService: RightService,
  userService: UserService,
  webUtils: AuthoringApiWebUtils,
  configurationService: AuthoringConfigurationService,
  presenceService: PresenceService,
) extends ApiRootComponent
    with ComponentImplementation:

  @RequestMapping(method = Method.GET)
  def getFeedbacks(query: ApiQuery): ApiQueryResults[FeedbackDto] =
    checkAccess(query)
    ApiQueries
      .queryFinder[AssetFeedbackFinder](queryFeedback, query)
      .map(FeedbackDto.apply)

  @RequestMapping(path = "{id}", method = Method.GET)
  def getFeedback(@PathVariable("id") id: Long): Option[FeedbackDto] =
    getFeedbackFinder(id).map(FeedbackDto.apply)

  @RequestMapping(path = "count", method = Method.GET)
  def countFeedbacks(query: ApiQuery): Long =
    checkAccess(query)
    ApiQueries
      .queryBuilder[AssetFeedbackFinder](queryFeedback, query)
      .getAggregateResult(QFunction.COUNT)

  @RequestMapping(path = "summary", method = Method.GET)
  def getFeedbackSummary(query: ApiQuery): Seq[FeedbackSummary] =
    checkAccess(query)
    ApiQueries
      .queryBuilder[AssetFeedbackFinder](queryFeedback, query)
      .setGroup(
        AssetFeedbackFinder.UnitName,
        AssetFeedbackFinder.ModuleName,
        AssetFeedbackFinder.LessonName,
        AssetFeedbackFinder.ContentName,
        AssetFeedbackFinder.AssetName
      )
      .setFunction(QFunction.COUNT)
      .getValues[Array[AnyRef]] collect {
      case Array(count: lang.Long, unitName, moduleName, lessonName, contentName, assetName: UUID) =>
        FeedbackSummary(
          unitName = Option(unitName).accept[UUID],
          moduleName = Option(moduleName).accept[UUID],
          lessonName = Option(lessonName).accept[UUID],
          contentName = Option(contentName).accept[UUID],
          assetName = assetName,
          count = count.intValue,
        )
      // TODO: KILLME w/ Hibernate 6. In Hibernate 5 when we go native with a remotes query, this is the type
      case Array(count: Number, unitName, moduleName, lessonName, contentName, assetName: String)  =>
        FeedbackSummary(
          unitName = Option(unitName).accept[String].map(UUID.fromString),
          moduleName = Option(moduleName).accept[String].map(UUID.fromString),
          lessonName = Option(lessonName).accept[String].map(UUID.fromString),
          contentName = Option(contentName).accept[String].map(UUID.fromString),
          assetName = UUID.fromString(assetName),
          count = count.intValue,
        )
    }
  end getFeedbackSummary

  @RequestMapping(path = "assignees", method = Method.GET)
  def getFeedbackAssignees(query: ApiQuery): Seq[FeedbackProfileDto] =
    checkAccess(query)
    ApiQueries
      .queryBuilder[AssetFeedbackFinder](queryFeedback, query)
      .setDataProjection(AssetFeedbackFinder.Assignee)
      .setDistinct(true)
      .getValues[Long]
      .flatMap(_.finder_?[UserFinder])
      .map(FeedbackProfileDto.apply)
  end getFeedbackAssignees

  @RequestMapping(path = "ids", method = Method.GET)
  def getFeedbackIds(query: ApiQuery): Seq[Long] =
    checkAccess(query)
    ApiQueries
      .queryBuilder[AssetFeedbackFinder](queryFeedback, query)
      .setProjection(Projection.ID)
      .getValues[lang.Long]
      .unboxInside()

  @RequestMapping(path = "branches/{branch}/archive", method = Method.POST)
  def archiveFeedback(@PathVariable("branch") branchId: Long): Unit =
    webUtils.branchOrFakeBranchOrThrow404(branchId, AccessRestriction.projectOwnerOr[EditSettingsAnyProjectRight])
    feedbackService.archiveBranch(branchId)
    if configurationService.getConfig.realTime then
      presenceService.deliverToScene(BranchFeedback("archive", user.id, None))(InBranch(branchId, None))

  @RequestMapping(method = Method.POST)
  def addFeedback(@RequestBody dto: NewFeedback): FeedbackDto =
    // for now, assuming if you can view you can comment
    checkBranchAccess(dto.branch)
    val finder = feedbackService.addFeedback(
      project = dto.project,
      branch = dto.branch,
      assetName = dto.assetName,
      contentName = dto.contentName,
      lessonName = dto.lessonName,
      moduleName = dto.moduleName,
      unitName = dto.unitName,
      identifier = dto.identifier,
      section = None,
      quote = dto.quote,
      feedback = dto.feedback,
      attachments = dto.attachments,
      assignee = dto.assignee.unboxInside()
    )
    presentMessage("create", finder)
    FeedbackDto(finder)
  end addFeedback

  @RequestMapping(path = "{id}", method = Method.DELETE)
  def deleteFeedback(@PathVariable("id") id: Long): Unit =
    val feedback = getFeedbackFinder(id).getOrElse(throw new ResourceNotFoundException())
    if feedback.creator.id != user.id then throw new AccessForbiddenException();
    is.delete(feedback)
    presentMessage("delete", feedback)

  @RequestMapping(path = "{id}", method = Method.PUT)
  def editFeedback(@PathVariable("id") id: Long, @RequestBody dto: EditFeedback): FeedbackDto =
    val feedback = getFeedbackFinder(id).getOrElse(throw new ResourceNotFoundException())
    if feedback.creator.id != user.id then throw new AccessForbiddenException();
    feedback.feedback = dto.feedback
    feedback.modified = now.date
    feedback.edited = now.date
    presentMessage("update", feedback)
    FeedbackDto(feedback)

  @RequestMapping(path = "download", method = Method.GET)
  def downloadFeedbacks(query: ApiQuery, request: WebRequest): FileResponse[?] =
    checkAccess(query)
    val feedbacks = getFeedbacks(query)

    val comments = queryActivity
      .addCondition(BaseCondition.inIterable(FeedbackActivityFinder.Feedback, feedbacks.map(_.id)))
      .addCondition(FeedbackActivityFinder.Event, Comparison.eq, FeedbackEvent.Reply.entryName)
      .setOrder(FeedbackActivityFinder.Created, Direction.ASC)
      .setDataProjection(BaseDataProjection.ofData(FeedbackActivityFinder.Feedback, FeedbackActivityFinder.Value))
      .getValues[Array[AnyRef]]
      .collect({ case Array(id: lang.Long, value: argonaut.Json) =>
        id.longValue -> HtmlUtils.toPlaintext(value.stringOrEmpty)
      })
      .groupMap(_._1)(_._2)

    val workspaces = Memo.mutableHashMapMemo[Long, AttachedReadWorkspace] { branchId =>
      webUtils.workspaceOrThrow404(branchId, AccessRestriction.none)
    }
    val titles     = Memo.mutableHashMapMemo[(Long, UUID), Option[String]] { case (branchId, nodeName) =>
      nodeService.load(workspaces(branchId)).byName(nodeName).map(_.title) | "Deleted".some
    }

    def courseTitle(branchId: Long) = titles(branchId -> workspaces(branchId).homeName)

    val fileName = for
      branchId <- query.getAllFilters.asScala.find(f => f.getProperty == "branch").map(_.getValue.toLong)
      name     <- courseTitle(branchId)
    yield s"$name - Feedback.csv"
    val out      = ExportFile.create(fileName | "Feedback.csv", MediaType.CSV_UTF_8, request)

    val dateFmt = new SimpleDateFormat("M/d/YYYY hh:mm:ss a")
    // This is awful but Excel cannot import date/time/timezone. Having the browser send the desired
    // timezone would be possible but it would be weird for two people to download the same Excel and
    // see different times in it. So go with the domain default.
    dateFmt.setTimeZone(TimeZone.getTimeZone(domain.timeZoneId))

    implicit val dateCellEncoder: CellEncoder[Date] = Encoder.from(dateFmt.format)

    out.file.writeCsvWithBom[FeedbackRow] { csv =>
      feedbacks forEach { feedback =>
        csv.write(
          FeedbackRow(
            course = courseTitle(feedback.branch),
            unit = feedback.unitName.strengthL(feedback.branch).flatMap(titles),
            module = feedback.moduleName.strengthL(feedback.branch).flatMap(titles),
            lesson = feedback.lessonName.strengthL(feedback.branch).flatMap(titles),
            content = titles(feedback.branch -> feedback.assetName),
            created = feedback.created,
            creator = feedback.creator.fullName,
            modified = feedback.modified,
            status = feedback.status | "New", // TODO: This ought to come from feedback workflow config...
            assignee = feedback.assignee.map(_.fullName),
            quote = feedback.quote,
            feedback = HtmlUtils.toPlaintext(feedback.feedback),
            comments = comments.getOrElse(feedback.id, Seq.empty).map(_.replace("\n", "")).mkString("\n"),
          )
        )
      }
    }

    FileResponse(out.toFileInfo)
  end downloadFeedbacks

  @RequestMapping(path = "upstreamProjects", method = Method.GET)
  def getUpstreamProjects(query: ApiQuery): ProjectsResponse =
    checkAccess(query)
    val branches = ApiQueries
      .queryBuilder[AssetFeedbackFinder](queryFeedback, query)
      .setDataProjection(AssetFeedbackFinder.Branch)
      .setDistinct(true)
      .getValues[Number] // TODO Hibernate 6 makes these Long
      .flatMap(id => projectService.loadBronch(id.longValue, AccessRestriction.none))
    ProjectsResponse(branches)
  end getUpstreamProjects

  private def presentMessage(action: String, feedback: AssetFeedbackFinder): Unit =
    if configurationService.getConfig.realTime then
      presenceService.deliverToScene(BranchFeedback(action, user.id, Some(feedback.id)))(
        InBranch(feedback.branch, None)
      )

  private def getFeedbackFinder(id: Long): Option[AssetFeedbackFinder] =
    ApiQueries
      .queryFinder[AssetFeedbackFinder](queryFeedback, ApiQuery.byId(id))
      .asOption
      .tap(feedback => checkBranchAccess(feedback.branch))

  private def checkAccess(query: ApiQuery): Unit =
    query.getAllFilters.asScala.find(_.getProperty == "branch").map(_.getValue.toLong) match
      case Some(branchId) =>
        checkBranchAccess(branchId)

      case None =>
        if !rightService.getUserHasRight(classOf[ViewAllProjectsRight]) then throw new AccessForbiddenException()

  private def checkBranchAccess(branchId: Long): Branch = webUtils.branchOrFakeBranchOrThrow404(branchId)

  private def queryFeedback: QueryBuilder = domain.queryAll[AssetFeedbackFinder]

  private def queryActivity: QueryBuilder = domain.queryAll[FeedbackActivityFinder]
end FeedbackWebController

// User is used so a user's updates don't cause their own session to re-fetch
// the changes they make. This shouldn't be user id, it should be tab session id.
final case class BranchFeedback(action: String, user: Long, feedback: Option[Long])

object BranchFeedback:
  implicit val BranchFeedbackType: EventType[BranchFeedback] = EventType("BranchFeedback")

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

package loi.cp.feedback

import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.web.exception.InvalidRequestException
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.exception.AccessForbiddenException
import com.learningobjects.cpxp.service.feedback.{AssetFeedbackFinder, FeedbackActivityFinder}
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.user.{UserDTO, UserFinder}
import com.learningobjects.cpxp.util.FileOps.*
import com.learningobjects.cpxp.util.{DeXssSupport, FormattingUtils, HtmlUtils}
import com.learningobjects.cpxp.web.ExportFile
import kantan.codecs.Encoder
import kantan.csv.{CellEncoder, HeaderEncoder}
import loi.asset.lesson.model.Lesson
import loi.asset.module.model.Module
import loi.asset.unit.model.Unit1
import loi.authoring.asset.Asset
import loi.authoring.feedback.FeedbackService
import loi.authoring.project.ProjectService
import loi.authoring.project.exception.NoSuchProjectIdException
import loi.cp.attachment.AttachmentComponent
import loi.cp.content.{ContentAccessService, CourseContentService}
import loi.cp.course.CourseAccessService
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.reference.EdgePath
import loi.cp.user.UserService
import org.apache.commons.text.StringEscapeUtils
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.json.ArgoExtras
import scaloi.misc.TryInstances.*
import scaloi.syntax.boolean.*
import scaloi.syntax.boxes.*
import scaloi.syntax.option.*

import java.lang
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone, UUID}
import scala.jdk.OptionConverters.*
import scala.util.Try

//noinspection ScalaUnusedSymbol
@Component
@Controller(root = true)
class CourseFeedbackWebController(
  val componentInstance: ComponentInstance,
  courseAccessService: CourseAccessService,
  contentAccessService: ContentAccessService,
  contentService: CourseContentService,
  feedbackService: FeedbackService,
  projectService: ProjectService,
  userService: UserService,
  cus: CurrentUrlService,
  user: UserDTO,
  domain: DomainDTO,
)(implicit is: ItemService, cs: ComponentService)
    extends ApiRootComponent
    with ComponentImplementation:

  @RequestMapping(path = "lwc/{context}/feedback/{path}", method = Method.POST)
  def addFeedback(
    @PathVariable("context") id: Long,
    @PathVariable("path") path: EdgePath,
    @RequestBody dto: NewCourseFeedback
  ): Try[Unit] = for
    course       <- accessCourse(id)
    (_, content) <- contentAccessService.readContent(id, path, user)
    contentPath  <- contentService.getCourseContent(course, path, courseAccessService.actualRights(course, user))
    projectId    <- course.getProjectId.toScala <@~* new IllegalStateException()
    branch       <- course.getBranchId.toScala <@~* new IllegalStateException()
    project      <- projectService.loadProject(projectId).toTry(NoSuchProjectIdException(projectId))
    assignees     = project.userIds
    _            <- dto.assignee.traverse(id => assignees.contains(id) <@~* new InvalidRequestException("Bad assignee"))
  yield
    val assetName = dto.assetName | content.asset.info.name
    feedbackService.addFeedback(
      project = projectId,
      branch = branch,
      assetName = assetName,
      contentName = content.asset.info.name.some,
      lessonName = contentPath.flatMap(_.branch.rflatten.find(_.asset.is[Lesson])).map(_.asset.info.name),
      moduleName = contentPath.flatMap(_.branch.rflatten.find(_.asset.is[Module])).map(_.asset.info.name),
      unitName = contentPath.flatMap(_.branch.rflatten.find(_.asset.is[Unit1])).map(_.asset.info.name),
      identifier = dto.id,
      section = Some(course.getId),
      quote = dto.quote,
      feedback = dto.feedback,
      attachments = dto.attachments,
      assignee = dto.assignee.unboxInside(),
    )

  @RequestMapping(path = "lwc/{context}/feedback", method = Method.GET)
  def getMyFeedback(
    @PathVariable("context") id: Long,
  ): Try[Seq[CourseFeedback]] = for
    course   <- accessCourse(id)
    branch   <- course.getBranchId.toScala <@~* new IllegalStateException()
    contents <- contentService.getCourseContents(course, courseAccessService.actualRights(course, user))
  yield feedbackService
    .getFeedback(branch, user.id, None)
    .map(feedback => CourseFeedback(feedback, Option(feedback.contentName).flatMap(contents.get).map(_.edgePath), Nil))

  @RequestMapping(path = "lwc/{context}/feedback/download", method = Method.GET)
  def downloadFeedbacks(@PathVariable("context") id: Long, request: WebRequest): Try[FileResponse[?]] = for
    course   <- accessCourse(id)
    branch   <- course.getBranchId.toScala <@~* new IllegalStateException()
    contents <- contentService.getCourseContents(course, courseAccessService.actualRights(course, user))
  yield
    val feedbackMap = feedbackService
      .getFeedback(branch, user.id, None)
      .filter(_.contentName != null)
      .groupBy(_.contentName)

    val fileName = s"${course.getName} - Content Feedback.csv"
    val out      = ExportFile.create(fileName, MediaType.CSV_UTF_8, request)

    val dateFmt = new SimpleDateFormat("M/d/YYYY")
    dateFmt.setTimeZone(TimeZone.getTimeZone(domain.timeZoneId))

    implicit val dateCellEncoder: CellEncoder[Date] = Encoder.from(dateFmt.format)
    out.file.writeCsvWithBom[FeedbackRow] { csv =>
      contents.tree.tdhisto[Asset[?]] { (ancestors, content) =>
        for
          feedbacks <- feedbackMap.get(content.asset.info.name)
          feedback  <- feedbacks
        do
          csv.write(
            FeedbackRow(
              course = course.getName,
              section = course.getGroupId,
              unit = ancestors.find(_.is[Unit1]).flatMap(asset => contents.get(asset.info.name)).map(_.title),
              module = ancestors.find(_.is[Module]).flatMap(asset => contents.get(asset.info.name)).map(_.title),
              lesson = ancestors.find(_.is[Lesson]).flatMap(asset => contents.get(asset.info.name)).map(_.title),
              title = content.title,
              url = cus.getUrl(s"${course.getUrl}/#/instructor/content/${content.edgePath}"),
              created = feedback.created,
              status = Option(feedback.status) | "New",
              quote = Option(feedback.quote),
              feedback = HtmlUtils.toPlaintext(feedback.feedback),
            )
          )
        end for
        content.asset
      }
    }

    FileResponse(out.toFileInfo)

  @RequestMapping(path = "lwc/{context}/feedback/{path}", method = Method.GET)
  def getMyFeedback(
    @PathVariable("context") id: Long,
    @PathVariable("path") path: EdgePath,
  ): Try[Seq[CourseFeedback]] = for
    course   <- accessCourse(id)
    branch   <- course.getBranchId.toScala <@~* new IllegalStateException()
    contents <- contentService.getCourseContents(course, courseAccessService.actualRights(course, user))
    content  <- contents.get(path) <@~* new InvalidRequestException("Bad path")
  yield
    val feedbacks = feedbackService.getFeedback(branch, user.id, content.asset.info.name.some)
    val replies   = feedbackService.getReplies(feedbacks.map(_.id)).groupBy(_.feedback.id)

    feedbacks.map(feedback =>
      CourseFeedback(
        feedback,
        Option(feedback.contentName).flatMap(contents.get).map(_.edgePath),
        replies.getOrElse(feedback.id, Nil).map(FeedbackReply.apply)
      )
    )

  @RequestMapping(path = "lwc/{context}/feedback/{id}/attachments/{att}", method = Method.GET)
  def getAttachment(
    @PathVariable("context") context: Long,
    @PathVariable("id") id: Long,
    @PathVariable("att") att: Long
  ): Try[WebResponse] =
    for
      feedback         <- getMyFeedback(context, id)
      replies           = feedbackService.getReplies(id :: Nil)
      isAttachment      = feedback.attachments.contains(att)
      isReplyAttachment = replies.exists(reply => reply.attachments.contains(att))
      _                <- (isAttachment || isReplyAttachment) <@~* new InvalidRequestException("Bad attachment")
    yield att.component[AttachmentComponent].view(false, false, null)

  @RequestMapping(path = "lwc/{context}/feedback/{id}/reply", method = Method.POST)
  def postReply(
    @PathVariable("context") context: Long,
    @PathVariable("id") id: Long,
    @RequestBody dto: NewReply
  ): Try[FeedbackReply] =
    for feedback <- getMyFeedback(context, id)
    yield
      val html   = dto.reply.split("\n").map(s => s"<p>${StringEscapeUtils.escapeHtml4(s)}</p>").mkString
      val finder = feedbackService.addReply(feedback, html, Nil)
      FeedbackReply(finder)

  @RequestMapping(path = "lwc/{context}/feedback/{id}/close", method = Method.POST)
  def closeFeedback(
    @PathVariable("context") context: Long,
    @PathVariable("id") id: Long,
  ): Try[Unit] =
    for feedback <- getMyFeedback(context, id)
    yield feedbackService.transition(feedback, Some("Done"), closed = true)

  @RequestMapping(path = "lwc/{context}/feedback/{id}/reopen", method = Method.POST)
  def reopenFeedback(
    @PathVariable("context") context: Long,
    @PathVariable("id") id: Long,
  ): Try[Unit] =
    for feedback <- getMyFeedback(context, id)
    yield
      feedbackService.transition(feedback, None, closed = false)
      // If it was assigned to the instructor/SME for review, unassign
      if Option(feedback.assignee).map(_.id).contains(user.getId) then feedbackService.assign(feedback, None)

  @RequestMapping(path = "lwc/{context}/feedback/assignees", method = Method.GET)
  def assignees(
    @PathVariable("context") id: Long,
  ): Try[List[FeedbackProfile]] = for
    course    <- accessCourse(id)
    _         <- isSmeCourse(course) <@~* notAnSME
    projectId <- course.getProjectId.toScala <@~* new IllegalStateException()
    project   <- projectService.loadProject(projectId).toTry(NoSuchProjectIdException(projectId))
  yield userService
    .getUsers(project.userIds)
    .values
    .toList
    .map(u => FeedbackProfile(u.id, FormattingUtils.userStr(u.userName, u.givenName, u.middleName, u.familyName)))

  private def getMyFeedback(context: Long, id: Long): Try[AssetFeedbackFinder] =
    for
      course   <- accessCourse(context)
      branch   <- course.getBranchId.toScala <@~* new IllegalStateException()
      feedback <- id.finder_?[AssetFeedbackFinder] <@~* new InvalidRequestException("Bad path")
      _        <- (feedback.branch == branch) <@~* new InvalidRequestException("Bad branch")
      _        <- (feedback.creator.id == user.id ||
                    Option(feedback.assignee).exists(_.id == user.id)) <@~* new InvalidRequestException("Bad user")
    yield feedback

  // Anyone in test/preview sections, else instructors
  private def accessCourse(id: Long): Try[LightweightCourse] =
    for
      course <- contentAccessService.getCourseAsLearner(id, user)
      _      <- (isSmeCourse(course) || courseAccessService.hasInstructorAccess(course)) <@~* notAnInstructor
    yield course

  private def isSmeCourse(course: LightweightCourse) =
    course.getGroupType == GroupType.TestSection || course.getGroupType == GroupType.PreviewSection

  private def notAnInstructor = new AccessForbiddenException("Not an instructor")

  private def notAnSME = new AccessForbiddenException("Not an SME")
end CourseFeedbackWebController

private[feedback] final case class NewCourseFeedback(
  assetName: Option[UUID], // typically a question
  quote: Option[String],
  id: Option[String],
  feedback: String,
  attachments: List[UploadInfo],
  assignee: Option[lang.Long],
)

private[feedback] final case class FeedbackProfile(
  id: Long,
  fullName: String,
)

private[feedback] object FeedbackProfile:
  def apply(user: UserFinder): FeedbackProfile = new FeedbackProfile(
    user.id,
    FormattingUtils.userStr(user.userName, user.givenName, user.middleName, user.familyName),
  )

private[feedback] final case class CourseFeedback(
  id: Long,
  assetName: UUID,
  edgePath: Option[EdgePath],
  created: Date,
  modified: Date,
  edited: Option[Date],
  status: Option[String],
  assignee: Option[FeedbackProfile],
  quote: Option[String],
  feedback: String,
  attachments: List[Long],
  closed: Boolean,
  replies: List[FeedbackReply],
)

private[feedback] object CourseFeedback:
  def apply(feedback: AssetFeedbackFinder, edgePath: Option[EdgePath], replies: List[FeedbackReply]): CourseFeedback =
    new CourseFeedback(
      id = feedback.id,
      assetName = feedback.assetName,
      edgePath = edgePath,
      created = feedback.created,
      modified = feedback.modified,
      edited = Option(feedback.edited),
      assignee = Option(feedback.assignee).map(FeedbackProfile.apply),
      status = Option(feedback.status),
      quote = Option(feedback.quote),
      feedback = DeXssSupport.deXss(feedback.feedback, s"Feedback:${feedback.id}"),
      attachments = feedback.attachments.toList.unboxInside(),
      closed = feedback.closed,
      replies = replies,
    )
end CourseFeedback

private[feedback] final case class FeedbackReply(
  id: Long,
  created: Date,
  creator: FeedbackProfile,
  reply: String,
  attachments: List[Long],
)

private[feedback] object FeedbackReply:
  def apply(activity: FeedbackActivityFinder): FeedbackReply =
    new FeedbackReply(
      activity.id,
      activity.created,
      FeedbackProfile(activity.creator),
      DeXssSupport.deXss(activity.value.stringOrEmpty, s"FeedbackActivity:${activity.id}"),
      attachments = activity.attachments.toList.unboxInside(),
    )

private[feedback] final case class NewReply(reply: String)

private[feedback] final case class FeedbackRow(
  course: String,
  section: String,
  unit: Option[String],
  module: Option[String],
  lesson: Option[String],
  title: String,
  url: String,
  created: Date,
  status: String,
  quote: Option[String],
  feedback: String,
)

private[feedback] object FeedbackRow:
  implicit def feedbackRowHeaderEncoder(implicit dateCellEncoder: CellEncoder[Date]): HeaderEncoder[FeedbackRow] =
    HeaderEncoder.caseEncoder(
      "Course",
      "Section",
      "Unit",
      "Module",
      "Lesson",
      "Title",
      "Course URL",
      "Created",
      "Status",
      "Quote",
      "Feedback",
    )(ArgoExtras.unapply)
end FeedbackRow

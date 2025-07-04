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

package loi.cp.search

import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.{Stopwatch, Timer}
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.Box
import com.learningobjects.cpxp.util.FileOps.*
import com.learningobjects.cpxp.web.ExportFile
import kantan.csv.HeaderEncoder
import loi.authoring.html.LinkCheckService
import loi.cp.content.{ContentAccessService, CourseContentService}
import scaloi.json.ArgoExtras

import java.util.UUID
import scala.concurrent.duration.*

@Component
@Controller(root = true)
private[search] class InstructorLinkCheckWebController(
  val componentInstance: ComponentInstance,
  contentAccessService: ContentAccessService,
  courseContentService: CourseContentService,
  linkCheckService: LinkCheckService,
  urlService: CurrentUrlService,
  user: UserDTO,
) extends ApiRootComponent
    with ComponentImplementation:

  import InstructorLinkCheckWebController.*

  @RequestMapping(path = "lwc/{context}/linkCheck", method = Method.POST)
  def initLinkCheck(
    @PathVariable("context") context: Long,
    request: WebRequest
  ): String =
    val course = contentAccessService.getCourseAsInstructor(context, user).get
    singleton synchronized {
      // only if the singleton is empty or has expired do we accept a new link check operation
      if singleton.exists(_._2.elapsed < ErrorLimit) then ""
      else
        val out =
          ExportFile.create(s"${course.getName} (${course.getGroupId}) - Link Check.csv", MediaType.CSV_UTF_8, request)
        singleton.value = out.guid -> new Stopwatch
        out.guid
    }
  end initLinkCheck

  @RequestMapping(
    path = "lwc/{context}/linkCheck/{guid}",
    method = Method.POST,
    async = true,
  )
  def runLinkCheck(
    @PathVariable("context") context: Long,
    @PathVariable("guid") out: ExportFile
  ): Unit = try
    singleton synchronized {
      if !singleton.exists(_._1 == out.guid) then throw new IllegalStateException()
    }

    val timer     = new Timer(TimeLimit)
    val course    = contentAccessService.getCourseAsInstructor(context, user).get
    val contents  = courseContentService.getCourseContents(course).get
    val edgePaths = contents.nonRootElements.map(cc => cc.asset.info.name -> cc.edgePath).toMap
    val ws        = course.getWorkspace

    def contentUrl(name: UUID): Option[String] =
      edgePaths.get(name).map(ep => s"${course.getUrl}/#instructor/content/$ep")

    out.file.writeCsvWithBom[LinkStatusRow] { csv =>
      linkCheckService.hyperlinkCheck(ws, timer) { (link, status) =>
        csv.write(
          LinkStatusRow(
            course = course.getName,
            section = course.getGroupId,
            unit = link.path.unit.flatMap(_.title),
            module = link.path.module.flatMap(_.title),
            lesson = link.path.lesson.flatMap(_.title),
            title = link.path.content.flatMap(_.title),
            link = link.href,
            text = link.text,
            okay = status.isOkay(link.href),
            statusCode = status.code,
            statusMessage = status.message,
            redirect = status.redirect,
            url = link.path.content.map(_.name).flatMap(contentUrl).map(urlService.getUrl),
          )
        )
      }
      if timer.didExpire then csv.write(TruncatedLinkStatusRow)
    }
  finally
    singleton synchronized {
      if singleton.exists(_._1 == out.guid) then singleton.value = null
    }

  @RequestMapping(path = "lwc/{context}/linkCheck/{guid}", method = Method.GET)
  def downloadLinkCheck(
    @PathVariable("context") context: Long,
    @PathVariable("guid") out: ExportFile
  ): FileResponse[?] =
    contentAccessService.getCourseAsInstructor(context, user).get
    FileResponse(out.toFileInfo)
end InstructorLinkCheckWebController

private object InstructorLinkCheckWebController:
  // This singleton is used to enforce only a single link check operation per VM to prevent an instructor
  // training session from causing chaos.
  private val singleton = Box.empty[(String, Stopwatch)]

  private final val TimeLimit = 10.minutes // async

  // If the singleton is held for longer than this then assume some failure has occurred
  private final val ErrorLimit = TimeLimit + 1.minute

  private val TruncatedLinkStatusRow = LinkStatusRow(
    course = "** Check Timed Out, Results Truncated **",
    section = "",
    unit = None,
    module = None,
    lesson = None,
    title = None,
    link = "",
    text = "",
    okay = false,
    statusCode = 0,
    statusMessage = "",
    redirect = None,
    url = None,
  )
end InstructorLinkCheckWebController

private final case class LinkStatusRow(
  course: String,
  section: String,
  unit: Option[String],
  module: Option[String],
  lesson: Option[String],
  title: Option[String],
  link: String,
  text: String,
  okay: Boolean,
  statusCode: Int,
  statusMessage: String,
  redirect: Option[String],
  url: Option[String],
)

private object LinkStatusRow:

  implicit val linkStatusRowHeaderEncoder: HeaderEncoder[LinkStatusRow] = HeaderEncoder.caseEncoder(
    "Course",
    "Section",
    "Unit",
    "Module",
    "Lesson",
    "Title",
    "Link",
    "Text",
    "Okay",
    "Status Code",
    "Status Message",
    "Redirect",
    "Course URL",
  )(ArgoExtras.unapply)
end LinkStatusRow

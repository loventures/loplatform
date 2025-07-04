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

package loi.authoring.html

import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.util.Timer
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.util.FileOps.*
import com.learningobjects.cpxp.web.ExportFile
import com.learningobjects.de.authorization.Secured
import kantan.csv.HeaderEncoder
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringWebUtils
import scaloi.json.ArgoExtras

import scala.concurrent.duration.*

@Component
@Controller(root = true)
@Secured(Array(classOf[AccessAuthoringAppRight]))
private[html] class LinkCheckWebController(
  val componentInstance: ComponentInstance,
  webUtils: AuthoringWebUtils,
  linkCheckService: LinkCheckService,
  urlService: CurrentUrlService,
) extends ApiRootComponent
    with ComponentImplementation:

  import LinkCheckWebController.*

  @RequestMapping(path = "authoring/search/{branch}/html/linkCheck", method = Method.POST)
  def initLinkCheck(
    @PathVariable("branch") branchId: Long,
    request: WebRequest
  ): String =
    val branch  = webUtils.branchOrFakeBranchOrThrow404(branchId)
    val project = branch.requireProject
    val out     = ExportFile.create(s"${project.name.trim} - Link Check.csv", MediaType.CSV_UTF_8, request)
    out.guid

  @RequestMapping(
    path = "authoring/search/{branch}/html/linkCheck/{guid}",
    method = Method.POST,
    async = true,
  )
  def runLinkCheck(
    @PathVariable("branch") branchId: Long,
    @PathVariable("guid") out: ExportFile
  ): Unit =
    val timer = new Timer(TimeLimit)
    val ws    = webUtils.workspaceOrThrow404(branchId, cache = false)

    out.file.writeCsvWithBom[LinkStatusRow] { csv =>
      linkCheckService.hyperlinkCheck(ws, timer) { (link, status) =>
        csv.write(
          LinkStatusRow(
            project = ws.projectInfo.name,
            code = ws.projectInfo.code,
            `type` = ws.projectInfo.productType,
            status = ws.projectInfo.liveVersion,
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
            url = link.path.content.map(c => urlService.getUrl(c.href)),
          )
        )
      }
      if timer.didExpire then csv.write(TruncatedLinkStatusRow)
    }
  end runLinkCheck

  @RequestMapping(path = "authoring/search/{branch}/html/linkCheck/{guid}", method = Method.GET)
  def downloadLinkCheck(
    @PathVariable("branch") branchId: Long,
    @PathVariable("guid") out: ExportFile
  ): FileResponse[?] =
    webUtils.branchOrFakeBranchOrThrow404(branchId)
    FileResponse(out.toFileInfo)
end LinkCheckWebController

private object LinkCheckWebController:
  private final val TimeLimit = 10.minutes // async

  private val TruncatedLinkStatusRow = LinkStatusRow(
    project = "** Check Timed Out, Results Truncated **",
    code = None,
    `type` = None,
    status = None,
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
end LinkCheckWebController

private final case class LinkStatusRow(
  project: String,
  code: Option[String],
  `type`: Option[String],
  status: Option[String],
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
    "Project",
    "Code",   // metadata
    "Type",   // metadata
    "Status", // metadata
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
    "Authoring URL",
  )(ArgoExtras.unapply)
end LinkStatusRow

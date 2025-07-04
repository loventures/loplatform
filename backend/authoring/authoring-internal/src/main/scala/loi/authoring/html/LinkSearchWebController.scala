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
import loi.authoring.html.LinkSearchService.LoLinkRe
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringWebUtils
import scaloi.json.ArgoExtras

import scala.concurrent.duration.*

@Component
@Controller(root = true)
@Secured(Array(classOf[AccessAuthoringAppRight]))
private[html] class LinkSearchWebController(
  val componentInstance: ComponentInstance,
  webUtils: AuthoringWebUtils,
  linkSearchService: LinkSearchService,
  urlService: CurrentUrlService,
) extends ApiRootComponent
    with ComponentImplementation:

  import LinkSearchWebController.*

  @RequestMapping(path = "authoring/search/{branch}/html/links", method = Method.GET)
  def links(
    @PathVariable("branch") branchId: Long,
    request: WebRequest
  ): FileResponse[?] =
    val timer = new Timer(TimeLimit)
    val ws    = webUtils.workspaceOrThrow404(branchId, cache = false)

    val out = ExportFile.create(s"${ws.projectInfo.name.trim} - Links.csv", MediaType.CSV_UTF_8, request)

    out.file.writeCsvWithBom[LinkRow] { csv =>
      linkSearchService.hyperlinkSearch(ws, timer) { link =>
        if !link.href.contains(DifferenceEngine) && !LoLinkRe.matches(link.href) then
          csv.write(
            LinkRow(
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
              url = link.path.content.map(c => urlService.getUrl(c.href)),
            )
          )

      }
      if timer.didExpire then csv.write(TruncatedLinkRow)
    }

    FileResponse(out.toFileInfo)
  end links
end LinkSearchWebController

private object LinkSearchWebController:
  private final val TimeLimit = 45.seconds // 60 second web request timeout

  // skip links to our infrastructure
  private final val DifferenceEngine = "difference-engine.com"

  private val TruncatedLinkRow = LinkRow(
    project = "** Search Timed Out, Results Truncated **",
    code = None,
    `type` = None,
    status = None,
    unit = None,
    module = None,
    lesson = None,
    title = None,
    link = "",
    text = "",
    url = None,
  )
end LinkSearchWebController

private final case class LinkRow(
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
  url: Option[String],
)

private object LinkRow:

  implicit val linkRowHeaderEncoder: HeaderEncoder[LinkRow] = HeaderEncoder.caseEncoder(
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
    "Authoring URL",
  )(ArgoExtras.unapply)
end LinkRow

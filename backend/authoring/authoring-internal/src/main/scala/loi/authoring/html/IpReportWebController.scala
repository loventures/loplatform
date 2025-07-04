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
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.cpxp.util.FileOps.*
import com.learningobjects.cpxp.web.ExportFile
import com.learningobjects.de.authorization.Secured
import com.learningobjects.tagsoup.{Parser, XMLWriter}
import kantan.codecs.Encoder
import kantan.csv.{CellEncoder, HeaderEncoder}
import loi.asset.course.model.Course
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.BlobService
import loi.authoring.edge.EdgeService
import loi.authoring.index.web.DcmPathUtils.*
import loi.authoring.node.AssetNodeService
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.web.AuthoringWebUtils
import org.apache.commons.text.StringEscapeUtils
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.{Attributes, InputSource}
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*
import scaloi.json.ArgoExtras
import scaloi.syntax.option.*

import java.io.{StringReader, StringWriter}
import scala.collection.mutable
import scala.concurrent.duration.*

@Component
@Controller(root = true)
@Secured(Array(classOf[AccessAuthoringAppRight]))
private[html] class IpReportWebController(
  val componentInstance: ComponentInstance,
  webUtils: AuthoringWebUtils,
)(implicit
  blobService: BlobService,
  nodeService: AssetNodeService,
  edgeService: EdgeService,
  urlService: CurrentUrlService,
  mimeWebService: MimeWebService,
) extends ApiRootComponent
    with ComponentImplementation:

  import IpReportWebController.*
  import loi.authoring.index.IndexServiceImpl.inOrderSubtree

  @RequestMapping(path = "authoring/search/{branch}/html/ipReport", method = Method.GET)
  def ipReport(
    @PathVariable("branch") branchId: Long,
    request: WebRequest
  ): FileResponse[?] =
    val timer     = new Timer(TimeLimit)
    val workspace = webUtils.workspaceOrThrow404(branchId, cache = false)

    val home     = nodeService.loadA[Course](workspace).byName(workspace.homeName).get
    val assetIds = inOrderSubtree(workspace, home.info.id, _.traverse)

    val results =
      for
        asset <- nodeService.load(workspace).byId(assetIds)
        if !asset.info.archived && !timer.expired
        path   = searchPath(workspace, asset.info.name).path
        html  <- asset.htmls
        ips   <- IpExtractor.extract(html)
      yield IpRow(
        project = workspace.projectInfo.name,
        code = workspace.projectInfo.code,
        `type` = workspace.projectInfo.productType,
        status = workspace.projectInfo.liveVersion,
        module = path.find(_.typeId == AssetTypeId.Module).flatMap(_.title),
        lesson = path.find(_.typeId == AssetTypeId.Lesson).flatMap(_.title),
        title = path.head.title,
        url = urlService.getUrl(path.head.href),
        ip0 = ips.headOption,
        ip1 = ips.drop(1).headOption,
        ip2 = ips.drop(2).headOption,
        ip3 = ips.drop(3).headOption,
        ip4 = ips.drop(4).headOption,
        ip5 = ips.drop(5).headOption,
        ip6 = (ips.length > 6).option(ips.drop(6).mkString(";")),
      )

    val out = ExportFile.create(s"${home.data.title} - IP Report.csv", MediaType.CSV_UTF_8, request)

    out.file.writeCsvWithBom[IpRow] { csv =>
      results foreach { row =>
        csv.write(row)
      }
      if timer.didExpire then csv.write(TruncatedIpRow)
    }

    FileResponse(out.toFileInfo)
  end ipReport
end IpReportWebController

object IpReportWebController:
  private final val TimeLimit = 45.seconds // 60 second web request timeout

  private val TruncatedIpRow = IpRow(
    project = "** Search Timed Out, Results Truncated **",
    code = None,
    `type` = None,
    status = None,
    module = None,
    lesson = None,
    title = None,
    url = "",
    None,
    None,
    None,
    None,
    None,
    None,
    None
  )
end IpReportWebController

/** IP information stored as semicolon separated values within an H6 tag. Some of the values are hyperlinked labels. We
  * also store IP information as attributes on IMG tags.
  */
object IpExtractor:
  def extract(html: String): List[List[String]] =
    val results = mutable.Buffer.empty[List[String]]
    val parser  = new Parser
    parser.setContentHandler(new DefaultHandler:
      var h6     = 0
      val string = new StringWriter()
      val writer = new XMLWriter(string)

      override def startElement(uri: String, localName: String, qName: String, attributes: Attributes): Unit =
        if h6 > 0 then
          writer.startElement(uri, localName, qName, attributes)
          h6 += 1
        else if localName.equalsIgnoreCase("h6") then
          h6 = 1
          string.getBuffer.setLength(0)
        else if localName.equalsIgnoreCase("img") && IpAttributes.exists(attr => attributes.getValue("", attr) ne null)
        then results.append(IpAttributes.map(attr => Option(attributes.getValue("", attr)).orZ))

      override def endElement(uri: String, localName: String, qName: String): Unit =
        if h6 > 1 then // nested element
          writer.endElement(uri, localName, qName)
          h6 -= 1
        else if h6 == 1 then // end match
          h6 = 0
          results.append(StringEscapeUtils.unescapeHtml4(string.toString).split(';').toList.map(_.trim))

      override def characters(ch: Array[Char], start: Int, length: Int): Unit =
        if h6 > 0 then writer.characters(ch, start, length)

      override def ignorableWhitespace(ch: Array[Char], start: Int, length: Int): Unit =
        if h6 > 0 then writer.ignorableWhitespace(ch, start, length))
    parser.parse(new InputSource(new StringReader(html)))
    results.toList
  end extract

  final val IpAttributes = List(
    "data-ip-pickup",
    "data-ip-asset-type",
    "data-ip-asset-source",
    "data-ip-asset-source-id",
    "data-ip-credit-line",
    "data-ip-description",
    "data-ip-pickup-info",
  )
end IpExtractor

// People do not order the fields in the H6 tag and so we can have nothing better than a grab bag
// of IP columns
final case class IpRow(
  project: String,
  code: Option[String],
  `type`: Option[String],
  status: Option[String],
  module: Option[String],
  lesson: Option[String],
  title: Option[String],
  url: String,
  ip0: Option[String],
  ip1: Option[String],
  ip2: Option[String],
  ip3: Option[String],
  ip4: Option[String],
  ip5: Option[String],
  ip6: Option[String],
)

object IpRow:
  implicit val assetTypeIdCellEncoder: CellEncoder[AssetTypeId] = Encoder.from(_.entryName)

  implicit val ipRowHeaderEncoder: HeaderEncoder[IpRow] = HeaderEncoder.caseEncoder(
    "Project",
    "Code",   // metadata
    "Type",   // metadata
    "Status", // metadata
    "Module",
    "Lesson",
    "Title",
    "URL",
    "New/Pickup",
    "Asset Type",
    "Asset Source",
    "Asset Source ID",
    "Credit Line",
    "Description",
    "Pickup Info"
  )(ArgoExtras.unapply)
end IpRow

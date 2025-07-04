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

package loi.authoring.exchange.imprt.imscc

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.util.TempFileMap
import com.learningobjects.de.task.TaskReport
import loi.asset.html.model.Html
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.exchange.imprt.ImporterUtils.guid
import loi.authoring.exchange.imprt.qti.Qti1AssessmentImporter
import loi.authoring.exchange.imprt.{NodeExchangeBuilder, NodeFamily}
import loi.cp.i18n.AuthoringBundle

import scala.xml.Node

/** Imports Common Cartridge content.
  */
@Service
class CommonCartridgeResourceImporter(
  mapper: ObjectMapper,
  qti1AssessmentImporter: Qti1AssessmentImporter,
  ltiImporter: CommonCartridgeLtiImporter
):
  import FileUploads.*

  def buildResources(
    resources: Seq[Node],
    itemTitles: Map[String, String],
    files: TempFileMap,
    taskReport: TaskReport,
    persistNewLtiTools: Boolean
  ): Map[String, NodeFamily] =

    resources
      .map(r =>
        ManifestResource(
          (r \ "@identifier").text,
          (r \ "@type").text,
          (r \ "@href").text,
          (r \ "file" \ "@href").text,
          (r \ "@intendeduse").text,
          r
        )
      )
      .foldLeft(Map.empty[String, NodeFamily])((acc, res) =>
        res match
          case r if acc.contains(r.id)                                                         => acc
          case r if isQti(r.rtype)                                                             =>
            val assessmentFile  = files.get(r.fileHref)
            val assessmentTitle = itemTitles.getOrElse(r.id, r.fileHref)
            val assessment      = qti1AssessmentImporter.buildAssessment(
              assessmentTitle,
              assessmentFile,
              r.fileHref,
              files,
              AssetTypeId.Assessment.entryName,
              taskReport,
              "web_resources/"
            )
            acc ++ Map(r.id -> assessment)
          case r if isWebContent(r.rtype, r.rhref, r.intendedUse)                              =>
            acc ++ buildHtmlMap(r, files, itemTitles.getOrElse(r.id, r.rhref))
          case r if isLti(r.rtype)                                                             =>
            acc ++ buildLtiMap(r, files, persistNewLtiTools, taskReport)
          case r if r.rtype == "associatedcontent/imscc_xmlv1p2/learning-application-resource" => acc
          case r                                                                               =>
            /* Unknown content */
            taskReport.addWarning(
              AuthoringBundle.message("imscc.import.unknownResourceType", itemTitles.getOrElse(r.id, r.rhref), r.rtype)
            )
            acc
      )

  private case class ManifestResource(
    id: String,
    rtype: String,
    rhref: String,
    fileHref: String,
    intendedUse: String,
    node: Node
  )

  private def getFileExtension(filename: String): String =
    filename.substring(filename.lastIndexOf(".") + 1, filename.length).toLowerCase

  private def isQti(resourceType: String): Boolean = validAssessmentResourceTypes.contains(resourceType)

  private def isWebContent(resourceType: String, filename: String, intendedUse: String): Boolean =
    resourceType.toLowerCase == "webcontent" &&
      getFileExtension(filename).toLowerCase == "html" &&
      intendedUse.toLowerCase != "assignment"

  private def isLti(resourceType: String): Boolean = resourceType == "imsbasiclti_xmlv1p3"

  private def buildLtiMap(
    r: ManifestResource,
    files: TempFileMap,
    persistNewTools: Boolean,
    taskReport: TaskReport
  ): Map[String, NodeFamily] =
    val asset = ltiImporter.buildLtiActivity(r.node, files, persistNewTools, taskReport)
    Map(r.id -> NodeFamily(asset, Seq(asset)))

  private def buildHtmlMap(
    r: ManifestResource,
    files: TempFileMap,
    name: String,
  ): Map[String, NodeFamily] =
    val data  = Html(
      title = name,
      source = None,
      attribution = Some("IMSCC"),
    )
    val asset = NodeExchangeBuilder
      .builder(guid, AssetTypeId.Html.entryName, mapper.valueToTree(data))
      .attachment(r.fileHref)
      .build()
    Map(r.id -> NodeFamily(asset, Seq(asset)))
  end buildHtmlMap
end CommonCartridgeResourceImporter

object FileUploads:
  val validAssessmentResourceTypes =
    Set("imsqti_xmlv1p2/imscc_xmlv1p1/assessment", "imsqti_xmlv1p2/imscc_xmlv1p2/assessment")

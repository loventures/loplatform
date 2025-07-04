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

package loi.authoring.exchange.imprt.qti

import java.nio.file.Paths
import java.util.UUID

import loi.authoring.exchange.imprt.HtmlLoader
import loi.authoring.exchange.imprt.ImporterUtils.guid
import loi.authoring.exchange.imprt.qti.QtiImportUtils.*
import loi.authoring.exchange.model.NodeExchangeData
import loi.cp.i18n.AuthoringBundle

import scala.collection.mutable.ListBuffer
import scala.xml.{Elem, Node}
import scala.xml.transform.{RewriteRule, RuleTransformer}

object Qti1ImageImporter:

  /** Given a node, return a triple containing (1) the node with image links updated to point to LO edge GUIDs, (2) list
    * of image assets, and (3) map from image asset GUID to the associated edge GUID.
    */
  def addImages(request: QuestionImportRequest): (Node, Seq[NodeExchangeData], Map[String, UUID]) =
    val node       = request.xml
    val fileName   = request.fileName
    val files      = request.files
    val taskReport = request.taskReport
    val htmlLoader = new HtmlLoader

    var assets         = new ListBuffer[NodeExchangeData]
    var imageToEdgeMap = scala.collection.mutable.Map[String, UUID]()
    var srcToGuidMap   = scala.collection.mutable.Map[String, String]()

    object ImageRewriteRule extends RewriteRule:
      override def transform(n: Node): Seq[Node] =
        n match
          case img: Elem if img.label == "img" =>
            val src   = (img \ "@src").text.replace("%24IMS-CC-FILEBASE%24/", request.imsccFilebase)
            val alt   = (img \ "@alt").text
            val title = getImageTitle(img)

            if src.isEmpty then taskReport.addError(AuthoringBundle.message("qti.import.imageNotSpecified", fileName))

            val imagePath = Paths.get(request.workingDirectory, src).toString

            if src.nonEmpty && !files.containsKey(imagePath) then
              taskReport.addError(AuthoringBundle.message("qti.import.imageNotFound", fileName, imagePath))

            val (imgWidth, imgHeight) = getImageDimensions(imagePath, files)

            val imageGuid = srcToGuidMap.getOrElse(src, guid)
            if !srcToGuidMap.contains(src) then
              val image = buildImageAsset(imageGuid, title, alt, imgWidth, imgHeight, imagePath)
              assets += image
              srcToGuidMap += (src -> imageGuid)

            val edgeGuid = imageToEdgeMap.getOrElse(imageGuid, UUID.randomUUID())
            if !imageToEdgeMap.contains(imageGuid) then imageToEdgeMap += (imageGuid -> edgeGuid)

            val width  = getImageDimensions((img \ "@width").text)
            val height = getImageDimensions((img \ "@height").text)

            <img src={"loEdgeId://" + edgeGuid} title={title} alt={alt} width={width} height={height} />

          case _ => n
    end ImageRewriteRule

    object ImageRewriter extends RuleTransformer(ImageRewriteRule)

    /* MattextRewriteRule defined here so it can use the above mutable data structures. */
    object MattextRewriteRule extends RewriteRule:
      override def transform(n: Node): Seq[Node] =
        n match
          case mattext: Elem if mattext.label == "mattext" && (mattext \ "@texttype").text == "text/html" =>
            val html    = mattext.child.text
            /* loadString wraps fragment inside <html><body> */
            val child   = htmlLoader.loadString(html)
            /* Transform the img tags and replace the mattext text value. */
            val newHtml = (ImageRewriter(child) \ "body").head.child.mkString("")
            <mattext texttype="text/html">{newHtml}</mattext>

          case _ => n
    end MattextRewriteRule

    object MattextRewriter extends RuleTransformer(MattextRewriteRule)

    val result: Node = MattextRewriter(node)
    (result, assets.toSeq, imageToEdgeMap.toMap)
  end addImages
end Qti1ImageImporter

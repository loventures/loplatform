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

import java.io.{File, FileInputStream}

import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.util.TempFileMap
import com.learningobjects.de.task.TaskReport
import javax.imageio.ImageIO
import javax.xml.parsers.SAXParserFactory
import loi.asset.file.image.model.Image
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.exchange.imprt.NodeExchangeBuilder
import loi.authoring.exchange.model.NodeExchangeData
import loi.cp.i18n.{AuthoringBundle, BundleMessage}
import org.log4s.*
import scalaz.ValidationNel
import scaloi.syntax.OptionOps.*

import scala.util.Try
import scala.xml.*
object QtiImportUtils:
  private val logger: Logger = getLogger
  private val IMSManifestXML = "imsmanifest.xml"

  def getManifestXml(
    files: TempFileMap,
  ): ValidationNel[BundleMessage, File] =
    Option(files.get(IMSManifestXML))
      .elseInvalidNel(AuthoringBundle.message("qti.import.manifestNotFound"))

  def getResources(manifestXml: Elem): NodeSeq = manifestXml \ "resources" \ "resource"

  def getImageTitle(img: Node): String =
    val title = (img \ "@title").text
    if title.isEmpty then
      val src = (img \ "@src").text
      src.substring(src.lastIndexOf("/") + 1)
    else title

  def getImageDimensions(imagePath: String, files: TempFileMap): (Int, Int) =
    files.get(imagePath) match
      case file: File if imagePath.endsWith(".svg") =>
        /*
         * If DTD validation is on, this will sometimes fail with 503 Service Unavailable for
         * http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd.
         * Disable validation: https://stackoverflow.com/a/1098892/364029
         */
        val saxFactory = SAXParserFactory.newInstance()
        saxFactory.setNamespaceAware(false)
        saxFactory.setFeature("http://xml.org/sax/features/validation", false)
        saxFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
        saxFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
        saxFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        val saxParser  = saxFactory.newSAXParser()
        val svg        = XML.withSAXParser(saxParser).load(new FileInputStream(file))
        val width      = Try(stripUnit((svg \ "@width").text).toInt).getOrElse(0)
        val height     = Try(stripUnit((svg \ "@height").text).toInt).getOrElse(0)
        (width, height)
      case file: File                               =>
        val data = ImageIO.read(file)
        (data.getWidth, data.getHeight)
      case null                                     => (0, 0)

  def getImageDimensions(dim: String): Option[Text] =
    dim match
      case s: String if s == "" => None
      case s: String            => Some(Text(stripUnit(s)))

  private def stripUnit(s: String) = s.replace("px", "")

  def buildImageAsset(
    guid: String,
    title: String,
    altText: String,
    width: Int,
    height: Int,
    imagePath: String
  ): NodeExchangeData =
    val imageData = Image(
      title = title,
      altText = Option(altText),
      width = width,
      height = height,
    )
    NodeExchangeBuilder
      .builder(guid, AssetTypeId.Image.entryName, JacksonUtils.getFinatraMapper.valueToTree(imageData))
      .title(title)
      .attachment(imagePath)
      .build()
  end buildImageAsset

  def addWarningIfStylesheetIsPresent(assessmentItem: Node, taskReport: TaskReport, filename: String): Unit =
    if (assessmentItem \ "stylesheet").nonEmpty then
      taskReport.addWarning(AuthoringBundle.message("qti.import.stylesheetNotSupported", filename))

  /** Get QTI 2.x question correct/incorrect feedback as a tuple. If no feedback found, empty string will be returned.
    */
  def getItemFeedback(assessmentItem: Node): (String, String) =
    /*
     * QTI 2.x declares question feedback using the modalFeedback element. If the outcomeIdentifier attribute is
     * FEEDBACKBASIC (or FEEDBACK), we can extract the correct/incorrect feedback.
     *
     * Examples:
     * <modalFeedback outcomeIdentifier="FEEDBACKBASIC" showHide="show" identifier="correct">correct feedback</modalFeedback>
     * <modalFeedback outcomeIdentifier="FEEDBACKBASIC" showHide="show" identifier="incorrect">incorrect feedback</modalFeedback>
     *
     * Note that there's another case where outcomeIdentifier and identifier are unique IDs and you need to determine
     * whether it's correct or incorrect feedback by looking at responseProcessing, doing some boolean arithmetic to,
     * and then checking for the presence of the correct element or by calculating the SCORE. This second case is not
     * implemented at this time.
     */
    def getFeedback(id: String): String =
      def getFeedbackHelper(id: String, outcomeId: String): Option[String] =
        (assessmentItem \ "modalFeedback")
          .filter(mf => (mf \ "@outcomeIdentifier").text == outcomeId)
          .filter(mf => mf.attribute("showHide").map(_.text).getOrElse("show") == "show")
          .find(mf => (mf \ "@identifier").text == id)
          .map(_.text.trim)
      getFeedbackHelper(id, "FEEDBACKBASIC").orElse(getFeedbackHelper(id, "FEEDBACK")).getOrElse("")
    (getFeedback("correct"), getFeedback("incorrect"))
  end getItemFeedback
end QtiImportUtils

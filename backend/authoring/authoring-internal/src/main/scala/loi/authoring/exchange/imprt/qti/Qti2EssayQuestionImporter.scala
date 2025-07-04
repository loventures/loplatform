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

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.annotation.Service
import loi.asset.contentpart.{BlockPart, HtmlPart}
import loi.asset.question.{EssayContent, EssayQuestion}
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group
import loi.authoring.exchange.imprt.ImporterUtils.{buildEmptyEdgesFromAssets, guid}
import loi.authoring.exchange.imprt.qti.QtiImportUtils.addWarningIfStylesheetIsPresent
import loi.authoring.exchange.imprt.{ImporterUtils, NodeExchangeBuilder, NodeFamily}

import scala.xml.{Node, XML}

@Service
class Qti2EssayQuestionImporter(mapper: ObjectMapper):
  def buildQuestion(request: QuestionImportRequest): NodeFamily =
    val mathRequest                     = request.copy(xml = ImporterUtils.convertMath(request.xml))
    val (assessmentItem, images, edges) = Qti2ImageImporter.addImages(mathRequest)

    addWarningIfStylesheetIsPresent(assessmentItem, request.taskReport, request.fileName)

    val itemBody = (assessmentItem \ "itemBody").head

    val prompt = ImporterUtils.cleanUpXml(getPromptNodes(itemBody).mkString("\n"))

    val essayData = EssayQuestion(
      title = "Untitled",
      keywords = null,
      questionContent = EssayContent(
        allowDistractorRandomization = Some(true),
        questionContentBlockText = BlockPart(parts = Seq(HtmlPart(prompt))),
        richCorrectAnswerFeedback = Some(HtmlPart("")),
        richIncorrectAnswerFeedback = Some(HtmlPart(""))
      )
    )

    val question = NodeExchangeBuilder
      .builder(guid, AssetTypeId.EssayQuestion.entryName, mapper.valueToTree(essayData))
      .edges(buildEmptyEdgesFromAssets(images, Group.Resources, edges))
      .build()
    NodeFamily(question, Seq(question) ++ images)
  end buildQuestion

  def getPromptNodes(itemBody: Node): Seq[Node] =
    itemBody.child map {
      case n: Node if n.label == "extendedTextInteraction" =>
        val prompt = (n \ "prompt").headOption.map(_.child).getOrElse(Seq.empty).mkString("\n")
        XML.loadString(s"<div>$prompt</div>")
      case n                                               => n
    }
end Qti2EssayQuestionImporter

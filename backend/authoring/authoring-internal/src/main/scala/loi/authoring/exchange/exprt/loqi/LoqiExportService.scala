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

package loi.authoring.exchange.exprt.loqi

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.util.FileUtils.cleanFilename
import com.learningobjects.de.web.MediaType.APPLICATION_ZIP_VALUE
import loi.asset.contentpart.{BlockPart, ContentPart, HtmlPart}
import loi.asset.course.model.Course
import loi.asset.root.model.Root
import loi.asset.util.Assex.*
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group.*
import loi.authoring.edge.{EdgeService, TraverseGraph, TraversedGraph}
import loi.authoring.exchange.docx.DocxQuestionAppender
import loi.authoring.exchange.docx.format.{HtmlSink, RunTag}
import loi.authoring.exchange.exprt.CompetencyUtil
import loi.authoring.node.AssetNodeService
import loi.authoring.workspace.AttachedReadWorkspace
import org.apache.commons.lang3.StringUtils.isNotBlank
import org.apache.poi.xwpf.usermodel.XWPFDocument
import scalaz.std.string.*
import scaloi.syntax.collection.*
import scaloi.syntax.option.*

import java.io.*
import java.util.UUID
import java.util.zip.{ZipEntry, ZipOutputStream}
import scala.util.{Try, Using}

@Service
class LoqiExportService(
  nodes: AssetNodeService,
)(implicit
  es: EdgeService
):

  import LoqiExportService.*

  /** Returns a zip file containing branch assessments as Word documents.
    *
    * The [[UploadInfo]] contains the suggested file name and media type.
    */
  def doExport(ws: AttachedReadWorkspace): Try[UploadInfo] =
    for
      root   <- nodes.loadA[Root](ws).byName(ws.rootName)
      course <- nodes.loadA[Course](ws).byName(ws.homeName)
    yield
      val suffix = ".zip"
      val zName  = s"${cleanFilename(ws.projectInfo.name)}$suffix"
      val zFile  = File.createTempFile("pAssessments-export", suffix)

      val graph = es.stravaigeOutGraphs(
        List(
          TraverseGraph
            .fromSource(course.info.name)
            .traverse(Elements)
            .traverse(Elements, Questions, Assesses) // course-level assessment qs (extant in old content)
            .traverse(Elements, Questions, Assesses) // course-level assessment qs (extant in old content)
            .traverse(Elements, Questions, Assesses) // 2n arg module-level assessment qs
            .traverse(Questions, Assesses)           // lesson-level assessment qs, module assess comps
            .traverse(Assesses),                     // lesson-level assessment competencies
          CompetencyUtil.rootCompetenciesGraph(root)
        ),
        ws
      )

      val rootCompetencies = CompetencyUtil.rootCompetencies(root, graph)

      Using.resource(new ZipOutputStream(new FileOutputStream(zFile))) { zOs =>
        graph.nodes
          .filter(a => QuizTypes.contains(a.info.typeId))
          .zipWithIndex
          .foreach { case (quiz, i) =>
            val entry                = new ZipEntry(
              // e.g. 001_title_for_quiz_1.docx
              // 251 = (260 - len('001_') - len('.docx') // 260 char limit in old windows
              "%03d_".format(i + 1)
                + s"${cleanFilename(quiz.title.orZ.take(251).trim)}.docx"
            )
            zOs.putNextEntry(entry)
            val questions            = graph.targetsInGroup(quiz, Questions)
            val questionCompetencies = getQuestionCompetencies(questions, graph, ws, rootCompetencies)
            val bOs                  = new ByteArrayOutputStream()
            exportQuiz(quiz, questions, questionCompetencies, bOs).get
            zOs.write(bOs.toByteArray)
            zOs.closeEntry()
          }
      }
      new UploadInfo(zName, APPLICATION_ZIP_VALUE, zFile, true)

  def exportQuiz(ws: AttachedReadWorkspace, quiz: Asset[?], out: OutputStream): Try[Unit] =
    val root  = nodes.loadA[Root](ws).byName(ws.rootName).get
    val graph =
      es.stravaigeOutGraphs(
        List(
          TraverseGraph
            .fromSource(quiz.info.name)
            .traverse(Questions)
            .traverse(Assesses),
          CompetencyUtil.rootCompetenciesGraph(root)
        ),
        ws
      )

    val questions            = graph.targetsInGroup(quiz, Questions)
    val rootCompetencies     = CompetencyUtil.rootCompetencies(root, graph)
    val questionCompetencies = getQuestionCompetencies(questions, graph, ws, rootCompetencies)

    exportQuiz(quiz, questions, questionCompetencies, out)
  end exportQuiz

  private def getQuestionCompetencies(
    questions: Seq[Asset[?]],
    graph: TraversedGraph,
    ws: AttachedReadWorkspace,
    rootCompetencies: Map[UUID, Asset[?]]
  ) =
    questions.groupMapUniq(_.info.name)(question =>
      CompetencyUtil.assetCompetencies(question, Assesses, graph, rootCompetencies)
    )

  /** Writes assessment questions into output stream as Word document. Nothing is done if the provided asset type is not
    * in [[QuizTypes]].
    *
    * @param asset
    *   The assessment
    * @param questions
    *   Asset questions.
    * @param competencies
    *   Question competencies.
    * @param os
    *   The output stream where document is written
    */
  def exportQuiz(
    asset: Asset[?],
    questions: Seq[Asset[?]],
    competencies: Map[UUID, Seq[Asset[?]]],
    os: OutputStream,
  ): Try[Unit] =
    Try {
      val doc    = new XWPFDocument()
      val target = HtmlSink(doc)
      target.addHtml(s"<$b>${asset.title.orZ}</$b>")
      doc.createParagraph()
      asset.instructions.flatMap(html).foreach(target.addHtml)
      doc.createParagraph()
      DocxQuestionAppender.appendQuestions(doc, questions, competencies)
      doc.write(os)
    }

  private def html(b: BlockPart) =
    Some(
      b.parts
        .flatMap {
          case p: HtmlPart    => Some(p.html)
          case c: ContentPart => c.renderedHtml
        }
        .mkString("\n")
    ).filter(isNotBlank)
end LoqiExportService

private object LoqiExportService:
  private val b = RunTag.Strong.entryName

  val QuizTypes: Set[AssetTypeId] = Set(
    AssetTypeId.Assessment,
    AssetTypeId.Diagnostic,
    AssetTypeId.PoolAssessment,
    AssetTypeId.Checkpoint,
  )

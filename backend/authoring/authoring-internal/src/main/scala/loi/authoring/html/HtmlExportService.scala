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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.service.exception.HttpApiException.badRequest
import loi.asset.course.model.Course
import loi.asset.html.model.Html
import loi.asset.lesson.model.Lesson
import loi.asset.module.model.Module
import loi.asset.root.model.Root
import loi.asset.unit.model.Unit1
import loi.asset.util.Assex.*
import loi.authoring.asset.Asset
import loi.authoring.asset.exception.UnsupportedAssetType
import loi.authoring.blob.BlobService
import loi.authoring.edge.Group.{Assesses, Elements, Resources, Teaches}
import loi.authoring.edge.{EdgeService, TraverseGraph, TraversedGraph}
import loi.authoring.exchange.exprt.CompetencyUtil
import loi.authoring.index.Strings.*
import loi.authoring.node.AssetNodeService
import loi.authoring.syntax.index.*
import loi.authoring.workspace.AttachedReadWorkspace
import org.apache.commons.io.IOUtils
import scalaz.std.string.*
import scaloi.syntax.option.*

import java.io.{File, FileOutputStream, OutputStreamWriter, PrintWriter}
import java.nio.charset.StandardCharsets
import java.util.zip.{ZipEntry, ZipOutputStream}
import javax.inject.Inject
import scala.util.Using

@Service
class HtmlExportService @Inject() (htmlTransferService: HtmlTransferService)(implicit
  edgeService: EdgeService,
  nodeService: AssetNodeService,
  blobService: BlobService,
  currentUrlService: CurrentUrlService,
):

  def exportHtmlZip(
    file: File,
    asset: Asset[?],
    workspace: AttachedReadWorkspace
  ): Unit =
    val graph = courseGraph(workspace)

    Using.resource(new ZipOutputStream(new FileOutputStream(file))) { zOs =>
      htmlTransferService.transferOperations(asset, graph) foreach {
        case TransferOp.Directory(path) =>
          zOs.putNextEntry(new ZipEntry(path))

        case TransferOp.Instructions(path, _, instructions) =>
          zOs.putNextEntry(new ZipEntry(path))
          zOs.write(instructions.getBytes(StandardCharsets.UTF_8))
          zOs.closeEntry()

        case TransferOp.Content(path, _, html) =>
          zOs.putNextEntry(new ZipEntry(path))
          zOs.write(html.getBytes(StandardCharsets.UTF_8))
          zOs.closeEntry()
      }
    }
  end exportHtmlZip

  def exportHtmlDoc(
    file: File,
    asset: Asset[?],
    course: Asset[Course],
    workspace: AttachedReadWorkspace
  ): Unit =
    val root         = nodeService.loadA[Root](workspace).byName(workspace.rootName).get
    val graph        = assetWithCompetencyGraph(asset, workspace, root)
    val competencies = CompetencyUtil.rootCompetencies(root, graph)

    Using.resource(
      new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
    ) { writer =>
      def writeHeader(prefix: H1Prefix, asset: Asset[?]): Unit =
        writer.println(s"<h1>[${prefix.shortForm}] – ${asset.title.orZ.trim.html4}</h1>")
        val teachesAndAssesses =
          asset.assetType.edgeConfig.contains(Assesses) && asset.assetType.edgeConfig.contains(Teaches)
        List(Teaches, Assesses) foreach { group =>
          val prefix = if teachesAndAssesses then group.entryName else "LO" // prefer LO, else Teaches/Assesses
          CompetencyUtil.assetCompetencies(asset, group, graph, competencies) foreach { competency =>
            writer.println(s"<h7><strong>[$prefix]</strong> ${competency.title.orZ.trim.html4}</h7>")
          }
        }
      end writeHeader

      def loop(asset: Asset[?]): Unit =
        writeHeader(H1Prefix.forTypeId(asset.info.typeId), asset)

        graph
          .targetsInGroup(asset, Elements)
          .foldLeft[Option[Asset[Lesson]]](None) {
            case (_, Lesson.Asset(lesson)) =>
              loop(lesson)
              Some(lesson)

            case (afterLesson, Html.Asset(html)) =>
              afterLesson foreach { lesson =>
                writer.println(s"<h1>[${H1Prefix.EndLesson.shortForm}] – End of ${lesson.title.orZ.trim.html4}</h1>")
              }
              writeHeader(H1Prefix.Page, html)
              for
                blob    <- blobService.getBlobInfo(html)
                src      = Using.resource(blob.openInputStream())(IOUtils.toString(_, StandardCharsets.UTF_8))
                rendered = CrappyRenderer.render(src, html, graph).trim
              do writer.println(rendered)
              None

            case (afterLesson, other) =>
              afterLesson foreach { lesson =>
                writer.println(s"<h1>[${H1Prefix.EndLesson.shortForm}] – End of ${lesson.title.orZ.trim.html4}</h1>")
              }
              writeHeader(H1Prefix.forTypeId(other.info.typeId), other)
              for
                blockPart <- other.instructions
                html       = blockPart.htmls.mkString
                rendered   = CrappyRenderer.render(html, other, graph).trim
              do writer.println(rendered)
              None
          }
      end loop

      writer.println(s"""
           |<html>
           |<head>
           |<meta charset="utf-8" />
           |<title>${course.data.title.html4} – ${asset.title.orZ.html4}</title>
           |</head>
           |<body>
           |""".stripMargin)

      loop(asset)

      writer.println("""
          |</body>
          |</html>
          |""".stripMargin)
    }
  end exportHtmlDoc

  def courseGraph(workspace: AttachedReadWorkspace): TraversedGraph =
    edgeService.stravaigeOutGraphs(
      TraverseGraph
        .fromSource(workspace.homeName)
        .traverse(Elements)
        .traverse(Elements, Resources)
        .traverse(Elements, Resources)
        .traverse(Elements, Resources)
        .traverse(Resources) :: Nil,
      workspace
    )

  def assetWithCompetencyGraph(
    asset: Asset[?],
    workspace: AttachedReadWorkspace,
    root: Asset[Root],
  ): TraversedGraph = asset match
    case Unit1.Asset(unit)    =>
      edgeService.stravaigeOutGraphs(
        CompetencyUtil.rootCompetenciesGraph(root) :: (courseElements(workspace) ::
          List(
            TraverseGraph
              .fromSource(unit.info.name)
              .traverse(Elements)
              .traverse(Elements, Teaches, Assesses, Resources)
              .traverse(Elements, Teaches, Assesses, Resources)
              .traverse(Teaches, Resources),
          )),
        workspace
      )
    case Module.Asset(module) =>
      edgeService.stravaigeOutGraphs(
        CompetencyUtil.rootCompetenciesGraph(root) :: (courseElements(workspace) ::
          List(
            TraverseGraph
              .fromSource(module.info.name)
              .traverse(Elements)
              .traverse(Elements, Teaches, Assesses, Resources)
              .traverse(Teaches, Resources),
          )),
        workspace
      )
    case Lesson.Asset(lesson) =>
      edgeService.stravaigeOutGraphs(
        CompetencyUtil.rootCompetenciesGraph(root) :: (courseElements(workspace) ::
          List(
            TraverseGraph
              .fromSource(lesson.info.name)
              .traverse(Elements, Teaches, Assesses)
              .traverse(Teaches, Resources),
          )),
        workspace
      )
    case invalid              =>
      throw badRequest(UnsupportedAssetType(invalid.info.typeId))

  // This junk is to know remotes during import, should be killed w/ laird
  private def courseElements(workspace: AttachedReadWorkspace) =
    TraverseGraph
      .fromSource(workspace.homeName)
      .traverse(Elements)
      .traverse(Elements)
      .traverse(Elements)
      .traverse(Elements)
end HtmlExportService

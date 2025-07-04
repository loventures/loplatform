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

import com.google.common.hash.Hashing
import com.google.common.io.CharSource
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.exception.UnprocessableEntityException
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.web.ExportFile
import com.learningobjects.de.web.MediaType
import loi.asset.assessment.model.*
import loi.asset.competency.service.CompetencyService
import loi.asset.contentpart.{BlockPart, HtmlPart}
import loi.asset.discussion.model.Discussion1
import loi.asset.external.CourseLink
import loi.asset.file.fileBundle.model.FileBundle
import loi.asset.html.model.Html
import loi.asset.lesson.model.Lesson
import loi.asset.root.model.Root
import loi.asset.resource.model.Resource1
import loi.asset.util.Assex.*
import loi.authoring.asset.Asset
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.blob.{BlobRef, BlobService}
import loi.authoring.edge.Group.{Assesses, Elements, Teaches}
import loi.authoring.edge.{EdgeService, Group, TraversedGraph}
import loi.authoring.exchange.exprt.{CompetencyUtil, CourseStructureExportService}
import loi.authoring.node.AssetNodeService
import loi.authoring.workspace.{AttachedReadWorkspace, WriteWorkspace}
import loi.authoring.write.*
import org.apache.commons.io.IOUtils
import scalaz.\/
import scalaz.std.list.*
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.syntax.claѕѕ.*
import scaloi.syntax.collection.*
import scaloi.syntax.option.*

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.ZipFile
import javax.inject.Inject
import scala.annotation.nowarn
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.Using

@Service
class HtmlImportService @Inject() (
  competencyService: CompetencyService,
  htmlExportService: HtmlExportService,
  htmlTransferService: HtmlTransferService,
  writeService: WriteService,
)(implicit
  edgeService: EdgeService,
  nodeService: AssetNodeService,
  blobService: BlobService,
  currentUrlService: CurrentUrlService,
):
  import CrappyRenderer.*
  import PageSection.blatTrans

  /** Validates an HTML zip file, returning a list of affected assets. */
  def validateHtmlZip(
    file: File,
    asset: Asset[?],
    workspace: AttachedReadWorkspace
  ): List[Asset[?]] =
    processHtmlZip[Asset[?], List[Asset[?]]](
      workspace,
      asset,
      file,
      onInstructionsChange = (asset, _) => asset,
      onContentChange = (asset, _) => asset,
      commit = assets => assets.toList
    )

  /** Imports an HTML zip file, returning the commit result. */
  def importHtmlZip(
    file: File,
    asset: Asset[?],
    workspace: WriteWorkspace
  ): CommitResult[AttachedReadWorkspace] =
    processHtmlZip[WriteOp, CommitResult[AttachedReadWorkspace]](
      workspace,
      asset,
      file,
      onInstructionsChange =
        (asset, updated) => asset.withInstructions(updated.blockPart).map(SetNodeData.fromAsset).get,
      onContentChange = (asset, updated) =>
        val blobRef = uploadHtml(updated, asset.data.title, commit = true)
        editHtmlOp(asset, blobRef)
      ,
      commit = writeOps => writeService.commit(workspace, writeOps.toList).get
    )

  private def processHtmlZip[A, B](
    workspace: AttachedReadWorkspace,
    root: Asset[?],
    file: File,
    onInstructionsChange: (Asset[?], String) => A,
    onContentChange: (Asset[Html], String) => A,
    commit: LazyList[A] => B
  ): B =
    val graph = htmlExportService.courseGraph(workspace)

    Using.resource(new ZipFile(file)) { zip =>
      val assetPath    = ExportFile.cleanFilename(root.title | "Untitled") + '/'
      val hasAssetPath = zip.entries.asScala.exists(_.getName.startsWith(assetPath))

      def zipPath(path: String) = if hasAssetPath then path else path.stripPrefix(assetPath)

      val results = htmlTransferService.transferOperations(root, graph) flatMap {
        case TransferOp.Directory(_) => None

        case TransferOp.Instructions(path, asset, original) =>
          for
            entry          <- zip.entry(zipPath(path))
            updated         = IOUtils.toString(zip.getInputStream(entry), StandardCharsets.UTF_8)
            if updated != original
            (_, derendered) = derender(updated, derenderMap(asset, graph))
          yield onInstructionsChange(asset, derendered)

        case TransferOp.Content(path, asset, original) =>
          for
            entry          <- zip.entry(zipPath(path))
            updated         = IOUtils.toString(zip.getInputStream(entry), StandardCharsets.UTF_8)
            if updated != original
            (_, derendered) = derender(updated, derenderMap(asset, graph))
          yield onContentChange(asset, derendered)
      }

      commit(results)
    }
  end processHtmlZip

  def editHtmlOp(
    asset: Asset[Html],
    blobRef: Option[BlobRef]
  ): WriteOp = SetNodeData[Html](asset.info.name, asset.data.copy(source = blobRef))

  def validateHtmlDoc(
    file: File,
    asset: Asset[?],
    workspace: AttachedReadWorkspace,
  ): PageImportValidation =
    val competencies     = competencyService.getCompetenciesByName(workspace)
    val root             = nodeService.loadA[Root](workspace).byName(workspace.rootName).get
    val graph            = htmlExportService.assetWithCompetencyGraph(asset, workspace, root)
    val rootCompetencies = CompetencyUtil.rootCompetencies(root, graph)

    val importValidity = PageSection.parse[List](file, asset, competencies)

    importValidity.fold(
      errors => InvalidPageImport(errors.list.toList.map(_.message)),
      success =>
        val (wrn, sections) = success
        val aligned         = PageSection.isAligned(sections)
        val warnings        = mutable.ListBuffer.from(wrn)
        val ops             =
          sections.flatMap(importOps(_, asset, warnings, graph, rootCompetencies, aligned))
        ValidPageImport(
          warnings = warnings.toList,
          added = ops.collectType[AddNode[?]].map(add => assetDataTitle(add.assetType.id, add.data)),
          modified = ops.collect {
            case snd: SetNodeData[?] => assetDataTitle(snd.assetType.id, snd.data)
            case sed: SetEdgeData    => // Overlay
              val edge = workspace.edgeInfo(sed.name)
              val node = nodeService.load(workspace).byId(edge.targetId).get
              assetDataTitle(node.info.typeId, node.data)
          },
          alignmentsAdded = ops.collectType[AddEdge].count(_.group == Teaches),
          alignmentsRemoved = ops.collectType[DeleteEdge].size,
        )
    )
  end validateHtmlDoc

  def importHtmlDoc(
    file: File,
    asset: Asset[?],
    workspace: WriteWorkspace,
  ): CommitResult[AttachedReadWorkspace] =
    val competencies     = competencyService.getCompetenciesByName(workspace)
    val root             = nodeService.loadA[Root](workspace).byName(workspace.rootName).get
    val graph            = htmlExportService.assetWithCompetencyGraph(asset, workspace, root)
    val rootCompetencies = CompetencyUtil.rootCompetencies(root, graph)

    val importValidity = PageSection.parse[List](file, asset, competencies)
    val (_, sections)  = importValidity.getOrElse(throw new UnprocessableEntityException("HTML"))
    val aligned        = PageSection.isAligned(sections)

    val warnings = mutable.ListBuffer.empty[String]
    val ops      = sections.flatMap(
      importOps(_, asset, warnings, graph, rootCompetencies, aligned, commit = true)
    )

    writeService.commit(workspace, ops).get
  end importHtmlDoc

  /** Get the asset title from a write op. */
  private def assetDataTitle(tpe: AssetTypeId, data: Any): String = data match
    case l: Lesson => s"Lesson: ${l.title}"
    case h: Html   => s"Page: ${h.title}"
    case other     => CourseStructureExportService.assetTypeNameMap(tpe) // too hard to get the titles

  /** Write ops to import a page element. */
  def importOps(
    section: PageSection[List],
    parent: Asset[?],
    warnings: mutable.ListBuffer[String],
    graph: TraversedGraph,
    rootCompetencies: Map[UUID, Asset[?]],
    aligned: Boolean,
    commit: Boolean = false
  ): List[WriteOp] =
    section match
      case lesson: LessonSection[List] =>
        importLesson(lesson, parent, warnings, graph, rootCompetencies, aligned, commit)

      case asset: AssetSection[List] =>
        importAsset(asset, parent.right, warnings, graph, rootCompetencies, aligned, commit)

  /** Write ops to create or update a lesson. */
  private def importLesson(
    lesson: LessonSection[List],
    parent: Asset[?],
    warnings: mutable.ListBuffer[String],
    graph: TraversedGraph,
    rootCompetencies: Map[UUID, Asset[?]],
    aligned: Boolean,
    commit: Boolean
  ): List[WriteOp] =
    val (target, updates) = findElement(parent, graph, lesson) match
      case Some(asset) if asset.is[Lesson] =>
        asset.right[UUID] -> aligned ?? realign(asset, warnings, graph, rootCompetencies, lesson)

      case _ =>
        val node = AddNode(Lesson(title = lesson.title))
        val edge = AddEdge(
          sourceName = parent.info.name,
          targetName = node.name,
          group = Elements,
          position = Some(Position.End),
        )
        node.name.left[Asset[?]] -> (node :: edge :: align(node.name, lesson))

    val ops = lesson.assets.flatMap(
      importAsset(
        _,
        target.map(_.asInstanceOf[Asset[?]]),
        warnings,
        graph,
        rootCompetencies,
        aligned,
        commit
      )
    )
    updates ::: ops
  end importLesson

  /** Write ops to create or update an HTML. */
  private def importAsset(
    section: AssetSection[List],
    parent: UUID \/ Asset[?],
    warnings: mutable.ListBuffer[String],
    graph: TraversedGraph,
    rootCompetencies: Map[UUID, Asset[?]],
    aligned: Boolean,
    commit: Boolean,
  ): List[WriteOp] =

    val htmlText = section.paragraphs.mkString("\n")

    val parentName = parent.swap.valueOr(_.info.name)

    parent.toOption.flatMap(findElement(_, graph, section)) match
      case Some(Html.Asset(asset)) =>
        val (wrn, derendered) = derender(htmlText, derenderMap(asset, graph))
        warnings ++= wrn

        @nowarn // md5 deprecated
        val changed = asset.data.source
          .forall(ref => blobService.getBlobMd5(ref) != Hashing.md5.hashString(derendered, StandardCharsets.UTF_8))
        val setData = changed.option(
          editHtmlOp(asset, uploadHtml(derendered, section.title, commit))
        )
        setData ::? aligned ?? realign(asset, warnings, graph, rootCompetencies, section)

      case Some(asset) =>
        val (wrn, derendered) = derender(htmlText, derenderMap(asset, graph))
        warnings ++= wrn

        val setData = for
          blockPart <- asset.instructions
          if blockPart.parts.length == 1
          firstPart <- blockPart.parts.headOption
          firstHtml <- classOf[HtmlPart].option(firstPart)
          html       = firstHtml.html
          if html.replace("\n", "") != derendered.replace("\n", "")
          setData   <- setNodeData(asset, derendered.blockPart)
        yield setData

        setData ::? aligned ?? realign(asset, warnings, graph, rootCompetencies, section)

      case None =>
        val (warnings, derendered) = derender(htmlText, Map.empty)
        val htmlBlockPart          = derendered.blockPart
        val node                   = section.typeId match
          case AssetTypeId.Html                  =>
            AddNode(Html(title = section.title, source = uploadHtml(derendered, section.title, commit)))
          case AssetTypeId.Assessment            =>
            AddNode(Assessment(title = section.title, instructions = htmlBlockPart))
          case AssetTypeId.Assignment            =>
            AddNode(Assignment1(title = section.title, instructions = htmlBlockPart))
          case AssetTypeId.Checkpoint            =>
            AddNode(Checkpoint(title = section.title, instructions = htmlBlockPart))
          case AssetTypeId.Diagnostic            =>
            AddNode(Diagnostic(title = section.title, instructions = htmlBlockPart))
          case AssetTypeId.Discussion            =>
            AddNode(Discussion1(title = section.title, instructions = htmlBlockPart))
          case AssetTypeId.FileBundle            =>
            AddNode(FileBundle(title = section.title))
          // case AssetTypeId.Scorm                    =>
          //   AddNode(Scorm(title = section.title))
          case AssetTypeId.ObservationAssessment =>
            AddNode(ObservationAssessment1(title = section.title, instructions = htmlBlockPart))
          case AssetTypeId.CourseLink            =>
            AddNode(CourseLink(title = section.title, instructions = htmlBlockPart))
          // case AssetTypeId.Lti                      =>
          //    AddNode(Lti(title = section.title))
          case AssetTypeId.PoolAssessment        =>
            AddNode(PoolAssessment(title = section.title, instructions = htmlBlockPart))
          case AssetTypeId.Resource1             =>
            AddNode(Resource1(title = section.title, instructions = htmlBlockPart))
          case unsupported                       =>
            throw new IllegalStateException(s"Unsupported asset type $unsupported")
        val edge                   = AddEdge(
          sourceName = parentName,
          targetName = node.name,
          group = Elements,
          position = Some(Position.End)
        )
        node :: edge :: align(node.name, section)
    end match
  end importAsset

  def setNodeData[A](asset: Asset[A], htmlBlockPart: BlockPart): Option[SetNodeData[A]] =
    asset.withInstructions(htmlBlockPart).map(SetNodeData.fromAsset)

  /** Align new asset with a its competencies. */
  private def align(
    name: UUID,
    section: PageSection[List]
  ): List[WriteOp] =
    List(Teaches -> section.teaches, Assesses -> section.assesses) flatMap { case (group, competencies) =>
      competencies.map(competency => align(name, group, competency))
    }

  /** Realign existing asset with a list of competencies. */
  private def realign(
    element: Asset[?],
    warnings: mutable.ListBuffer[String],
    graph: TraversedGraph,
    rootCompetencies: Map[UUID, Asset[?]],
    section: PageSection[List]
  ): List[WriteOp] =
    List(Teaches -> section.teaches, Assesses -> section.assesses) flatMap { case (group, competencies) =>
      val existing         = CompetencyUtil.assetCompetencies(element, group, graph, rootCompetencies)
      // existing edges
      val edges            = graph.outEdgesInGroup(element, group).toList
      // existing edges that should be retained or removed
      val (retain, remove) = edges.partition(edge => competencies.contains(edge.target.info.name))
      // remove unwanted edges
      val deletes          = remove.map(edge => DeleteEdge(name = edge.name))
      // add new edges
      val adds             = competencies
        .filterNot(uuid => retain.exists(_.target.info.name == uuid))
        .map(competency => align(element.info.name, group, competency))
      adds ::: deletes
    }

  private def align(element: UUID, group: Group, competency: UUID): AddEdge =
    AddEdge(
      sourceName = element,
      targetName = competency,
      group = group,
      position = Some(Position.End)
    )

  /** Find a child element by name ignoring case. */
  private def findElement(
    parent: Asset[?],
    graph: TraversedGraph,
    section: PageSection[List]
  ): Option[Asset[?]] =
    graph
      .targetsInGroup(parent, Elements)
      .find(element =>
        element.info.typeId == section.typeId && element.title.exists(_.trim `equalsIgnoreCase` section.title.trim)
      )

  private def uploadHtml(text: String, title: String, commit: Boolean): Option[BlobRef] =
    val source   = CharSource.wrap(text).asByteSource(StandardCharsets.UTF_8)
    val blobName = blobService.createBlobName(source.openStream(), "authoring/")
    commit.option(
      blobService
        .putBlob(
          blobService.getDefaultProvider,
          blobName,
          title,
          MediaType.TEXT_HTML,
          source.size,
          source
        )
        .get
    )
  end uploadHtml
end HtmlImportService

sealed trait PageImportValidation

final case class InvalidPageImport(
  errors: List[String]
) extends PageImportValidation

final case class ValidPageImport(
  warnings: List[String],
  added: List[String],
  modified: List[String],
  alignmentsAdded: Int,
  alignmentsRemoved: Int,
) extends PageImportValidation

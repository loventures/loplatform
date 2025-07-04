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

package loi.authoring.exchange.imprt.validation

import cats.syntax.option.*
import com.fasterxml.jackson.core.JsonProcessingException
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.service.mime.MimeWebService
import com.learningobjects.cpxp.util.FileUtils
import com.learningobjects.cpxp.util.lookup.FileLookup
import com.learningobjects.de.task.TaskReport
import com.learningobjects.de.web.{InvalidMediaTypeException, MediaType}
import loi.asset.blob.SourceProperty
import loi.asset.contentpart.{BlockPart, HtmlPart}
import loi.asset.course.model.Course
import loi.asset.html.model.Html
import loi.asset.resource.model.Resource1
import loi.asset.root.model.Root
import loi.authoring.AssetType
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.edge.Group
import loi.authoring.exchange.imprt.exception.{MediaTypeParseException, NoMediaTypeForFilenameException}
import loi.authoring.exchange.imprt.store.ImportReceiptDao
import loi.authoring.exchange.imprt.{ImportError, ImportReceipt, ImportTask}
import loi.authoring.exchange.model.*
import loi.authoring.validate.ValidationService
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.asset.edge.EdgeData
import loi.cp.i18n.AuthoringBundle
import org.apache.commons.io.FileUtils as ApacheFileUtils
import org.apache.commons.lang3.StringUtils
import scalaz.std.list.*
import scalaz.std.option.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.traverse.*
import scaloi.syntax.collection.*
import scaloi.syntax.option.*

import java.io.{File, FileInputStream}
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.UUID
import scala.compat.java8.OptionConverters.*
import scala.util.{Failure, Success, Try, Using}

class ValidateImportTask private (
  report: TaskReport,
  receipt: ImportReceipt,
  files: FileLookup,
  manifest: ExchangeManifest,
  targetWs: AttachedReadWorkspace,
)(
  importReceiptDao: ImportReceiptDao,
  mimeService: MimeWebService,
  validationService: ValidationService
) extends ImportTask[ValidatedExchangeManifest](report, receipt)(importReceiptDao):

  override protected def run(): Option[ValidatedExchangeManifest] =

    for
      _                 <- checkVersion()
      trimmedNodes      <- checkRequiredFields()
      _                 <- checkDuplicateIds(trimmedNodes)
      validNodes        <- checkNodes(trimmedNodes)
      _                 <- checkCompIds(validNodes.map(_.id).toSet)
      validatedManifest <- selectRootAndHome(validNodes)
    yield validatedManifest

  private def checkVersion(): Option[Unit] =
    if ExchangeManifest.currentVersion == manifest.version then Some(())
    else
      addError(AuthoringBundle.message("import.versionNotSupported"))
      None

  private def checkRequiredFields(): Option[List[NodeExchangeData]] =

    val trimmedNodes = manifest.nodes.map(node =>
      val trimmedId     = StringUtils.trimToNull(node.id)
      val trimmedTypeId = StringUtils.trimToNull(node.typeId)

      if trimmedId == null then addError(AuthoringBundle.message("import.required.id"))
      else if trimmedTypeId == null then addError(AuthoringBundle.message("import.required.typeId", trimmedId))

      node.copy(id = trimmedId, typeId = trimmedTypeId)
    )

    if hasErrors then None else Some(trimmedNodes.toList)
  end checkRequiredFields

  private def checkDuplicateIds(nodes: Seq[NodeExchangeData]): Option[Unit] =

    val index = nodes.groupBy(_.id)

    index
      .withFilter({ case (_, datas) => datas.length > 1 })
      .foreach({ case (id, datas) =>
        addError(
          AuthoringBundle
            .message("import.duplicateId", id, int2Integer(datas.length))
        )
      })

    if hasErrors then None else Some(())
  end checkDuplicateIds

  private def checkNodes(
    nodes: List[NodeExchangeData]
  ): Option[List[ValidatedNodeExchangeData[?]]] =

    val index = nodes.groupUniqBy(_.id)

    nodes.traverse(node =>
      report.markProgress()
      for
        assetType  <- checkTypeId(node)
        _          <- checkEdgePositions(node)
        validEdges <- checkEdges(node, assetType, index)
        validFile  <- checkFile(node, assetType)
        validNode  <- deserializeAndValidate(assetType, node, validEdges, validFile)
      yield validNode
    )
  end checkNodes

  private def selectRootAndHome(
    nodes: List[ValidatedNodeExchangeData[?]]
  ): Option[ValidatedExchangeManifest] =

    if targetWs.rootName == null then
      // then we are importing a project LOAF into a rootless project
      // we may need to synthesize a root.1 and/or Group.Courses edge
      // we must choose a root and home

      // picking the first root.1/course.1 is not the most intelligent selection
      // people who import LOAFs with multiple such things can go suck an egg
      nodes.find(_.assetType == AssetType[Root]) match
        case Some(root) =>
          root.edges.find(_.group == Group.Courses) match
            case Some(courseEdge) =>
              ValidatedExchangeManifest(nodes, (root.id, courseEdge.target).some, manifest.competencyIds).some
            case None             =>
              addError(AuthoringBundle.message("import.courseRequired"))
              None
        case None       =>
          nodes.find(_.assetType == AssetType[Course]) match
            case Some(course) =>
              val courseEdge = ValidEdgeExchangeData(
                Group.Courses,
                course.id,
                0,
                traverse = true,
                UUID.randomUUID(),
                EdgeData.empty,
                targetExists = true,
                targetInWorkspace = false,
                synthetic = true
              )
              val root       = ValidatedNodeExchangeData(
                UUID.randomUUID().toString,
                AssetType[Root],
                Root(targetWs.projectInfo.name),
                Seq(courseEdge),
                None,
                synthetic = true
              )
              ValidatedExchangeManifest(
                root +: nodes,
                (root.id, course.id).some,
                manifest.competencyIds
              ).some
            case None         =>
              addError(AuthoringBundle.message("import.courseRequired"))
              None
    else
      // else we are importing a .docx or QTI file into a pre-existing aka root-ful project
      // we synthesize nothing and never choose a root and home
      ValidatedExchangeManifest(nodes, None, manifest.competencyIds).some

  private def checkTypeId(node: NodeExchangeData): Option[AssetType[?]] =
    val assetType = AssetType.types.get(AssetTypeId.withName(node.typeId))
    if assetType.isEmpty then addError(AuthoringBundle.message("import.noSuchTypeId", node.typeId, node.id))
    assetType

  private def checkEdges(
    source: NodeExchangeData,
    sourceType: AssetType[?],
    index: Map[String, NodeExchangeData]
  ): Option[Seq[ValidEdgeExchangeData]] =

    var fail = false

    val validEdges = source.edges.flatMap(edge =>
      if !sourceType.edgeRules.contains(edge.group) then
        addError(
          AuthoringBundle
            .message("import.edge.unknownGroup", edge.group.entryName, source.id)
        )
        fail = true
        None
      else if edge.edgeId == null then
        addError(
          AuthoringBundle
            .message("import.required.edgeId", source.id, edge.target, edge.group.entryName)
        )
        fail = true
        None
      else

        val tgtExists =
          edge.targetInWorkspace.option(true) || checkTargetExists(edge.target, source, sourceType, index)

        tgtExists match
          case Some(exists) =>
            val edgeData = Option(edge.edgeData).getOrElse(EdgeData.empty)
            Some(
              ValidEdgeExchangeData(
                edge.group,
                edge.target,
                edge.position,
                edge.traverse,
                edge.edgeId,
                edgeData,
                exists,
                edge.targetInWorkspace,
                synthetic = false,
              )
            )
          case None         =>
            addError(AuthoringBundle.message("import.idNotFound", edge.target, source.id))
            fail = true
            None
        end match
    )

    val edgesByGroupByTarget = validEdges.groupBy(_.group).view.mapValues(_.groupBy(_.target)).toMap
    edgesByGroupByTarget.foreach({ case (group, edgesByTarget) =>
      edgesByTarget
        .filter({ case (_, edges) =>
          edges.size > 1
        })
        .keys
        .foreach(target =>
          fail = true
          addError(
            AuthoringBundle
              .message("import.edge.duplicateTarget", source.id, target, group.entryName)
          )
        )
    })

    if fail then None else Some(validEdges)
  end checkEdges

  private def checkTargetExists(
    targetId: String,
    source: NodeExchangeData,
    sourceType: AssetType[?],
    index: Map[String, NodeExchangeData]
  ): Option[Boolean] =

    val sourceIsBeingCreated      = !manifest.competencyIds.contains(source.id)
    val sourceIsCompetencyIshType = AssetTypeId.CompetencyAndSetTypes.contains(sourceType.id)

    val missingTargetIsRequired = sourceIsBeingCreated || !sourceIsCompetencyIshType

    val target = index.get(targetId)

    if target.isEmpty then
      if missingTargetIsRequired then None
      else Some(false)
    else Some(true)
  end checkTargetExists

  private def checkEdgePositions(source: NodeExchangeData): Option[Unit] =

    var fail = false

    source.edges
      .groupBy(_.group)
      .foreach({ case (group, edges) =>
        // val importedPositions = edges.indices                             // legacy loaf positions look like this
        // val expectedPositions = edges.indices.map(i => i * AssetEdge.Gap) // exchange positions look like this
        val actualPositions = edges.map(_.position).sorted
        // there's no need for positions to be perfect like this
        if actualPositions.exists(_ < 0) || actualPositions.distinct != actualPositions then
          fail = true
          addError(
            AuthoringBundle
              .message(
                "import.edge.invalidPosition",
                group.entryName,
                source.id,
                actualPositions.mkString("[", ", ", "]"),
              )
          )
        end if
      })

    // we can't check `hasErrors` since the report might have errors from other nodes
    if fail then None else Some(())
  end checkEdgePositions

  // None means that `node` has a file/attachment and something is wrong with it, halt
  // Some(None) means that `node` does not have a file/attachment, continue
  // Some(Some) means that `node` has a file/attachment and it is good, continue
  private def checkFile(
    node: NodeExchangeData,
    assetType: AssetType[?]
  ): Option[Option[ValidatedFileData]] =

    def checkFilename(filename: String): Option[Unit] =

      val illegalPrefix = ValidateImportTask.illegalFilenamePrefixes
        .exists(prefix => filename.startsWith(prefix))
      val illegalName   = FileUtils.isIllegalFilename(filename)

      if illegalPrefix || illegalName then
        addError(ImportError.IllegalAssetFilename(filename, node.id))
        None
      else Some(())
    end checkFilename

    def getFile(filename: String): Option[File] =
      val file = files.get(s"attachments/$filename").asScala
      if file.isEmpty then addError(ImportError.MissingAssetFile(filename, node.id))
      file

    def guessContentType(file: File): Option[String] =
      import scalaz.syntax.std.boolean.*
      import scaloi.syntax.option.*
      def probeContentType: Option[String]            = Option(Files.probeContentType(file.toPath))
      def guessContentType: Option[String]            =
        Using.resource(new FileInputStream(file)) { in =>
          Option(URLConnection.guessContentTypeFromStream(in))
        }
      def looksLikeHtmlFragment(str: String): Boolean = str.trim.endsWith("</p>") || str.trim.endsWith("</div>")
      def imagineHtml: Option[String]                 =
        Try(looksLikeHtmlFragment(Files.readString(file.toPath))).toOption.flatMap(_.option("text/html"))
      probeContentType || guessContentType || imagineHtml
    end guessContentType

    def parseMediaType(filename: String, file: File, typeId: String): Option[MediaType] =
      val parsed    = Option(mimeService.getMimeType(filename))
      // if no inferrable content type, use the type id
      val mediaType = parsed.orElse(typeId match
        case "html.1" => Some("text/html")
        case "js.1"   => Some("text/javascript")
        case _        => guessContentType(file))
      if mediaType.isEmpty then addError(NoMediaTypeForFilenameException(filename, node.id).getErrorMessage)
      mediaType.flatMap(mt =>
        try Option(MediaType.parseMediaType(mt))
        catch
          case ex: InvalidMediaTypeException =>
            addError(MediaTypeParseException(mt, node.id, ex).getErrorMessage)
            None
      )
    end parseMediaType

    def checkMediaTypeIsSupported(
      mediaType: MediaType,
      filename: String,
      assetType: AssetType[?]
    ): Option[Unit] =
      if assetType.allowsAttachmentType(mediaType) then Some(())
      else
        addError(
          AuthoringBundle
            .message("import.mediaTypeNotSupported", filename, node.id, mediaType.toString, assetType.id.entryName)
        )
        None

    // Comes from zip manifest. Would like to rename this to node.file,
    // but then we'd have to bump up the zip version.
    node.attachment match
      case Some(filename) =>
        for
          _         <- checkFilename(filename)
          file      <- getFile(filename)
          mediaType <- parseMediaType(filename, file, node.typeId)
          _         <- checkMediaTypeIsSupported(mediaType, filename, assetType)
        yield Option(ValidatedFileData(filename, mediaType, file))

      case None =>
        // `case None` means that the node has no attachment, so validation passes and
        // we return a Some. The Some's value is a None because there is no attachment
        Some(None)
    end match
  end checkFile

  private def deserializeAndValidate(
    assetType: AssetType[?],
    node: NodeExchangeData,
    edges: Seq[ValidEdgeExchangeData],
    attachment: Option[ValidatedFileData]
  ): Option[ValidatedNodeExchangeData[?]] =

    // captures the _ of the assetType in A
    def helper[A](
      assetType: AssetType[A],
      node: NodeExchangeData,
      edges: Seq[ValidEdgeExchangeData]
    ): Option[ValidatedNodeExchangeData[A]] =

      lazy val attemptDeser =
        try Some(JacksonUtils.getFinatraMapper.treeToValue(node.data, assetType.dataClass))
        catch
          case ex: JsonProcessingException =>
            addError(
              AuthoringBundle
                .message("import.node.deserializationFailure", node.id, ex.getMessage)
            )
            None

      def attemptDataVal(data: A) =
        validationService.createValidate(data, None, None)(using assetType) match
          case Success(_)  => Some(())
          case Failure(ex) =>
            addError(AuthoringBundle.message("import.node.validationFailure", node.id, ex.getMessage))
            None

      for
        deserData        <- attemptDeser
        // We will ignore the blobRef from the zip because it could come from a different
        // environment with different available providers and domains. We'll instead
        // make a new blobRef with the node's file/attachment with our current domain id
        // and default provider.
        dataWithoutSource = SourceProperty.putSource(deserData, None)
        _                <- attemptDataVal(dataWithoutSource)
      yield ValidatedNodeExchangeData(
        node.id,
        assetType,
        dataWithoutSource,
        edges,
        attachment,
        synthetic = false
      )
      end for
    end helper

    def rewriteResource1[A](exchange: ValidatedNodeExchangeData[A]): ValidatedNodeExchangeData[?] =
      exchange.data match
        case Resource1(
              title,
              iconAlt,
              duration,
              keywords,
              archived,
              iconCls,
              _,    // ReadingInstructions and ReadingMaterial
              None, // embedCode for video, audio, pdf, etc
              None, // lti
              BlockPart(
                Seq(
                  HtmlPart(
                    html,
                    _
                  )
                ),
                _
              ),
              license,
              author,
              attribution,
              accessRight,
              contentStatus
            ) =>
          val file    = files.createFile(exchange.id)
          val wrapped = if html.contains("</p>") then html else s"<p>$html</p>"
          ApacheFileUtils.writeStringToFile(file, wrapped, StandardCharsets.UTF_8)
          ValidatedNodeExchangeData(
            exchange.id,
            Html.assetTypeForHtml,
            Html(
              title = title,
              iconAlt = iconAlt,
              keywords = keywords,
              archived = archived,
              iconCls = iconCls,
              duration = duration,
              license = license,
              author = author,
              attribution = attribution,
              source = None,
              accessRight = accessRight,
              contentStatus = contentStatus,
            ),
            exchange.edges,
            Some(
              ValidatedFileData(s"${exchange.id}.html", MediaType.TEXT_HTML, file)
            ),
            synthetic = false,
          )

        case _ => exchange

    helper(assetType, node, edges).map(rewriteResource1)
  end deserializeAndValidate

  private def checkCompIds(validNodeIds: Set[String]): Option[Unit] =
    val extraCompIds = manifest.competencyIds.diff(validNodeIds)
    if extraCompIds.nonEmpty then
      addError(AuthoringBundle.message("import.extraCompIds", extraCompIds.mkString("[", ",", "]")))
      None
    else Some(())
end ValidateImportTask

object ValidateImportTask:
  private val illegalFilenamePrefixes = Seq("..", "./", "/")

  def apply(
    receipt: ImportReceipt,
    files: FileLookup,
    manifest: ExchangeManifest,
    targetWs: AttachedReadWorkspace
  )(
    importReceiptDao: ImportReceiptDao,
    mimeService: MimeWebService,
    validationService: ValidationService
  ): ValidateImportTask =
    val report = receipt.report.addChild("Validating Import", manifest.size)
    new ValidateImportTask(report, receipt, files, manifest, targetWs)(importReceiptDao, mimeService, validationService)
  end apply
end ValidateImportTask

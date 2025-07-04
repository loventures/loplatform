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

package loi.authoring.exchange.imprt

import java.io.{File, FileInputStream, InputStream}
import java.net.{URI, URLDecoder}
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.util.UUID
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.{ObjectMapper, ObjectWriter}
import com.learningobjects.cpxp.util.lookup.FileLookup
import com.learningobjects.cpxp.util.{GuidUtil, TempFileMap}
import com.learningobjects.de.task.UnboundedTaskReport
import loi.authoring.edge.Group
import loi.authoring.exchange.imprt.math.MathMLToLatexConverter
import loi.authoring.exchange.model.{EdgeExchangeData, ExchangeManifest, NodeExchangeData}
import loi.cp.asset.edge.EdgeData
import loi.cp.i18n.BundleMessage
import org.apache.commons.lang3.StringUtils
import org.log4s.Logger
import scalaz.syntax.`validation`.*
import scalaz.{NonEmptyList, Validation, ValidationNel}

import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try, Using}
import scala.xml.transform.{RewriteRule, RuleTransformer}
import scala.xml.{Elem, Node, XML}

object ImporterUtils:
  private val logger: Logger = org.log4s.getLogger

  def buildEmptyEdgesFromAssets(assets: Seq[NodeExchangeData], group: Group): Seq[EdgeExchangeData] =
    assets.zipWithIndex.map { case (asset, position) =>
      EdgeExchangeData(group, asset.id, position, traverse = true, UUID.randomUUID(), EdgeData.empty)
    }

  def buildEmptyEdgesFromAssets(
    assets: Seq[NodeExchangeData],
    group: Group,
    edgeMap: Map[String, UUID]
  ): Seq[EdgeExchangeData] =
    assets.zipWithIndex.map { case (asset, position) =>
      EdgeExchangeData(group, asset.id, position, traverse = true, edgeMap(asset.id), EdgeData.empty)
    }

  def guid: String = UUID.randomUUID().toString

  def writeManifestFile(manifest: ExchangeManifest, mapper: ObjectMapper): File =
    val writer: ObjectWriter = mapper.writer(new DefaultPrettyPrinter())
    val manifestFile         = File.createTempFile("importer", ".tmp")
    writer.writeValue(manifestFile, manifest)
    manifestFile

  def writeExchangeZip(outputPath: Path, manifestFile: File, lookup: FileLookup): Unit =
    def copyAttachment(key: String, attachmentsDirectory: Path) =
      val file           = lookup.get(key).get()
      val oldPath        = file.toPath
      val attachmentPath = attachmentsDirectory.resolve(key)
      try
        /* Create all parent directories. */
        Files.createDirectories(attachmentPath.getParent)
        /* Copy the attachment. */
        Files.copy(oldPath, attachmentPath)
      catch
        case _: FileAlreadyExistsException =>
          // NOOP, just don't want to blow up
          logger.warn(s"Tried to copy $oldPath to $attachmentPath but file already exists. Ignoring.")
    end copyAttachment

    val ignoreFileTypes = Set(".cnxml", ".xml", ".qti")
    val env             = Map("create" -> "true")
    val uri             = URI.create(s"jar:file:$outputPath")
    val zipfs           = FileSystems.newFileSystem(uri, env.asJava)
    try
      val manifestJsonPath     = manifestFile.toPath
      val pathInZipfile        = zipfs.getPath("/manifest.json")
      val attachmentsDirectory = Files.createDirectory(zipfs.getPath("attachments/"))
      Files.copy(manifestJsonPath, pathInZipfile, StandardCopyOption.REPLACE_EXISTING)
      lookup
        .keySet()
        .asScala
        .filterNot(_.startsWith("__MACOSX"))
        .filter(k => !ignoreFileTypes.exists(t => k.endsWith(t)))
        .foreach(key => copyAttachment(key, attachmentsDirectory))
    finally if zipfs != null then zipfs.close()
    end try
  end writeExchangeZip

  def createValidatingReport: UnboundedTaskReport =
    val report = new UnboundedTaskReport("Validating Zip File")
    report.markStart()
    report

  // Wraps TempFileMap.importZip in a Try -> Validation
  def readZip(
    typ: ThirdPartyImportType,
    files: TempFileMap,
    unconvertedStream: InputStream
  ): ValidationNel[BundleMessage, Unit] =
    Try(files.importZip(unconvertedStream)) match
      case Success(_) => ().successNel[BundleMessage]
      case Failure(e) => ImportError.CannotReadZip(typ, e).failureNel

  // Wraps XML.load in a Try -> Validation
  def loadXml(
    file: File,
    typ: ThirdPartyImportType
  ): Validation[NonEmptyList[BundleMessage], Elem] =
    Using.resource(new FileInputStream(file)) { stream =>
      Try(XML.load(stream)) match
        case Success(xml) => xml.successNel[BundleMessage]
        case Failure(e)   => ImportError.InvalidManifest(file.getName, typ, e).failureNel[Elem]
    }

  def overwriteManifestInExchangeZip(manifest: ExchangeManifest, outputPath: Path, mapper: ObjectMapper): Unit =
    val manifestFile = writeManifestFile(manifest, mapper)
    val env          = Map("create" -> "true")
    val uri          = URI.create(s"jar:file:$outputPath")
    val zipfs        = FileSystems.newFileSystem(uri, env.asJava)
    try
      val manifestJsonPath = manifestFile.toPath
      val pathInZipfile    = zipfs.getPath("/manifest.json")
      Files.copy(manifestJsonPath, pathInZipfile, StandardCopyOption.REPLACE_EXISTING)
    finally
      if manifestFile.exists() then manifestFile.delete()
      if zipfs != null then zipfs.close()
  end overwriteManifestInExchangeZip

  def cleanUpXml(xml: String): String =
    xml.trim
      .replaceAll("\\s+", " ")
      .replaceAll(" style=\"[^\"]*\"", "")
      .replaceAll(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"", "")
      .replaceAll(" xmlns=\"http://www.imsglobal.org/xsd/imsqti_v2p1\"", "")

  def convertMath(node: Node): Node =
    MathRewriter(node)

  def createTempFilePath(filename: String): Path =
    val cleanName = deleteWhitespace(filename)
    Paths.get(System.getProperty("java.io.tmpdir"), s"${GuidUtil.shortGuid()}_$cleanName")

  def deleteWhitespace(str: String): String =
    StringUtils.deleteWhitespace(URLDecoder.decode(str, StandardCharsets.UTF_8.displayName()))

  private object MathRewriteRule extends RewriteRule:
    override def transform(n: Node): Seq[Node] =
      n match
        case math: Elem if math.label == "math" =>
          val span = MathMLToLatexConverter.convert(math)
          XML.loadString(span)
        case _                                  => n

  private object MathRewriter extends RuleTransformer(MathRewriteRule)
end ImporterUtils

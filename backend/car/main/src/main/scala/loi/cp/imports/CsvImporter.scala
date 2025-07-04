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

package loi.cp.imports

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.google.common.cache.CacheBuilder
import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.ApiQuery
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.component.web.{ErrorResponse, FileResponse, WebRequest, WebResponse}
import com.learningobjects.cpxp.component.{AbstractComponent, ComponentService, ComponentSupport}
import com.learningobjects.cpxp.controller.upload.{UploadInfo, Uploader, Uploads}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainWebService}
import com.learningobjects.cpxp.service.exception.{RestErrorType, RestExceptionInterface}
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.GuidUtil
import com.learningobjects.cpxp.web.ExportFile
import fs2.*
import fs2.io.file.{Files, Path}
import jakarta.servlet.http.HttpServletRequest
import kantan.csv.*
import kantan.csv.ops.*
import loi.cp.imports.CsvImporterComponent.{CsvValidationStatus, ImportDTO, MatchError}
import loi.cp.tx.DEIEIO
import org.apache.commons.lang3.StringUtils
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.either.*
import scalaz.syntax.std.option.*
import scalaz.{-\/, \/, \/-}
import scaloi.json.ArgoExtras

import java.io.File
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.*

@Component
class CsvImporter(implicit
  domain: DomainDTO,
  user: UserDTO,
  executionContext: ExecutionContext,
  cs: ComponentService,
  facadeService: FacadeService,
  importCoordinator: ImportCoordinator,
  domainWebService: DomainWebService,
) extends AbstractComponent
    with CsvImporterComponent:

  import CsvImporter.*
  import errors.*

  type CsvRow = Seq[String]

  override def startImport(req: HttpServletRequest): MatchError.type \/ ImportComponent =
    val importDto = isJson(req) option parseImportDto(req)
    val upload    = importDto.cata(dto => Uploads.retrieveUpload(dto.uploadGuid), parseUpload(req))

    logger.info(s"got upload: ${upload.getFileName} - ${upload.getSize}")
    val header = readHeader(upload)

    for
      importType <- importDto.cata(getImportType, guessImportType(header)) \/> MatchError
      importer   <- getImporterForType(importType) \/> MatchError
    yield
      // todo: allow a way for clients to "identify" their imports?
      val importIdentifier = Option.empty

      /* there is a way to do this ref/deref with streams that is elegant,
       * simple, pure, functional, and very very wrong. */
      upload.ref()
      val result =
        importCoordinator.importStream(
          getItems(upload)(importer),
          Some(importType.impl.getName),
          user,
          importIdentifier,
          Some(header.lineNumber.toLong + 1L)
        )(_ => upload.deref())

      result.setImportFileFromUpload(upload)
      result
    end for
  end startImport

  private def isJson(req: HttpServletRequest): Boolean =
    MediaType.JSON_UTF_8.is(MediaType.parse(req.getContentType))

  private def parseImportDto(req: HttpServletRequest): ImportDTO =
    JacksonUtils.getMapper.readValue(req.getInputStream, classOf[ImportDTO])

  private def parseUpload(req: HttpServletRequest): UploadInfo =
    Uploader.parse(req).getUploads.iterator.next

  override def validateImportFile(dto: ImportDTO): ErrorResponse \/ String =
    val upload = Uploads.retrieveUpload(dto.uploadGuid)
    val header = readHeader(upload)

    getImportType(dto) match
      case None             =>
        ErrorResponse.badRequest.copy(body = Some(MatchError)).left
      case Some(importType) =>
        val validationToken   = GuidUtil.guid()
        statuses.put(validationToken, CsvValidationStatus.InProgress.widen.right)
        val importer          =
          ComponentSupport.lookup(classOf[Importer[ImportItem]], importType.impl)
        val items             = getItems(upload)(importer)
        val validationProcess =
          importCoordinator.validateStream(items = items, indexOffset = Some(header.lineNumber.toLong + 1L))

        val doValidate: IO[Option[StreamStatusReport]] =
          DEIEIO.tx(validationProcess.compile.last)

        upload.ref()
        doValidate
          .flatMap(_.fold[IO[StreamStatusReport]](IO.raiseError(NoStreamValueException))(IO.pure))
          .map(CsvValidationStatus.Finished(_, importType).widen)
          .unsafeRunAsync { result =>
            upload.deref()
            statuses.put(validationToken, result.toDisjunction)
          }
        validationToken.right
    end match
  end validateImportFile

  override def downloadErrors(importId: Long, request: WebRequest): ErrorResponse \/ WebResponse =
    for importComponent <- importId.component_?[ImportComponent] \/> ErrorResponse.notFound
    yield
      val out = ExportFile.create("errors.csv", MediaType.CSV_UTF_8, request)
      val res = importComponent
        .getErrors(ApiQuery.ALL)
        .asScala
        .flatMap(error => error.getMessages.map(msg => ErrorRow(error.getIndex, msg)))
        .toList
      out.file.writeCsv(res, rfc.withHeader("Line Number", "Message"))
      FileResponse(out.toFileInfo)

  def getValidationStatus(token: String): ErrorResponse \/ CsvValidationStatus =
    Option(statuses.getIfPresent(token)) match
      case None         => ErrorResponse.notFound(token).left
      case Some(-\/(e)) => ErrorResponse.badRequest.copy(body = Some(e)).left
      case Some(\/-(r)) => r.right

  def getItems(upload: UploadInfo)(i: Importer[ImportItem]): Stream[IO, GenericError \/ ImportItem] =
    val header     = readHeader(upload)
    val fileStream = getFileStream(header.lineNumber, upload.getFile)
    fileStream
      .filter(StringUtils.isNotBlank)
      .map(_.split(",", -1).toList)
      .map(deserializeRow(i)(header.values))

  def deserializeRow(importer: Importer[ImportItem])(header: CsvRow)(row: CsvRow): GenericError \/ ImportItem =
    importer.deserializeCsvRow(header, row)

  private def readHeader(upload: UploadInfo): Header =
    val fileReader = scala.io.Source.fromFile(upload.getFile).bufferedReader()
    try
      var line   = 1
      var header = fileReader.readLine()
      while header.trim == "" do
        header = fileReader.readLine()
        line += 1
      val raw    = header.split(",").toList
      Header(raw, line)
    finally fileReader.close()
  end readHeader

  def getFileStream(headerLine: Int, file: File): Stream[IO, String] =
    Stream.emits(
      Files[IO]
        .readAll(Path.fromNioPath(file.toPath)) // TODO: Parameterize a dedicated IO pool.
        .through(text.utf8.decode)
        .through(text.lines)
        .drop(headerLine)
        .compile
        .toList
        .unsafeRunSync()
    )

  def getImportType(dto: ImportDTO): Option[ImportType] =
    val upload  = Uploads.retrieveUpload(dto.uploadGuid)
    val header  = readHeader(upload)
    val headers = lower(header.values.toSet)
    importCoordinator.getImportTypes.toSeq
      .find { case (importType, importer) =>
        importType.impl.getName == dto.impl &&
        (lower(importer.requiredHeaders) == headers || lower(importer.allHeaders).intersect(headers) == headers)
      }
      .map(tuple => tuple._1)
  end getImportType
end CsvImporter

object CsvImporter:

  private val logger = org.log4s.getLogger

  /** Determines an import 'type' for a set of attributes.
    *
    * @param header
    * @param importCoordinator
    * @return
    */
  def guessImportType(header: Header)(implicit importCoordinator: ImportCoordinator): Option[ImportType] =
    val headers       = lower(header.values.toSet)
    val possibleTypes = importCoordinator.getImportTypes.toSeq.filter { case (importType, importer) =>
      lower(importer.requiredHeaders) == headers || lower(importer.allHeaders).intersect(headers) == headers
    }

    if possibleTypes.length == 1 then possibleTypes.headOption.map(_._1) else None

  /** lowercases elements in Set[String]
    */
  private def lower(s: Set[String]): Set[String] = s.map(_.toLowerCase())

  def getImporterForType(importType: ImportType): Option[Importer[ImportItem]] =
    Option(ComponentSupport.lookup(classOf[Importer[ImportItem]], importType.impl))

  /** Represents a header in a Csv File.
    *
    * @param values
    *   a list of comma-delimited headers in the Csv File
    * @param lineNumber
    *   the line number that the header was located in
    */
  case class Header(values: Seq[String], lineNumber: Int)

  case class ErrorRow(lineNumber: Long, message: String)

  given HeaderEncoder[ErrorRow] = HeaderEncoder.caseEncoder(
    "Line Number",
    "Message"
  )(ArgoExtras.unapply)

  object NoStreamValueException
      extends RuntimeException("validation incomplete; see server logs")
      with RestExceptionInterface:
    def getErrorType      = RestErrorType.UNEXPECTED_ERROR
    def getHttpStatusCode = 500
    def getJson           = null

  private val statuses = CacheBuilder
    .newBuilder()
    .expireAfterAccess(10, TimeUnit.MINUTES)
    .build[String, Throwable \/ CsvValidationStatus]
end CsvImporter

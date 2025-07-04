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

package com.learningobjects.cpxp.component.web

import argonaut.EncodeJson
import com.google.common.net.{HttpHeaders, MediaType}
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.util.*
import com.learningobjects.cpxp.service.attachment.Disposition
import com.learningobjects.cpxp.util.{FileInfo, LocalFileInfo, PathFileInfo}
import jakarta.servlet.http.HttpServletResponse
import loi.cp.Widen
import org.apache.commons.text.StringEscapeUtils
import org.apache.http.HttpStatus
import scalaz.std.string.*
import scaloi.syntax.OptionOps.*

import java.io.{File, InputStream}
import java.net.URI
import java.net.URLConnection.guessContentTypeFromName
import java.nio.file.Paths
import java.time.Instant
import java.util.Date
import scala.concurrent.duration.Duration
import scala.language.implicitConversions
import scala.xml.Elem

/** This type represents a non-entity response to a Web request.
  */
sealed trait WebResponse extends HttpResponse with Widen[WebResponse]

/** When a response has already been provided as a side-effect; i.e. do nothing. */
case object NoResponse extends WebResponse:
  def instance = NoResponse

  val statusCode                   = 0 // this could go away if i removed statusCode as the definition of an HttpResponse
  val headers: Map[String, String] = Map()

/** No content response. */
case object NoContentResponse extends WebResponse:
  def instance = NoContentResponse

  val statusCode                   = HttpServletResponse.SC_NO_CONTENT
  val headers: Map[String, String] = Map()

/** A response that writes directly to a Tomcat response object. */
final case class DirectResponse(
  statusCode: Int = 200,
  headers: Map[String, String] = Map.empty,
)(val respond: HttpServletResponse => Unit)
    extends WebResponse
object DirectResponse:
  def apply(respond: HttpServletResponse => Unit): DirectResponse =
    DirectResponse()(respond)

/** Issues a redirect. */
final case class RedirectResponse(url: String, statusCode: Int, headers: Map[String, String] = Map())
    extends WebResponse

object RedirectResponse:
  def apply(url: String, statusCode: Int) =
    new RedirectResponse(url, statusCode)
  def permanent(url: String)              =
    new RedirectResponse(url, HttpServletResponse.SC_MOVED_PERMANENTLY)
  def permanent(uri: URI)                 =
    new RedirectResponse(uri.toString, HttpServletResponse.SC_MOVED_PERMANENTLY)
  def temporary(url: String)              =
    new RedirectResponse(url, HttpServletResponse.SC_MOVED_TEMPORARILY)
  def temporary(uri: URI)                 =
    new RedirectResponse(uri.toString, HttpServletResponse.SC_MOVED_TEMPORARILY)
end RedirectResponse

/** Dispatches to a servlet container-managed URL. */
final case class DispatchResponse(url: String, statusCode: Int, headers: Map[String, String] = Map())
    extends WebResponse

object DispatchResponse:
  def apply(url: String) = new DispatchResponse(url, HttpServletResponse.SC_OK)

/** Delivers a HTTP error. */
final case class ErrorResponse(statusCode: Int, headers: Map[String, String] = Map(), body: Option[AnyRef] = None)
    extends WebResponse

object ErrorResponse:
  import HttpServletResponse.*

  def badRequest                                                     = ErrorResponse(SC_BAD_REQUEST)
  def badRequest(body: AnyRef)                                       = ErrorResponse(SC_BAD_REQUEST, body = Option(body))
  def conflict                                                       = ErrorResponse(SC_CONFLICT)
  def serverError                                                    = ErrorResponse(SC_INTERNAL_SERVER_ERROR)
  def serverError(body: AnyRef)                                      = ErrorResponse(SC_INTERNAL_SERVER_ERROR, body = Option(body))
  def internalError(th: Throwable)                                   = serverError(th.getMessage)
  def notFound                                                       = ErrorResponse(SC_NOT_FOUND)
  def notFound(body: AnyRef)                                         = ErrorResponse(SC_NOT_FOUND, body = Option(body))
  def forbidden                                                      = ErrorResponse(SC_FORBIDDEN)
  def forbidden(body: AnyRef)                                        = ErrorResponse(SC_FORBIDDEN, body = Option(body))
  // this sends a "FormBased" x-www-authenticate header, maybe parameterize it
  def unauthorized                                                   = ErrorResponse(SC_UNAUTHORIZED)
  def unacceptable                                                   = ErrorResponse(SC_NOT_ACCEPTABLE)
  def unacceptable(body: AnyRef)                                     = ErrorResponse(SC_NOT_ACCEPTABLE, body = Option(body))
  def unprocessable                                                  = ErrorResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY)
  def unprocessable(body: AnyRef)                                    = ErrorResponse(HttpStatus.SC_UNPROCESSABLE_ENTITY, body = Option(body))
  def methodNotAllowed                                               = ErrorResponse(SC_METHOD_NOT_ALLOWED)
  def validationError(property: String, value: Any)(message: String) =
    ErrorResponse(SC_BAD_REQUEST, body = Some(ValidationError(property, value, message)))
end ErrorResponse

// shorter syntax
final class ErrorResponseOps(private val self: AnyRef) extends AnyVal:
  def to400: ErrorResponse = ErrorResponse.badRequest(self)
  def to403: ErrorResponse = ErrorResponse.forbidden(self)
  def to404: ErrorResponse = ErrorResponse.notFound(self)
  def to422: ErrorResponse = ErrorResponse.unprocessable(self)

object ErrorResponseOps extends ToErrorResponseOps

trait ToErrorResponseOps:
  implicit def toErrorResponseOps(value: AnyRef): ErrorResponseOps = new ErrorResponseOps(value)

/** Simple validatation error structure. Value must be Jackson-serializable. */
final case class ValidationError(property: String, value: Any, message: String, _type: String = "ValidationError")

/** Delivers HTML content. */
final case class HtmlResponse[H <: Html](html: H, statusCode: Int, headers: Map[String, String] = Map())
    extends WebResponse

object HtmlResponse:
  def apply[H <: Html](html: H, statusCode: Int) =
    new HtmlResponse(html, statusCode)

  /** Construct a successful html response from html content. */
  def apply[H <: Html](html: H): HtmlResponse[H] =
    new HtmlResponse(html, HttpServletResponse.SC_OK)

  /** Construct a successful html response from raw html. */
  def apply(html: String): HtmlResponse[RawHtml] =
    new HtmlResponse(RawHtml(html), HttpServletResponse.SC_OK)

  /** Construct a successful html response from XML html. */
  def apply(html: Elem): HtmlResponse[RawHtml] =
    new HtmlResponse(RawHtml(html.toString), HttpServletResponse.SC_OK)

  /** Construct a successful html response from dynamically rendered html. */
  def apply(renderable: Renderable): HtmlResponse[DynamicHtml] =
    new HtmlResponse(DynamicHtml(renderable), HttpServletResponse.SC_OK)

  /** Construct a successful html response from a html template. */
  def apply(context: AnyRef, template: String): HtmlResponse[HtmlTemplate] =
    new HtmlResponse(HtmlTemplate(context, template), HttpServletResponse.SC_OK)

  /** Construct a successful html response from a html template. */
  def apply(context: AnyRef, ci: Class[? <: ComponentInterface], template: String): HtmlResponse[HtmlTemplate] =
    new HtmlResponse(HtmlTemplate(context, ci, template), HttpServletResponse.SC_OK)
end HtmlResponse

/** Delivers a file. */
final case class FileResponse[F <: FileInfo](fileInfo: F, statusCode: Int, headers: Map[String, String] = Map())
    extends WebResponse

object FileResponse:
  type Any = FileResponse[? <: FileInfo]

  def apply[F <: FileInfo](fileInfo: F)                                                                       =
    new FileResponse(fileInfo, HttpServletResponse.SC_OK)
  def apply(file: File)                                                                                       =
    new FileResponse(
      localFileInfo(
        file = file,
        fileName = file.getName,
        mediaType = Option(guessContentTypeFromName(file.getName)).map(MediaType.parse),
        lastModified = new Date(file.lastModified),
        Disposition.attachment
      ),
      statusCode = HttpServletResponse.SC_OK
    )
  def apply(file: File, fileName: String, mediaType: MediaType, lastModified: Date, disposition: Disposition) =
    new FileResponse(
      localFileInfo(file, fileName, Some(mediaType), lastModified, disposition),
      HttpServletResponse.SC_OK
    )

  /** Construct a [[FileResponse]] that serves a resource of the given class.
    */
  def resource(
    ctx: AnyRef,
    rsrc: String,
    statusCode: Int = HttpServletResponse.SC_OK,
    mediaType: Option[MediaType] = None,
    headers: Map[String, String] = Map.empty
  ) =
    val info = new PathFileInfo(Paths.get(ctx.getClass.getResource(rsrc).toURI))
    info.setContentType(mediaType.fold(guessContentTypeFromName(info.path.getFileName.toString))(_.toString))
    info.setDisposition(Disposition.attachment, rsrc)
    FileResponse[PathFileInfo](
      fileInfo = info,
      statusCode = statusCode,
      headers = headers,
    )
  end resource

  private def localFileInfo(
    file: File,
    fileName: String,
    mediaType: Option[MediaType],
    lastModified: Date,
    disposition: Disposition
  ): LocalFileInfo =
    val info = new LocalFileInfo(file)
    mediaType.map(_.toString).foreach(info.setContentType)
    info.setLastModified(lastModified)
    info.setDisposition(disposition, fileName)
    info
  end localFileInfo
end FileResponse

/** Delivers text content. */
final case class TextResponse(text: String, mediaType: MediaType, statusCode: Int, headers: Map[String, String] = Map())
    extends WebResponse:
  def +(header: (String, String)): TextResponse                          = copy(headers = headers + header)
  def disposed(fileName: String, disposition: Disposition): TextResponse =
    this + (HttpHeaders.CONTENT_DISPOSITION -> s"""${disposition.name}; filename="$fileName"""") // TODO: international encoding from HttpUtils
  def cached(now: Instant, duration: Duration): TextResponse             =
    this + (HttpHeaders.CACHE_CONTROL       -> s"max-age=${duration.toSeconds}") + (HttpHeaders.PRAGMA -> "cache") +
      (HttpHeaders.EXPIRES                  -> s"${now.plusMillis(duration.toMillis).toEpochMilli}")

object TextResponse:
  import HttpServletResponse.*
  def of(text: String, tpe: MediaType) = TextResponse(text, tpe, SC_OK)
  def plain(text: String)              = TextResponse(text, MediaType.PLAIN_TEXT_UTF_8, SC_OK)
  def xml(text: String)                = TextResponse(text, MediaType.XML_UTF_8, SC_OK)
  def json(text: String)               = TextResponse(text, MediaType.JSON_UTF_8, SC_OK)
  def xml(xml: Elem)                   = TextResponse(s"""<?xml version="1.0" encoding="UTF-8"?>\n$xml""", MediaType.XML_UTF_8, SC_OK)

  def csvDownload(fileName: String)(rows: Seq[Seq[Any]]) =
    TextResponse(CsvEncode(rows), MediaType.CSV_UTF_8, HttpServletResponse.SC_OK)
      .disposed(fileName, Disposition.attachment)
  def csvDownload(fileName: String, document: String)    =
    TextResponse(document, MediaType.CSV_UTF_8, HttpServletResponse.SC_OK).disposed(fileName, Disposition.attachment)
end TextResponse

/** Delivers an InputStream. */
final case class StreamResponse[S <: InputStream](stream: S, statusCode: Int, headers: Map[String, String] = Map())
    extends WebResponse

object StreamResponse:
  import HttpServletResponse.*
  def of(s: InputStream) = StreamResponse(s, SC_OK)

/** An arbitrary JSON-serializable response. */
final case class EntityResponse[E <: AnyRef](
  entity: E,
  statusCode: Int = HttpServletResponse.SC_OK,
  headers: Map[String, String] = Map()
) extends WebResponse

//TODO: Name
final case class ArgoResponse[E](
  entity: E,
  statusCode: Int = HttpServletResponse.SC_OK,
  headers: Map[String, String] = Map()
)(implicit val encoder: EncodeJson[E])
    extends WebResponse

private[web] object CsvEncode:
  def apply(rows: Seq[Seq[Any]]): String = rows.map(encodeRow).mkString("\r\n")

  private def encodeRow(row: Seq[Any]) =
    row.map(Option.apply).map(_.foldZ(_.toString)).map(StringEscapeUtils.escapeCsv).mkString(",")

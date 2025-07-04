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

package com.learningobjects.cpxp.web

import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.StringConvert
import com.learningobjects.cpxp.component.web.WebRequest
import com.learningobjects.cpxp.component.web.converter.StringConverter
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.service.attachment.Disposition
import com.learningobjects.cpxp.util.{GuidUtil, LocalFileInfo}
import jakarta.servlet.http.*
import scaloi.syntax.any.*

import java.io.{File, Serializable}
import java.util.Date

/** Container class for capturing information about a transient export file. This class is typically stored in the
  * client's HTTP session; the associated file will be destroyed when the session is terminated, when this container is
  * garbage collected or when the appserver shuts down.
  */
@StringConvert(`using` = classOf[ExportFileConverter])
class ExportFile(
  /** The export filename. */
  val fileName: String,

  /** The export mime type. */
  val mediaType: MediaType
) extends HttpSessionBindingListener
    with Serializable:

  /** A random GUID for this export. */
  val guid = GuidUtil.guid

  /** The temporary file associated with this export. */
  val file = File.createTempFile("export", ".tmp")

  // Delete on appserver exit
  file.deleteOnExit()

  /** When this export container was created. */
  val created: Date = new Date

  private[web] var deleted = false

  /** Delete on garbage collect. */
  override def finalize() = delete()

  /** Nop when associated with HTTP session. */
  override def valueBound(e: HttpSessionBindingEvent): Unit = ()

  /** Delete on session expiry. */
  override def valueUnbound(e: HttpSessionBindingEvent): Unit = delete()

  private def delete(): Unit =
    if !deleted then
      deleted = true
      file.delete()

  /** Bind this export file in a session. */
  def bind(session: HttpSession): Unit =
    session.setAttribute(ExportFile.sessionKey(guid), this)

  /** Convert to a file info, deferring garbage collection cleanup. */
  def toFileInfo: LocalFileInfo =
    new LocalFileInfo(file, () => delete()) <| { info =>
      info.setContentType(mediaType.toString)
      info.setLastModified(new Date(file.lastModified))
      info.setDisposition(Disposition.attachment, fileName)
    }

  /** Convert to an upload info, deferring garbage collection cleanup. */
  def toUploadInfo: UploadInfo = new ExportUploadInfo

  // inner class that hold a reference to the [ExportFile] to defer garbage collection-induced deletion
  private class ExportUploadInfo extends UploadInfo(fileName, mediaType.toString, file, false)
end ExportFile

object ExportFile:

  def cleanFilename(filename: String): String =
    InvalidCharacters.replaceAllIn(filename, "")

  private final val InvalidCharacters = """([ .]+$)|[/<>:"|?*\\]""".r

  /** Create and bind to a session. */
  def create(fileName: String, mediaType: MediaType, request: WebRequest): ExportFile =
    new ExportFile(cleanFilename(fileName), mediaType) <| {
      _.bind(request.getRawRequest.getSession)
    }

  /** Look up an export file in the user's HTTP session by GUID. */
  def lookup(guid: String, request: HttpServletRequest): Option[ExportFile] =
    Option(request.getSession) flatMap { s =>
      Option(s.getAttribute(sessionKey(guid)).asInstanceOf[ExportFile])
    }

  /** The session key for an export GUID. */
  private def sessionKey(guid: String) = s"export:$guid"
end ExportFile

class ExportFileConverter(request: => HttpServletRequest) extends StringConverter[ExportFile]:
  override def apply(input: StringConverter.Raw[ExportFile]): Option[ExportFile] =
    ExportFile.lookup(input.value, request).filterNot(_.deleted)

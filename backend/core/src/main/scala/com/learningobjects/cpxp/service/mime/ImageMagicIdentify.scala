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

package com.learningobjects.cpxp.service.mime

import com.learningobjects.cpxp.util.FileInfo
import com.learningobjects.de.web.MediaType
import scaloi.syntax.OptionOps.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future, TimeoutException, blocking}
import scala.sys.process.{Process, ProcessLogger}
import scala.util.Try

object ImageMagickIdentify:

  private val log = org.log4s.getLogger

  // the media type to use for a result of running `identify`. The keys of this map are
  // the output of the `identify -format %m` command. I think the choices of keys can
  // be seen with `identify -list coder`, but I just ran `identify` on some sample files
  // to get this list, and then added in `BMP2` for fun.
  private val mediaTypes: Map[String, MediaType] = Map(
    "JPEG" -> MediaType.IMAGE_JPEG,
    "GIF"  -> MediaType.IMAGE_GIF,
    "PNG"  -> MediaType.IMAGE_PNG,
    "BMP"  -> MediaType.IMAGE_BMP,
    "BMP2" -> MediaType.IMAGE_BMP,
    "BMP3" -> MediaType.IMAGE_BMP,
    "SVG"  -> MediaType.APPLICATION_SVG
  )

  def identify(info: FileInfo, attachmentId: Long): Option[MediaType] =
    identify(info).fold(
      {
        case _: TimeoutException   =>
          log.warn(s"Timed out trying to identify image file format for attachment $attachmentId")
          None
        case ex: UnknownFileFormat =>
          log.warn(s"${ex.getMessage} for attachment $attachmentId")
          None
        case ex: Throwable         =>
          log.warn(ex)(s"Failed to identify image file format for attachment $attachmentId")
          None
      },
      Some.apply
    )

  def identify(info: FileInfo): Try[MediaType] =

    val resultBuffer = new StringBuffer()
    val errorBuffer  = new StringBuffer()

    val logger  = ProcessLogger(s => resultBuffer.append(s), s => errorBuffer.append(s))
    val process = Process("identify -format %m -") #< info.openInputStream() run logger

    val exitValue = Try {
      val exitVal = Await.result(Future(blocking(process.exitValue())), 30.seconds)
      if exitVal != 0 then throw new RuntimeException(errorBuffer.toString.trim())
    }

    process.destroy()

    exitValue.flatMap(_ =>
      val format = resultBuffer.toString.trim
      mediaTypes.get(format).toTry(UnknownFileFormat(format))
    )
  end identify
end ImageMagickIdentify

case class UnknownFileFormat(format: String) extends RuntimeException(s"Unknown 'identify %m' format: $format")

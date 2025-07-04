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

package loi.cp.email

import java.io.File
import javax.mail.Part
import javax.mail.internet.MimeBodyPart

/** Describes a class that can convert some representation of an attachment into a MIME body part attachment.
  *
  * @tparam A
  *   the type of entity that can be attached
  */
trait CanAttach[A]:

  /** Convert an attachment into a MIME body part attachment.
    * @param attachment
    *   the entity to attach
    * @return
    *   the MIME body part
    */
  def attachmentPart(attachment: A): MimeBodyPart

object CanAttach:

  def forFile(fileName: String, mimeType: String): CanAttach[File] = (file: File) =>
    val part = new MimeBodyPart()
    part.attachFile(file)
    part.setDisposition(Part.ATTACHMENT)
    part.setFileName(fileName) // file.getName is likely some random temp name
    part.setHeader("Content-Type", mimeType)
    part

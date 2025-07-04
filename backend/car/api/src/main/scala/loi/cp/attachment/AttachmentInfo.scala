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

package loi.cp.attachment

import argonaut.CodecJson
import com.learningobjects.cpxp.Id

import java.lang
import java.time.Instant

/** An attachment for a piece of lightweight content.
  *
  * @param id
  *   the persistence identifier of the attachment
  * @param fileName
  *   the name of the file
  * @param size
  *   the size of the file
  * @param mimeType
  *   the mime type of the attachment
  * @param createDate
  *   the date the attachment was created
  */
case class AttachmentInfo(id: AttachmentId, fileName: String, size: Long, mimeType: String, createDate: Instant)
    extends Id:
  override def getId: lang.Long = id.value

object AttachmentInfo:
  import scaloi.json.ArgoExtras.*
  implicit val codec: CodecJson[AttachmentInfo] = CodecJson.derive[AttachmentInfo]

  def apply(attachment: AttachmentComponent): AttachmentInfo =
    AttachmentInfo(
      AttachmentId(attachment.getId),
      attachment.getFileName,
      attachment.getSize,
      attachment.getMimeType,
      attachment.getCreateTime.toInstant
    )
end AttachmentInfo

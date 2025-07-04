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

package loi.cp.reply

import com.learningobjects.cpxp.controller.upload.UploadInfo
import loi.cp.localmail.Localmail

/** Representation of an email reply. Exists for the purpose of integration testing. There is no implementation.
  */
// this has to be jackson b/c upload info (for now)
final case class Reply(
  from: Localmail.Address,
  to: Localmail.Address,
  subject: String,
  messageId: String,
  inReplyTo: Option[String],
  body: String,
  attachmentUploads: List[UploadInfo],
)

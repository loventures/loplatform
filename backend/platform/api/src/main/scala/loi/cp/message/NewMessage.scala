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

package loi.cp.message

import java.lang.Long as JLong
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.controller.upload.UploadInfo

/** Serialized form of a new message.
  * @param recipients
  *   the recipients of the message
  * @param context
  *   the context in which the message was sent, if any
  * @param subject
  *   the subject line
  * @param body
  *   the body
  * @param attachments
  *   any attachments to include
  */
case class NewMessage(
  recipients: Seq[Recipient],
  @JsonProperty("context_id") @JsonDeserialize(contentAs = classOf[JLong]) context: Option[Long],
  subject: String,
  body: String,
  @JsonProperty("uploads") attachments: Seq[UploadInfo]
)

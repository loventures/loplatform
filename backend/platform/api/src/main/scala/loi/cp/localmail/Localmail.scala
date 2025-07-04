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

package loi.cp.localmail

import java.time.Instant

import argonaut.CodecJson
import com.learningobjects.cpxp.scala.cpxp.PK
import loi.cp.attachment.AttachmentInfo

final case class Localmail(
  id: Long,
  from: Localmail.Address,
  to: Localmail.Address,
  subject: String,
  messageId: String,
  inReplyTo: Option[String],
  body: String,
  date: Instant,
  attachments: List[AttachmentInfo],
)

object Localmail:
  case class Address(
    address: String,
    name: String
  )

  import scaloi.json.ArgoExtras.*
  implicit val addressCodec: CodecJson[Address] = CodecJson.derive[Address]
  implicit val codec: CodecJson[Localmail]      = CodecJson.derive[Localmail]
  implicit val pk: PK[Localmail]                = _.id
end Localmail

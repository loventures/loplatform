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

import argonaut.*
import argonaut.Argonaut.*
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.learningobjects.cpxp.component.annotation.StringConvert
import loi.cp.reference.{ArgonautReferenceDeserializer, ArgonautReferenceSerializer, ArgonautStringConverter}
import scalaz.Functor

@StringConvert(`using` = classOf[AttachmentIdStringConverter])
@JsonSerialize(`using` = classOf[AttachmentIdSerializer])
@JsonDeserialize(`using` = classOf[AttachmentIdDeserializer])
case class AttachmentId(value: Long)

object AttachmentId:
  def applyM[F[_]](f: F[Long])(implicit F: Functor[F]): F[AttachmentId] =
    F.map(f)(AttachmentId(_))

  implicit val attachmentIdCodec: CodecJson[AttachmentId] =
    CodecJson(_.value.asJson, _.as[Long].map(AttachmentId.apply))

class AttachmentIdStringConverter extends ArgonautStringConverter[AttachmentId]
class AttachmentIdSerializer      extends ArgonautReferenceSerializer[AttachmentId, Long]
class AttachmentIdDeserializer    extends ArgonautReferenceDeserializer[AttachmentId]

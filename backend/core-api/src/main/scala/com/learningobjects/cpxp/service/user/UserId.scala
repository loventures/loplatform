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

package com.learningobjects.cpxp.service.user

import java.lang

import argonaut.Argonaut.*
import argonaut.*
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.annotation.StringConvert
import com.learningobjects.cpxp.scala.cpxp.PK
import com.learningobjects.cpxp.service.user.UserId.{
  UserIdJsonDeserializer,
  UserIdJsonSerializer,
  UserIdStringConverter
}
import loi.cp.reference.{ArgonautReferenceDeserializer, ArgonautReferenceSerializer, ArgonautStringConverter}
import scaloi.json.ArgoExtras

import scala.util.Random
import scalaz.syntax.std.map.*

/** The persistence id of a user in the system. When a parameter, the caller is responsible that for validation that the
  * value contained in the reference actually references a user.
  */
@StringConvert(`using` = classOf[UserIdStringConverter])
@JsonSerialize(`using` = classOf[UserIdJsonSerializer])
@JsonDeserialize(`using` = classOf[UserIdJsonDeserializer])
case class UserId(value: Long) extends Id:
  // shim for existing code
  def id: Long = value

  override def getId: lang.Long = value

object UserId:

  /** Create a stub [[UserId]] for testing purposes. */
  def stub(): UserId = this(Random.nextLong())

  implicit val attemptIdCodec: CodecJson[UserId]                        =
    CodecJson(_.value.asJson, _.as[Long].map(UserId.apply))
  implicit def userMapEncode[V: EncodeJson]: EncodeJson[Map[UserId, V]] =
    ArgoExtras.longMapEncode[V].contramap(_.mapKeys(_.value))
  implicit def userMapDecode[V: CodecJson]: DecodeJson[Map[UserId, V]]  =
    ArgoExtras.longMapDecode[V].map(_.mapKeys(UserId(_)))

  private[user] class UserIdStringConverter  extends ArgonautStringConverter[UserId]
  private[user] class UserIdJsonSerializer   extends ArgonautReferenceSerializer[UserId, Long]
  private[user] class UserIdJsonDeserializer extends ArgonautReferenceDeserializer[UserId]

  /** [[UserId]]s have a database primary key. */
  implicit val pk: PK[UserId] = _.value
end UserId

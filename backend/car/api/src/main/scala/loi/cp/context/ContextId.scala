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

package loi.cp.context

import argonaut.*
import argonaut.Argonaut.*
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.annotation.StringConvert
import com.learningobjects.cpxp.scala.cpxp.PK
import loi.cp.context.ContextId.{ContextIdJsonDeserializer, ContextIdJsonSerializer, ContextIdStringConverter}
import loi.cp.reference.{ArgonautReferenceDeserializer, ArgonautReferenceSerializer, ArgonautStringConverter}
import scalaz.*
import scalaz.std.anyVal.*
import scalaz.syntax.std.map.*
import scaloi.json.ArgoExtras

import java.lang as jl
import scala.util.Random

/** The persistence id of a context in the system. When a parameter, the caller is responsible that for validation that
  * the value contained in the reference actually references a context.
  */
@StringConvert(`using` = classOf[ContextIdStringConverter])
@JsonSerialize(`using` = classOf[ContextIdJsonSerializer])
@JsonDeserialize(`using` = classOf[ContextIdJsonDeserializer])
final case class ContextId(value: Long) extends Id:
  // shim for existing code
  def id: Long = value

  override def getId: jl.Long = value

object ContextId:

  /** Create a stub [[ContextId]] for testing purposes. */
  def mock(value: Long = Random.nextLong()): ContextId = this(value)

  /** Fiats that this context id originates from a database column containing a context id */
  def fromDatabase(value: Long) = this(value)

  implicit val attemptIdCodec: CodecJson[ContextId]                     =
    CodecJson(_.value.asJson, _.as[Long].map(ContextId.apply))
  implicit def userMapCodec[V: CodecJson]: CodecJson[Map[ContextId, V]] =
    ArgoExtras.longMapCodec[V].xmap(_.mapKeys(ContextId(_)))(_.mapKeys(_.id))

  private[context] class ContextIdStringConverter  extends ArgonautStringConverter[ContextId]
  private[context] class ContextIdJsonSerializer   extends ArgonautReferenceSerializer[ContextId, Long]
  private[context] class ContextIdJsonDeserializer extends ArgonautReferenceDeserializer[ContextId]

  implicit val pk: PK[ContextId] = _.value

  implicit val equal: Equal[ContextId] = Equal.equalBy(_.id)
end ContextId

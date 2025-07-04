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

package loi.cp.customisation

import java.math.BigDecimal
import java.time.Instant

import argonaut.*
import com.learningobjects.cpxp.scala.json.{Absent, OptionalField}
import loi.cp.reference.EdgePath

/** Instructor content customisation overlay.
  */
final case class ContentOverlay(
  order: Option[List[EdgePath]] = None,
  hide: Option[Set[EdgePath]] = None,
  title: Option[String] = None,
  instructions: Option[String] = None,
  duration: Option[Long] = None,
  isForCredit: Option[Boolean] = None,
  pointsPossible: Option[BigDecimal] = None,
  gateDate: OptionalField[Instant] = Absent(),
  dueDate: OptionalField[Instant] = Absent(),
  metadata: Option[Json] = None
)

object ContentOverlay:
  final val empty = ContentOverlay()

  import Argonaut.*
  implicit private val instantCodec: CodecJson[Instant] = scaloi.json.ArgoExtras.instantCodec
  // the things we do for our principles...
  implicit val encodeJson: EncodeJson[ContentOverlay]   = EncodeJson { co =>
    def optF[T: EncodeJson](name: String)(x: OptionalField[T]) =
      x.catOpt(name -> jNull)(name := _)
    def opt[T: EncodeJson](name: String)(x: Option[T])         =
      x.map(name := _)

    val fields = List(
      opt("order")(co.order),
      opt("hide")(co.hide),
      opt("title")(co.title),
      opt("instructions")(co.instructions),
      opt("duration")(co.duration),
      opt("isForCredit")(co.isForCredit),
      opt("pointsPossible")(co.pointsPossible),
      optF("gateDate")(co.gateDate),
      optF("dueDate")(co.dueDate),
      opt("metadata")(co.metadata),
    ).flatten
    jObjectFields(fields*)
  }
  implicit val decodeJson: DecodeJson[ContentOverlay]   =
    DecodeJson.derive[ContentOverlay]
end ContentOverlay

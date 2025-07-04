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

/** Update to instructor content customisation overlay.
  */
final case class ContentOverlayUpdate(
  order: OptionalField[List[EdgePath]] = Absent(),
  hide: OptionalField[Set[EdgePath]] = Absent(),
  title: OptionalField[String] = Absent(),
  instructions: OptionalField[String] = Absent(),
  duration: OptionalField[Long] = Absent(),
  isForCredit: OptionalField[Boolean] = Absent(),
  pointsPossible: OptionalField[BigDecimal] = Absent(),
  gateDate: OptionalField[Instant] = Absent(),
  dueDate: OptionalField[Instant] = Absent(),
  metadata: OptionalField[Json] = Absent()
):

  /** Merge this update with a content overlay. */
  def |:(overlay: ContentOverlay): ContentOverlay =
    ContentOverlay(
      order = overlay.order |: order,
      hide = overlay.hide |: hide,
      title = overlay.title |: title,
      instructions = overlay.instructions |: instructions,
      duration = overlay.duration |: duration,
      isForCredit = overlay.isForCredit |: isForCredit,
      pointsPossible = overlay.pointsPossible |: pointsPossible,
      gateDate = overlay.gateDate |: gateDate,
      dueDate = overlay.dueDate |: dueDate,
      metadata = overlay.metadata |: metadata,
    )
end ContentOverlayUpdate

object ContentOverlayUpdate:

  import Argonaut.*
  implicit private val instantCodec: CodecJson[Instant]     = scaloi.json.ArgoExtras.instantCodec
  implicit val encodeJson: EncodeJson[ContentOverlayUpdate] = EncodeJson { co =>
    def optF[T: EncodeJson](name: String)(x: OptionalField[T]) =
      x.catOpt(name -> jNull)(name := _)

    val fields = List(
      optF("order")(co.order),
      optF("hide")(co.hide),
      optF("title")(co.title),
      optF("instructions")(co.instructions),
      optF("duration")(co.duration),
      optF("isForCredit")(co.isForCredit),
      optF("pointsPossible")(co.pointsPossible),
      optF("gateDate")(co.gateDate),
      optF("dueDate")(co.dueDate),
      optF("metadata")(co.metadata),
    ).flatten
    jObjectFields(fields*)
  }
  implicit val decodeJson: DecodeJson[ContentOverlayUpdate] =
    DecodeJson.derive[ContentOverlayUpdate]
end ContentOverlayUpdate

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

package loi.cp.progress

import argonaut.{CodecJson, DecodeJson, EncodeJsonKey}
import com.fasterxml.jackson.annotation.{JsonCreator, JsonValue}
import loi.cp.path.Path as IdPath
import loi.cp.reference.EdgePath
import scalaz.*
import scalaz.std.string.*

/** A wrapper around a path to be used in progress.
  *
  * Legacy progress uses [[IdPath]]; lightweight progress uses [[EdgePath]]. This is not quite their coproduct, but it's
  * a way of pushing the decision of which path has been passed in/out to a point where it's known.
  */
final case class ProgressPath(@JsonValue override val toString: String):
  def asLegacyPath: Option[IdPath] =
    try Some(IdPath.fromEncodedString(toString))
    catch case _: IllegalArgumentException => None
  def asEdgePath: EdgePath         = EdgePath.parse(toString)

object ProgressPath:
  import language.implicitConversions

  // jackson wtf
  @JsonCreator def apply(s: String): ProgressPath = new ProgressPath(s)

  // injections from other kinds of paths
  implicit def fromLegacyPath(lp: IdPath): ProgressPath =
    ProgressPath(lp.toString)
  implicit def fromEdgePath(ep: EdgePath): ProgressPath =
    ProgressPath(ep.toString)

  import Isomorphism.*
  val stringIso: ProgressPath <=> String = IsoSet(_.toString, apply)

  implicit val equal: Equal[ProgressPath]            = Equal.fromIso(stringIso)
  implicit val order: Order[ProgressPath]            = Order.fromIso(stringIso)
  implicit val ordering: math.Ordering[ProgressPath] = order.toScalaOrdering

  implicit val progressPathCodec: CodecJson[ProgressPath] =
    CodecJson.derived[String].xmap(apply)(_.toString)

  implicit val progressPathKeyCodec: EncodeJsonKey[ProgressPath] =
    EncodeJsonKey.from[ProgressPath](_.toString)

  implicit def progressPathMapDecodeJson[T](implicit
    decode: DecodeJson[Map[String, T]]
  ): DecodeJson[Map[ProgressPath, T]] =
    DecodeJson(json => decode(json).map(_.map(t => this(t._1) -> t._2)))
end ProgressPath

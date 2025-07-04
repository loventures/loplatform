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

import argonaut.*
import argonaut.Argonaut.*
import scalaz.*
import scalaz.syntax.std.boolean.*
import scalaz.std.anyVal.*
import scalaz.std.set.*
import scalaz.std.tuple.*
import scaloi.syntax.OptionOps.*

final case class IncrementTypes(
  visited: Int,
  testedOut: Int,
  skipped: Int,
):
  import IncrementType.*

  def contains(tpe: IncrementType): Boolean = tpe match
    case VISITED   => visited > 0
    case TESTEDOUT => testedOut > 0
    case SKIPPED   => skipped > 0

  def +(tpe: IncrementType): IncrementTypes = tpe match
    case VISITED   => copy(visited = visited + 1)
    case TESTEDOUT => copy(testedOut = testedOut + 1)
    case SKIPPED   => copy(skipped = skipped + 1)

  def -(tpe: IncrementType): IncrementTypes = tpe match
    case VISITED   => copy(visited = 0 max visited - 1)
    case TESTEDOUT => copy(testedOut = 0 max testedOut - 1)
    case SKIPPED   => copy(skipped = 0 max skipped - 1)

  def toSet: Set[IncrementType] =
    ((visited > 0) ?? Set[IncrementType](VISITED)) ++
      ((testedOut > 0) ?? Set(TESTEDOUT)) ++
      ((skipped > 0) ?? Set(SKIPPED))

  override def toString: String =
    List(
      OptionNZ(visited).map("visited " + _),
      OptionNZ(testedOut).map("tested out " + _),
      OptionNZ(skipped).map("skipped " + _),
    ).flatten.mkString("IncrementTypes(", ",", ")")
end IncrementTypes

object IncrementTypes:
  val empty: IncrementTypes = IncrementTypes(0, 0, 0)

  def of(its: IncrementType*): IncrementTypes =
    IncrementTypes(
      visited = its.count(_ == IncrementType.VISITED),
      testedOut = its.count(_ == IncrementType.TESTEDOUT),
      skipped = its.count(_ == IncrementType.SKIPPED),
    )

  import Isomorphism.*
  val iso: IncrementTypes <=> (Int, Int, Int) =
    IsoSet.apply(it => (it.visited, it.testedOut, it.skipped), (apply).tupled)

  implicit val monoid: Monoid[IncrementTypes] = Monoid.fromIso(iso)

  val encodeJson: EncodeJson[IncrementTypes] = EncodeJson(a =>
    Json(
      IncrementType.VISITED.entryName   := a.visited,
      IncrementType.TESTEDOUT.entryName := a.testedOut,
      IncrementType.SKIPPED.entryName   := a.skipped
    )
  )

  val decodeJson: DecodeJson[IncrementTypes] = DecodeJson(cursor =>
    for
      visited   <- (cursor --\ IncrementType.VISITED.entryName).as[Int]
      testedOut <- (cursor --\ IncrementType.TESTEDOUT.entryName).as[Int]
      skipped   <- (cursor --\ IncrementType.SKIPPED.entryName).as[Int] ||| DecodeResult.ok(0)
    yield IncrementTypes(visited, testedOut, skipped)
  )

  implicit val codec: CodecJson[IncrementTypes] = CodecJson.derived(using encodeJson, decodeJson)
end IncrementTypes

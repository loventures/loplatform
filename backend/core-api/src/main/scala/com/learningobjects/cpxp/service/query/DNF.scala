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

package com.learningobjects.cpxp.service.query

import java.util as ju

import scala.annotation.varargs
import scala.jdk.CollectionConverters.*

/** A complex composition of conditions, represented in Disjunctive Normal Form, i.e., a list of lists representing
  * conjunctive conditions, disjoined together.
  */
case class DNF(value: Seq[Seq[Condition]]):
  def asJava: ju.List[ju.List[Condition]] =
    value.map(_.asJava).asJava

  // (A + B) + (C + D) = A + B + C + D
  def ||(right: DNF): DNF =
    DNF(this.value ++ right.value)

  def ||(right: Condition): DNF =
    this || DNF(right)

  // (A + B) * (C + D) = (A * C) + (B * C) + (A * D) + (B * C)
  def &&(right: DNF): DNF =
    DNF(for
      cl <- this.value
      cr <- right.value
    yield cl ++ cr)

  def &&(right: Condition): DNF =
    this && DNF(right)
end DNF

object DNF:
  val empty: DNF = DNF(Seq.empty)

  /** A DNF wrapping a single condition. */
  def apply(cond: Condition): DNF =
    DNF(Seq(Seq(cond)))

  /** A DNF which conjoins the given conditions */
  @varargs
  def conjunction(conds: Condition*): DNF =
    DNF(Seq(conds))

  /** A DNF which disjoins the given conditions */
  @varargs
  def disjunction(conds: Condition*): DNF =
    DNF(conds map (Seq(_)))
end DNF

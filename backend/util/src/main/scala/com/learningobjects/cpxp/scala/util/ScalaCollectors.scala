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

package com.learningobjects.cpxp.scala.util

import java.util.stream.Collector.Characteristics
import java.util as ju
import java.util.{function as jf, stream as s}
import scala.collection.Factory
import scala.collection.mutable

object ScalaCollectors:
  type ScalaCollector[CC[_], E] = s.Collector[E, mutable.Builder[E, CC[E]], CC[E]]

  def toSeq[E]: ScalaCollector[Seq, E] = collector[Seq, E]

  def collector[CC[X] <: IterableOnce[X], E](implicit fac: Factory[E, CC[E]]): ScalaCollector[CC, E] =
    /* the type parameters of `s.Collector`, for concision and clarity */
    type T = E
    type A = mutable.Builder[E, CC[E]]
    type R = CC[E]

    new s.Collector[T, A, R]:
      def characteristics                  = ju.Collections.emptySet[Characteristics]()
      val supplier: jf.Supplier[A]         = () => fac.newBuilder
      val accumulator: jf.BiConsumer[A, T] = (a, t) => a += t
      val combiner: jf.BinaryOperator[A]   = (a1, a2) => a1 ++= a2.result()
      val finisher: jf.Function[A, R]      = _.result()
  end collector
end ScalaCollectors

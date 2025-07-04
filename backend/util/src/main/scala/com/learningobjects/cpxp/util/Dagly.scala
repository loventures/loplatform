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

package com.learningobjects.cpxp.util

import java.{lang as jl, util as ju}

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

trait Dagly[T] /* extends Equals[T] */:
  def outgoingEdges(t: T): Set[T]

object Dagly:

  /** A pretty crap topological-sort, but a passable one. Stolen largely from our friends at SBT.
    */
  def sort[T](ts: Iterable[T])(implicit dag: Dagly[T]): List[T] =
    val seen: mutable.Set[T]          = mutable.Set.empty
    val result: mutable.ListBuffer[T] = mutable.ListBuffer.empty

    def visit(node: T): Unit =
      if !seen(node) then
        seen += node
        dag.outgoingEdges(node) foreach visit
        result += node

    ts foreach visit

    result.toList
  end sort

  /* and for java... */
  def sort[T](ts: jl.Iterable[T], dag: T => jl.Iterable[T]): ju.List[T] =
    sort(ts.asScala)(using t => dag(t).asScala.toSet).asJava
end Dagly

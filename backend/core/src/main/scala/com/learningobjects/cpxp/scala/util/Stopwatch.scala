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

import scala.concurrent.duration.*

/** A simple stopwatch.
  */
class Stopwatch:

  /** The stopwatch start time.
    */
  val start: Long = System.currentTimeMillis

  /** Get the elapsed time.
    *
    * @return
    *   the elapsed time
    */
  def elapsed: FiniteDuration = (System.currentTimeMillis - start).millis
end Stopwatch

/** Stopwatch companion.
  */
object Stopwatch:

  /** Profile a function.
    *
    * @param f
    *   the function
    * @tparam A
    *   the result type of the function
    * @return
    *   the function result and execution time
    */
  def profiled[A](f: => A, sw: Stopwatch = new Stopwatch): (A, FiniteDuration) =
    (f, sw.elapsed)
end Stopwatch

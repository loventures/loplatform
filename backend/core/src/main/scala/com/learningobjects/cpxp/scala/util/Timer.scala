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

import scala.concurrent.duration.Duration

class Timer(timeout: Duration):
  private val end: Long      = if timeout.isFinite then System.currentTimeMillis + timeout.toMillis else Long.MaxValue
  private var latch: Boolean = false

  /** Has this time limit expired.
    */
  def expired: Boolean =
    if System.currentTimeMillis >= end then latch = true
    latch

  /** Did this time limit expire on the last call to [expired]. */
  def didExpire: Boolean = latch
end Timer

object Timer:
  object Eternal extends Timer(Duration.Inf)

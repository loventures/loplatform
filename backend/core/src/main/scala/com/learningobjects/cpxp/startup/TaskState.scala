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

package com.learningobjects.cpxp.startup

import enumeratum.{Enum, EnumEntry}

/** Persisted state of a startup task.
  */
sealed trait TaskState extends EnumEntry

/** Startup task state companion.
  */
object TaskState extends Enum[TaskState]:

  /** Convert boolean to success or failure. */
  def apply(success: Boolean): TaskState = if success then Success else Failure

  /** Enumeration values.
    */
  val values = findValues

  /** Task is in a state of failure. */
  case object Failure extends TaskState

  /** Failed task should be retried. */
  case object Retry extends TaskState

  /** Failed task should be skipped. */
  case object Skip extends TaskState

  /** Task is in a state of success. */
  case object Success extends TaskState
end TaskState

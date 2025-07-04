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

package loi.cp.startup

import java.lang.Long as JLong
import java.util.Date

import com.learningobjects.cpxp.service.query.{BaseDataProjection, QueryService}
import com.learningobjects.cpxp.service.startup.StartupTaskConstants
import com.learningobjects.cpxp.startup.{TaskIdentifier, TaskState}

/** Helper companion object for working with startup task states.
  */
object TaskStates:

  /** A task state maps task identifiers to the most recent persisted task state. */
  private type TaskStateMap = Map[TaskIdentifier, TaskState]

  /** Get the most recent state of previous startup task runs, as persisted to the database for a given domain.
    *
    * @param domain
    *   the domain in question
    * @return
    *   a map from task identifiers to the most recently persisted state
    */
  def taskStates(domain: Long)(implicit qs: QueryService): TaskStateMap =
    runLog(domain)
      .groupBy(_._1)
      .view
      .mapValues { states =>
        states.map(_._2).sortBy(_._1).last._2
      }
      .toMap

  /** Get the sequence of all persisted startup task runs for a given domain.
    *
    * @param domain
    *   the domain
    * @return
    *   the persisted task state
    */
  private def runLog(domain: Long)(implicit qs: QueryService): Seq[(TaskIdentifier, (Date, TaskState))] =
    qs.queryRoot(domain, StartupTaskConstants.ITEM_TYPE_STARTUP_TASK)
      .setDataProjection(
        BaseDataProjection.ofData(
          StartupTaskConstants.DATA_TYPE_STARTUP_TASK_IDENTIFIER,
          StartupTaskConstants.DATA_TYPE_STARTUP_TASK_VERSION,
          StartupTaskConstants.DATA_TYPE_STARTUP_TASK_TIMESTAMP,
          StartupTaskConstants.DATA_TYPE_STARTUP_TASK_STATE
        )
      )
      .getValues[Array[?]] collect { case Array(identifier: String, version: JLong, timestamp: Date, state: String) =>
      TaskIdentifier(identifier, version.intValue) ->
        (timestamp -> TaskState.withName(state))
    }
end TaskStates

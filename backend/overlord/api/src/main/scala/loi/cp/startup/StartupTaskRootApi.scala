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

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, HttpContext, Method, WebResponse}
import com.learningobjects.cpxp.startup.{StartupTaskInfo, TaskState}
import com.learningobjects.de.authorization.Secured
import loi.cp.overlord.OverlordRight

import scala.concurrent.Future

/** Startup task web API.
  */
@Controller(value = "startupTasks", root = true)
@RequestMapping(path = "startupTasks")
@Secured(Array(classOf[OverlordRight]))
trait StartupTaskRootApi extends ApiRootComponent:

  /** Re-execute the system startup task service. Use this after updating some failed tasks to Retry or Skip state.
    */
  @RequestMapping(path = "execute", method = Method.POST, async = true)
  def execute(): Unit

  /** Get system startup tasks.
    *
    * @return
    *   the startup tasks in dependency order
    */
  @RequestMapping(path = "system", method = Method.GET)
  def systemTasks: Seq[StartupTaskDto]

  /** Get domain startup tasks.
    *
    * @param id
    *   the domain id
    * @return
    *   the startup tasks in dependency order
    */
  @RequestMapping(path = "byDomain/{id}", method = Method.GET)
  def tasksByDomain(@PathVariable("id") id: Long): Seq[StartupTaskDto]

  /** Get a startup task receipt.
    *
    * @param id
    *   the receipt id
    * @return
    *   the startup task receipt
    */
  @RequestMapping(path = "receipts/{id}", method = Method.GET)
  def receipt(@PathVariable("id") id: Long): Option[StartupTaskReceipt]

  /** Get startup task receipts.
    *
    * @param q
    *   the API query
    * @return
    *   the startup task receipts
    */
  @RequestMapping(path = "receipts", method = Method.GET)
  def receipts(q: ApiQuery): ApiQueryResults[StartupTaskReceipt]

  /** Update the state of a system startup receipt.
    *
    * @param id
    *   the receipt id
    * @param state
    *   the state update
    */
  @RequestMapping(path = "receipts/{id}", method = Method.PUT)
  def updateReceipt(@PathVariable("id") id: Long, @RequestBody state: StartupTaskStateDto): Option[StartupTaskReceipt]

  /** Receive a stream of server status event updates.
    *
    * @param http
    *   the http context
    * @return
    *   a stream of SSE events
    */
  @RequestMapping(path = "events", method = Method.GET)
  def events(http: HttpContext): WebResponse

  /** Cancel any open SSE event stream.
    *
    * @param http
    *   the http context
    */
  @RequestMapping(path = "events", method = Method.DELETE)
  def cancelEvents(http: HttpContext): Unit

  /** Get startup task system status.
    */
  @RequestMapping(path = "status", method = Method.GET)
  def status: Future[String]
end StartupTaskRootApi

/** DTO for a startup task.
  *
  * @param identifier
  *   the task identifier
  * @param version
  *   the task version
  * @param runAfter
  *   the tasks that this depends on
  * @param state
  *   the current execution state of the task
  */
case class StartupTaskDto(
  identifier: String,
  version: Int,
  runAfter: Seq[String],
  state: Option[TaskState]
)

/** Startup task DTO companion.
  */
object StartupTaskDto:

  /** Construct a task state DTO.
    *
    * @param info
    *   the task information
    * @param state
    *   the current task state
    * @return
    *   the task state DTO
    */
  def apply(info: StartupTaskInfo.Any, state: Option[TaskState]): StartupTaskDto =
    StartupTaskDto(info.startupClass.getName, info.binding.version, info.binding.runAfter.toSeq.map(_.getName), state)
end StartupTaskDto

/** DTO for updating a task state.
  *
  * @param state
  *   the new task state
  */
case class StartupTaskStateDto(
  state: TaskState
)

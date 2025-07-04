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

package loi.cp.job

import java.util.Date
import javax.validation.groups.Default

import com.fasterxml.jackson.annotation.{JsonProperty, JsonView}
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{Method, WebResponse}
import com.learningobjects.cpxp.component.{ComponentInterface, RestfulComponent}
import com.learningobjects.de.web.Queryable
import com.learningobjects.de.web.Queryable.Trait

/** A cron-scheduled job. */
@Schema("job")
trait Job[J <: Job[J]] extends RestfulComponent[J]:
  import Job.*

  /** The name of this job. */
  @Queryable(traits = Array(Trait.CASE_INSENSITIVE))
  @JsonProperty(NameProperty)
  def getName: String

  /** The cron schedule for this job. See Quartz CronExpression. */
  @JsonProperty
  def getSchedule: String

  /** Whether this job is disabled. */
  @JsonProperty
  def isDisabled: Boolean

  /** Whether this job is Manual. */
  @JsonProperty
  def isManual: Boolean

  /** When this job is next scheduled to run. */
  @JsonView(Array(classOf[Default]))
  def getScheduled: Option[Date]

  /** If this job is currently running, metadata about the current run. */
  @JsonProperty
  def getCurrentRun: Option[Run]

  /** Query previous runs of this job. */
  @RequestMapping(path = "runs", method = Method.GET)
  def getRuns(q: ApiQuery): ApiQueryResults[Run]

  /** Get a previous run of this job. */
  @RequestMapping(path = "runs/{id}", method = Method.GET)
  def getRun(@PathVariable("id") id: Long): Option[Run]

  /** Execute this job now. Be aware that this expressly manipulates transactions. */
  @RequestMapping(path = "execute", method = Method.POST, async = true)
  def execute(): Run
end Job

/** "Static" methods of a job implementation, invoked without any backing instance.
  */
trait JobObject extends ComponentInterface:

  /** Render custom configuration HTML for configuring this job. */
  @RequestMapping(path = "adminUI", method = Method.GET)
  def renderAdminUI(): WebResponse

/** Job component companion.
  */
object Job:

  /** The name property. */
  final val NameProperty = "name"

  final val Manual = "Manual"

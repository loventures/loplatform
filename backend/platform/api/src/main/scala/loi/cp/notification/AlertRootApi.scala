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

package loi.cp.notification

import java.util.Date

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.de.authorization.Secured
import com.learningobjects.de.web.Deletable

@Controller(value = "alerts", root = true)
@RequestMapping(path = "alerts")
@Secured(allowAnonymous = false)
trait AlertRootApi extends ApiRootComponent:
  import AlertRootApi.*

  /** Query your alerts. */
  @RequestMapping(method = Method.GET)
  def get(q: ApiQuery): ApiQueryResults[Alert]

  /** Get an alert. */
  @Deletable
  @RequestMapping(path = "{id}", method = Method.GET)
  def get(@PathVariable("id") id: Long): Option[Alert]

  /** Return a summary of your alerts. */
  @RequestMapping(path = "summary", method = Method.GET)
  def summary(q: ApiQuery): AlertSummary

  /** Flag viewed alerts. */
  @RequestMapping(path = "{alertId}/view", method = Method.POST)
  def view(@PathVariable("alertId") alertId: Long): Unit

  /** Update last viewed time. */
  @RequestMapping(path = "viewed", method = Method.POST)
  def viewed(@RequestBody viewed: AlertViewed): Unit
end AlertRootApi

object AlertRootApi:

  case class AlertSummary(
    /** Count of recent notifications */
    count: Long,
    /** date of most recent notification */
    date: Option[Date],
    /* date when you last explicitly viewed notifications */
    viewDate: Option[Date]
  )

  case class AlertViewed(
    /** Most recent date of the notifications downloaded */
    date: Date
  )
end AlertRootApi

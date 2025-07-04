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

package loi.cp.analytics.bus

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.{IntegrationAdminRight, MonitoringAdminRight}

/** API root for interacting with analytic buses.
  */
@Controller(value = "analyticBuses", root = true)
@RequestMapping(path = "analyticBuses")
@Secured(Array(classOf[IntegrationAdminRight]))
trait AnalyticBusRootApi extends ApiRootComponent:

  /** Get all the analytic buses.
    * @return
    *   the analytic buses
    */
  @RequestMapping(method = Method.GET)
  def getAnalyticBuses: Seq[AnalyticBus]

  @RequestMapping(path = "status", method = Method.GET)
  @Secured(value = Array(classOf[MonitoringAdminRight]), overrides = true)
  def getAnalyticBusesStatus: List[AnalyticBusDTO]

  /** Get an analytic bus.
    * @param id
    *   the bus ID
    * @return
    *   the analytic bus
    */
  @RequestMapping(path = "{id}", method = Method.GET)
  def getAnalyticBus(@PathVariable("id") id: Long): Option[AnalyticBus]

  /** Pauses an analytic bus if its active
    * @param id
    *   the bus ID
    */
  @RequestMapping(path = "{id}/pause", method = Method.POST)
  def pauseAnalyticBus(@PathVariable("id") id: Long): Unit

  /** Resumes a non-active analytic bus while re-setting its failure count to 0
    * @param id
    *   the bus ID
    */
  @RequestMapping(path = "{id}/resume", method = Method.POST)
  def resumeAnalyticBus(@PathVariable("id") id: Long): Unit

  /** Immediately flushes (or retries) the queue of events on a bus, instead of waiting
    * @param id
    *   the bus ID
    */
  @RequestMapping(path = "{id}/pump", method = Method.POST)
  def pumpBus(@PathVariable("id") id: Long): Unit
end AnalyticBusRootApi

case class AnalyticBusDTO(
  id: Long,
  domain: String,
  name: String,
  state: AnalyticBusState,
  failureCount: Long
)

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

import com.learningobjects.cpxp.component.RestfulRootComponent
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ErrorResponse, Method}
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.HostingAdminRight
import loi.cp.component.ComponentRootApi
import java.util.Date

import loi.cp.job.JobRootApi.CronDTO

import scalaz.\/

/** Root controller for managing scheduled jobs. */
@Controller(value = "jobs", root = true)
@RequestMapping(path = "jobs")
@Secured(Array(classOf[HostingAdminRight]))
trait JobRootApi extends RestfulRootComponent[Job[?]]:

  /** Get available job implementations. */
  @RequestMapping(path = "components", method = Method.Any)
  def components: ComponentRootApi

  /** Validate Schedule */
  @RequestMapping(path = "validateCron", method = Method.POST)
  def validateCron(@RequestBody schedule: CronDTO): ErrorResponse \/ Option[Date]
end JobRootApi

object JobRootApi:
  case class CronDTO(schedule: String)

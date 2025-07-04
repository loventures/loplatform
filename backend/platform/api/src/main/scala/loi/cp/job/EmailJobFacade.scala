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

import com.learningobjects.cpxp.dto.*
import com.learningobjects.cpxp.service.job.JobFinder.*

trait EmailJobFacade extends JobFacade:
  @FacadeJson(DATA_TYPE_JOB_JSON)
  def getEmailAddresses: List[String]
  def setEmailAddresses(emailAddresses: List[String]): Unit

  @FacadeJson(DATA_TYPE_JOB_JSON)
  def getConfig: JobConfig
  def setConfig(jobConfig: JobConfig): Unit

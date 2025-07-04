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

package loi.cp.analytics.job

import com.learningobjects.cpxp.dto.FacadeJson
import com.learningobjects.cpxp.service.job.JobFinder.DATA_TYPE_JOB_JSON
import loi.cp.job.EmailJobFacade

trait InstructorMetricsReportFacade extends EmailJobFacade:

  @FacadeJson(DATA_TYPE_JOB_JSON)
  def getExcludeEmails: List[String]
  def setExcludeEmails(emails: List[String]): Unit

  @FacadeJson(DATA_TYPE_JOB_JSON)
  def getExcludeEmailDomains: List[String]
  def setExcludeEmailDomains(domains: List[String]): Unit

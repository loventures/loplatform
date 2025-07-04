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

import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.startup.StartupTaskScope.Domain
import com.learningobjects.cpxp.startup.{StartupTask, StartupTaskBinding}
import loi.cp.startup.FolderEnsurers

/** Convert analytic folders into buses.
  */
@StartupTaskBinding(version = 20171024, taskScope = Domain)
class AnalyticBusStartupTask2(implicit
  fs: FacadeService,
  domain: DomainDTO
) extends StartupTask
    with FolderEnsurers:

  override def run(): Unit =
    ensureFolderByType(AnalyticBusServiceImpl.AnalyticFolderType)

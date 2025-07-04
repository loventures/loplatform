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

import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.startup.StartupTaskScope.AnyDomain
import com.learningobjects.cpxp.startup.{StartupTask, StartupTaskBinding}
import loi.cp.job.JobRootApiImpl
import loi.cp.localmail.LocalmailRootFacade
import loi.cp.network.NetworkServiceImpl

/** A shared startup task responsible for creating domain folders expected by platform machinery.
  *
  * @param domain
  *   the domain being initialized
  * @param fs
  *   the facade service
  */
@StartupTaskBinding(
  version = 20161207,
  taskScope = AnyDomain
)
class PlatformStartupTask(implicit
  domain: DomainDTO,
  fs: FacadeService
) extends StartupTask
    with FolderEnsurers:

  override def run(): Unit =
    ensureFolderByType(JobRootApiImpl.JobFolderType)
    ensureFolderByType(NetworkServiceImpl.NetworkFolderType)
    ensureFolderByType(LocalmailRootFacade.LocalmailFolderType)
end PlatformStartupTask

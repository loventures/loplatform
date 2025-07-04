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

package loi.cp.message

import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.startup.{StartupTask, StartupTaskBinding}
import com.learningobjects.cpxp.startup.StartupTaskScope.Domain
import loi.cp.startup.FolderEnsurers

@StartupTaskBinding(version = 20161018, taskScope = Domain)
class MessagingStartupTask(implicit
  domain: DomainDTO,
  fs: FacadeService,
  val componentInstance: ComponentInstance
) extends StartupTask
    with FolderEnsurers
    with ComponentImplementation:
  def run() =
    ensureFolderByType(MessageServiceImpl.MessageStorageFolderType)
    ()
end MessagingStartupTask

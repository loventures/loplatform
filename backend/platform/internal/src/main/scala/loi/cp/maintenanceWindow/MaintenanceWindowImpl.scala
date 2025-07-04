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

package loi.cp.maintenanceWindow

import java.util.Date

import com.learningobjects.cpxp.component.annotation.{Component, PostCreate}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.maintenanceWindow.MaintenanceWindowFinder

/** Implementation of the MaintenanceWindow trait.
  */
@Component
class MaintenanceWindowImpl(val componentInstance: ComponentInstance, self: MaintenanceWindowFinder)(implicit
  is: ItemService
) extends MaintenanceWindow
    with ComponentImplementation:

  @PostCreate
  private def init(maintenanceWindow: MaintenanceWindowDTO): Unit =
    update(maintenanceWindow)

  override def getId = componentInstance.getId

  override def getDuration: Long = self.duration

  override def getStartTime: Date = self.startTime

  override def isDisabled: Boolean = self.disabled

  override def getAnnouncementId: Long = self.announcementId

  override def update(maintenanceWindow: MaintenanceWindowDTO): Unit =
    self.startTime = maintenanceWindow.startTime
    self.duration = maintenanceWindow.duration
    self.disabled = maintenanceWindow.disabled
    self.announcementId = maintenanceWindow.announcementId

  override def delete(): Unit = is.delete(self)
end MaintenanceWindowImpl

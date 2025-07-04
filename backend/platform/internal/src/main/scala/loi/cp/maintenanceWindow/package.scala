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

package loi.cp

import com.learningobjects.cpxp.component.DataModel
import com.learningobjects.cpxp.service.maintenanceWindow.MaintenanceWindowFinder.*

package object maintenanceWindow:
  implicit val MaintenanceWindowDataModel: DataModel[MaintenanceWindow] =
    DataModel(
      ITEM_TYPE_MAINTENANCE_WINDOW,
      singleton = true,
      schemaMapped = false,
      Map(
        MaintenanceWindow.durationProperty       -> DATA_TYPE_MAINTENANCE_WINDOW_DURATION,
        MaintenanceWindow.startTimeProperty      -> DATA_TYPE_MAINTENANCE_WINDOW_START_TIME,
        MaintenanceWindow.disabledProperty       -> DATA_TYPE_MAINTENANCE_WINDOW_DISABLED,
        MaintenanceWindow.announcementIdProperty -> DATA_TYPE_MAINTENANCE_WINDOW_ANNOUNCEMENT_ID
      )
    )
end maintenanceWindow

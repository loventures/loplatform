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

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.de.web.{Queryable, QueryableId}
import java.util.Date

/** A Maintenance Window.
  */
trait MaintenanceWindow extends ComponentInterface with QueryableId:

  @JsonProperty
  @Queryable
  def getStartTime: Date

  @JsonProperty
  @Queryable
  def getDuration: Long

  @JsonProperty
  @Queryable
  def isDisabled: Boolean

  /** Get the id of the Announcement that is associated with this particular Maintenance Window */
  @JsonProperty
  @Queryable
  def getAnnouncementId: Long

  def update(maintenanceWindow: MaintenanceWindowDTO): Unit

  def delete(): Unit
end MaintenanceWindow

object MaintenanceWindow:
  final val idProperty             = "id"
  final val startTimeProperty      = "startTime"
  final val durationProperty       = "duration"
  final val disabledProperty       = "disabled"
  final val announcementIdProperty = "announcementId"

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

package loi.cp.announcement

import java.util.Date

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.ItemMapping
import com.learningobjects.de.web.{Queryable, QueryableId}

/** An Announcement.
  */
@ItemMapping(value = "Announcement", singleton = true) // TODO: fix AppEvent+DataModel and then remove!
trait Announcement extends ComponentInterface with QueryableId:

  @JsonProperty
  @Queryable
  def getStartTime: Date

  @JsonProperty
  @Queryable
  def getEndTime: Date

  @JsonProperty
  @Queryable(traits = Array(Queryable.Trait.CASE_INSENSITIVE))
  def getMessage: String

  @JsonProperty
  def getStyle: String

  @JsonProperty
  @Queryable
  def isActive: Boolean

  def update(announcement: AnnouncementDTO): Unit

  def delete(): Unit
end Announcement

object Announcement:
  final val startTimeProperty = "startTime"
  final val endTimeProperty   = "endTime"
  final val messageProperty   = "message"
  final val activeProperty    = "active"

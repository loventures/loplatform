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

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}

@Service
trait AnnouncementService:
  def get(parent: Id, apiQuery: ApiQuery): ApiQueryResults[Announcement]

  def get(parent: Id, id: Long): Option[Announcement]

  def create(parent: Id, announcement: AnnouncementDTO): Announcement

  def getActive(apiQuery: ApiQuery, contexts: List[Long] = Nil): ApiQueryResults[Announcement]

  def hide(annId: Long): Unit

  def update(oldAnnouncement: Announcement, announcement: AnnouncementDTO): Announcement

  def delete(announcement: Announcement): Unit
end AnnouncementService

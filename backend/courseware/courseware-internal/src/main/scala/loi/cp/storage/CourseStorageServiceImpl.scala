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

package loi.cp.storage

import argonaut.Json
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.context.ContextId
import loi.cp.storage.impl.CourseStorageDao

@Service
class CourseStorageServiceImpl(dao: CourseStorageDao) extends CourseStorageService:
  import CourseStorageDao.*

  override def get[T: CourseStoreable](crs: ContextId): T =
    val entity = dao.loadCourseStorage(crs.id)
    val data   = entity.map(_.getStoragedData).getOrElse(Json.jEmptyObject)
    decodeField[T](crs, None, data)

  override def modify[T: CourseStoreable](crs: ContextId)(mod: T => T): T =
    val entity  = dao.ensureAndLoadCourseStorageForUpdate(crs.id)
    val data    = Option(entity.getStoragedData).getOrElse(Json.jEmptyObject)
    val decoded = decodeField[T](crs, None, data)
    val modded  = mod(decoded)
    entity.setStoragedData(put(data, modded))
    modded

  override def reset(course: ContextId, usr: UserId): Unit =
    val crs = ContextId(course.getId)
    dao.loadUserStorage(crs.id, usr.id) foreach { entity =>
      entity.setStoragedData(Json.jEmptyObject)
    }

  override def get[T: CourseStoreable](course: ContextId, usr: UserId): T =
    val crs    = ContextId(course.getId)
    val entity = dao.loadUserStorage(crs.id, usr.id)
    val data   = entity.map(_.getStoragedData).getOrElse(Json.jEmptyObject)
    decodeField[T](crs, Some(usr), data)

  override def get[T: CourseStoreable](crs: ContextId, usrs: List[UserId]): Map[Long, T] =
    val entities = dao.loadUsersStorage(crs.id, usrs.map(_.id))

    entities
      .map(e =>
        val usr  = UserId(e.getUserId)
        val data = e.getStoragedData
        usr.id -> decodeField[T](crs, Some(usr), data)
      )
      .toMap
  end get

  override def modify[T: CourseStoreable](crs: ContextId, usr: UserId)(mod: T => T): T =
    val entity  = dao.ensureAndLoadUserStorageForUpdate(crs.id, usr.id)
    val data    = Option(entity.getStoragedData).getOrElse(Json.jEmptyObject)
    val decoded = decodeField[T](crs, None, data)
    val modded  = mod(decoded)
    entity.setStoragedData(put(data, modded))
    modded
end CourseStorageServiceImpl

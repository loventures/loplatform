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

import scalaz.syntax.std.option.*

/** This service is a collection of utilities around interacting with Course Storage without the restriction of having
  * the current shape of the stored data in any type.
  *
  * This is useful mainly for sys scripts and upgrade jobs and should not be used for application code.
  */
@Service
class RawCourseStorageService(dao: impl.CourseStorageDao):

  def getRaw(crs: ContextId): Option[Json] =
    dao.loadCourseStorage(crs.id).map(_.getStoragedData)

  def modifyRaw[T: CourseStoreable](crs: ContextId)(mod: Json => T): T =
    val entity = dao.ensureAndLoadCourseStorageForUpdate(crs.id)
    val data   = Option(entity.getStoragedData) | Json.jEmptyObject
    val modded = mod(data)
    entity.setStoragedData(put(data, modded))
    modded

  def getRaw(course: ContextId, usr: UserId): Option[Json] =
    val crs = ContextId(course.getId)
    dao.loadUserStorage(crs.id, usr.id).map(_.getStoragedData)

  def modifyRaw[T: CourseStoreable](crs: ContextId, usr: UserId)(mod: Json => T): T =
    val entity = dao.ensureAndLoadUserStorageForUpdate(crs.id, usr.id)
    val data   = Option(entity.getStoragedData) | Json.jEmptyObject
    val modded = mod(data)
    entity.setStoragedData(put(data, modded))
    modded

  private def put[T](obj: Json, value: T)(implicit storeable: CourseStoreable[T]) =
    obj.withObject(_.+(storeable.key, storeable.codec.encode(value)))
end RawCourseStorageService

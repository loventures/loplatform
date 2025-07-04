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
package impl

import argonaut.Json
import com.learningobjects.cpxp.component.annotation.Service as Dao
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.{Comparison, QueryService}
import com.learningobjects.cpxp.service.user.UserId
import jakarta.persistence.LockTimeoutException
import loi.cp.context.ContextId
import loi.cp.lti.CourseColumnIntegrations
import scalaz.syntax.functor.*
import scaloi.misc.TryInstances.*
import scaloi.syntax.BooleanOps.*
import scaloi.syntax.TryOps.*
import scaloi.{Created, Gotten}

import scala.jdk.CollectionConverters.*
import scala.util.Success

@Dao
trait CourseStorageDao:
  def loadCourseStorage(courseId: Long): Option[CourseStorageEntity]
  def loadUserStorage(courseId: Long, userId: Long): Option[CourseUserStorageEntity]
  def loadUsersStorage(courseId: Long, userIds: List[Long]): List[CourseUserStorageEntity]

  def ensureAndLoadCourseStorageForUpdate(courseId: Long): CourseStorageEntity
  def ensureAndLoadUserStorageForUpdate(courseId: Long, userId: Long): CourseUserStorageEntity

@Dao
class CourseStorageDaoImpl(implicit
  fs: FacadeService,
  qs: QueryService,
) extends CourseStorageDao:

  def loadCourseStorage(courseId: Long): Option[CourseStorageEntity] =
    courseId.facade[CourseStorageParentFacade].getCourseStorage

  def loadUserStorage(courseId: Long, userId: Long): Option[CourseUserStorageEntity] =
    userId.facade[CourseUserStorageParentFacade].getCourseUserStorage(courseId)

  def loadUsersStorage(courseId: Long, userIds: List[Long]): List[CourseUserStorageEntity] =
    qs.queryParents(userIds.map(UserId(_)).asJava, CourseUserStorageFinder.ITEM_TYPE_COURSE_USER_STORAGE)
      .addCondition(CourseUserStorageFinder.DATA_TYPE_COURSE, Comparison.eq, courseId)
      .getFacades[CourseUserStorageFacade]
      .toList

  // fails if
  //   the courseId is not in groupfinder
  //   we can't lock the coursestoragefinder row
  def ensureAndLoadCourseStorageForUpdate(courseId: Long): CourseStorageEntity =
    (courseId.facade_![CourseStorageParentFacade] |<@~* notFound("course", courseId))
      .map(_.getOrCreateCourseStorage)
      .flatMap({
        case Gotten(csf)  => (
          csf.refresh(true)
            <@~* new LockTimeoutException(s"error locking storage for $courseId")
            >| csf
        )
        case Created(csf) =>
          csf.setStoragedData(Json.jEmptyObject)
          csf.setLtiColumnIntegrations(None)
          Success(csf)
      })
      .get

  // fails if
  //   the userId is not in userfinder
  //   we can't lock the courseuserstoragefinder row
  def ensureAndLoadUserStorageForUpdate(courseId: Long, userId: Long): CourseUserStorageEntity =
    (userId.facade_![CourseUserStorageParentFacade] |<@~* notFound("user", userId))
      .map(_.getOrCreateCourseUserStorage(courseId))
      .flatMap({
        case Gotten(cusf)  => (
          cusf.refresh(true)
            <@~* new LockTimeoutException(s"error locking storage for user $userId, course $courseId")
            >| cusf
        )
        case Created(cusf) =>
          cusf.setStoragedData(Json.jEmptyObject)
          Success(cusf)
      })
      .get

  private def notFound(what: String, where: Long) =
    new NoSuchElementException(s"$what not found: $where")
end CourseStorageDaoImpl

object CourseStorageDao:
  // Finds an entry in 'data' of type [T] and extracts and decodes that value
  def decodeField[T](course: ContextId, user: Option[UserId], data: Json)(implicit T: CourseStoreable[T]): T =
    data.field(T.key) match
      case Some(d) =>
        T.codec
          .decodeJson(d)
          .fold(
            (message, history) => throw UnstoragingError(T.key, message, course.id, user.map(_.id), history),
            decoded => decoded
          )
      case None    => T.empty

  def put[T](obj: Json, value: T)(implicit T: CourseStoreable[T]) =
    obj.withObject(_.+(T.key, T.codec.encode(value)))

  def decodeColumnIntegrations(course: ContextId, data: Json): CourseColumnIntegrations =
    CourseColumnIntegrations.codec
      .decodeJson(data)
      .fold(
        (message, history) => throw UnstoragingError(CourseColumnIntegrations.key, message, course.id, None, history),
        decoded => decoded
      )
end CourseStorageDao

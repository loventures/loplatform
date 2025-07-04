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
import com.learningobjects.cpxp.dto.*
import com.learningobjects.cpxp.service.group.GroupFinder
import com.learningobjects.cpxp.service.user.UserFinder
import scaloi.GetOrCreate

// the seen...

trait CourseStorageEntity:
  def refresh(lock: Boolean): Boolean

  @FacadeData(CourseStorageFinder.DATA_TYPE_COURSE_STORAGE)
  def getStoragedData: Json
  def setStoragedData(data: Json): Unit

  @FacadeData(CourseStorageFinder.DATA_TYPE_LTI_COLUMNS)
  def getLtiColumnIntegrations: Option[Json]
  def setLtiColumnIntegrations(data: Option[Json]): Unit
end CourseStorageEntity

trait CourseUserStorageEntity:
  def refresh(lock: Boolean): Boolean

  def getUserId: Long

  @FacadeData(CourseUserStorageFinder.DATA_TYPE_COURSE_USER_STORAGE)
  def getStoragedData: Json
  def setStoragedData(data: Json): Unit

// ... and the unseen

@FacadeItem(GroupFinder.ITEM_TYPE_GROUP)
private[impl] trait CourseStorageParentFacade extends Facade:
  @FacadeChild
  def getCourseStorage: Option[CourseStorageFacade]
  def getOrCreateCourseStorage: GetOrCreate[CourseStorageFacade]

@FacadeItem(UserFinder.ITEM_TYPE_USER)
private[impl] trait CourseUserStorageParentFacade extends Facade:
  import CourseUserStorageFinder.*

  @FacadeChild
  def getCourseUserStorage(
    @FacadeCondition(DATA_TYPE_COURSE) course: Long,
  ): Option[CourseUserStorageFacade]
  def getOrCreateCourseUserStorage(
    @FacadeCondition(DATA_TYPE_COURSE) course: Long,
  ): GetOrCreate[CourseUserStorageFacade]
end CourseUserStorageParentFacade

@FacadeItem(CourseStorageFinder.ITEM_TYPE_COURSE_STORAGE)
private[impl] trait CourseStorageFacade extends Facade with CourseStorageEntity

@FacadeItem(CourseUserStorageFinder.ITEM_TYPE_COURSE_USER_STORAGE)
private[impl] trait CourseUserStorageFacade extends Facade with CourseUserStorageEntity:
  override def getUserId: Long = this.getParentId

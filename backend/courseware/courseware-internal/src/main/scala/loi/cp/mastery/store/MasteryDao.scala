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

package loi.cp.mastery.store

import argonaut.{DecodeJson, EncodeJson}
import com.learningobjects.cpxp.component.annotation.Service as Dao
import com.learningobjects.cpxp.dto.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.user.UserFinder
import loi.cp.mastery.UserMasteryState
import scaloi.GetOrCreate

@Dao
class MasteryDao()(implicit fs: FacadeService):

  def getMasteryFacade(userId: Long, courseId: Long): Option[UserMasteryFacade] =
    userId.facade[UserMasteryParentFacade].getUserMastery(courseId)

  def getMasteryFacadeForUpdate(userId: Long, courseId: Long): UserMasteryFacade =
    val user = userId.facade[UserMasteryParentFacade]
    user
      .getOrCreateUserMastery(
        courseId,
        um =>
          um.setCourse(courseId)
          um.setState(UserMasteryState.Empty)
      )
      .update(_.lock(true)) // lock it if we got it
      .result
  end getMasteryFacadeForUpdate
end MasteryDao

@FacadeItem(UserFinder.ITEM_TYPE_USER)
private trait UserMasteryParentFacade extends Facade:
  @FacadeChild
  def getUserMastery(
    @FacadeCondition(UserMasteryFinder.DATA_TYPE_USER_MASTERY_COURSE) course: Long,
  ): Option[UserMasteryFacade]

  def getOrCreateUserMastery(
    @FacadeCondition(UserMasteryFinder.DATA_TYPE_USER_MASTERY_COURSE) course: Long,
    init: UserMasteryFacade => Unit,
  ): GetOrCreate[UserMasteryFacade]
end UserMasteryParentFacade

@FacadeItem(UserMasteryFinder.ITEM_TYPE_USER_MASTERY)
trait UserMasteryFacade extends Facade:
  @FacadeData(UserMasteryFinder.DATA_TYPE_USER_MASTERY_COURSE)
  def setCourse(course: Long): Unit

  @FacadeData(UserMasteryFinder.DATA_TYPE_USER_MASTERY_STATE)
  def getState(implicit DecodeJson: DecodeJson[UserMasteryState]): UserMasteryState
  def setState(state: UserMasteryState)(implicit EncodeJson: EncodeJson[UserMasteryState]): Unit

  def lock(pessimistic: Boolean): Unit
end UserMasteryFacade

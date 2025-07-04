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

package loi.cp.user

import argonaut.Json
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.facade.FacadeService
import scaloi.{Created, Gotten}

@Service
class UserStorageDao(implicit fs: FacadeService):

  def loadUserStorage(userId: Long): Option[UserStorageFacade] = userId.facade[UserStorageParentFacade].getUserStorage

  def loadUserStorageForUpdate(userId: Long): Either[String, UserStorageFacade] =
    for
      user    <- userId.facade_?[UserStorageParentFacade].toRight(s"no such user $userId")
      storage <- getOrCreateForUpdate(user)
    yield storage

  private def getOrCreateForUpdate(user: UserStorageParentFacade): Either[String, UserStorageFacade] =
    user.getOrCreateUserStorage match
      case Gotten(userStorage)  =>
        Either.cond(
          userStorage.refresh(true),
          userStorage,
          s"could not obtain lock on user storage ${userStorage.getId}"
        )
      case Created(userStorage) =>
        userStorage.setStoragedData(Json.jEmptyObject)
        Right(userStorage)
end UserStorageDao

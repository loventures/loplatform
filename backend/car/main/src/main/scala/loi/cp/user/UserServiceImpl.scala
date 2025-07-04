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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.{Comparison, QueryBuilder, Function as QBFunction}
import com.learningobjects.cpxp.service.user.{UserService as _, *}
import com.learningobjects.cpxp.util.Ids
import loi.cp.right.{Right, RightMatch, RightService}
import scaloi.syntax.ClassTagOps.classTagClass

import scala.reflect.ClassTag

@Service
class UserServiceImpl(
  domainDto: => DomainDTO,
  rightService: RightService
)(implicit
  fs: FacadeService,
) extends UserService:

  override def getUser(id: Long): Option[UserDTO] =
    Option(id.facade[UserFacade]).map(UserDTO.apply)

  override def getUser(usernameOrEmail: String): Option[UserDTO] =
    val userFolder = UserConstants.ID_FOLDER_USERS.facade[UserFolderFacade]
    Option(userFolder.findUserByUsername(usernameOrEmail))
      .orElse(Option(userFolder.findUserByEmailAddress(usernameOrEmail)))
      .map(UserDTO.apply)

  override def getUsers(ids: Iterable[Long]): Map[Long, UserDTO] =
    ids.facades[UserFacade].map(user => user.getId.longValue -> UserDTO(user)).toMap

  override def userHasDomainRight[A <: Right: ClassTag](userId: Long): Boolean =
    rightService.getUserHasRight(domainDto, Ids.of(userId), classTagClass[A], RightMatch.EXACT)

  override def getUsersByUsername(usernames: Iterable[String]): Map[String, UserDTO] =
    queryActiveUsers
      .addCondition(UserConstants.DATA_TYPE_USER_NAME, Comparison.in, usernames, QBFunction.LOWER)
      .getFacades[UserFacade]
      .map(user => user.getUserName -> UserDTO(user))
      .toMap

  override def queryActiveUsers: QueryBuilder =
    UserConstants.ID_FOLDER_USERS
      .facade[UserFolderFacade]
      .queryUsers
      .addCondition(UserConstants.DATA_TYPE_DISABLED, Comparison.eq, false)
end UserServiceImpl

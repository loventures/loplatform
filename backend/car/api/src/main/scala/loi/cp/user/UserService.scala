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
import com.learningobjects.cpxp.service.query.QueryBuilder
import com.learningobjects.cpxp.service.user.*
import loi.cp.right.Right

import scala.reflect.ClassTag

@Service
trait UserService:

  /** Gets the user with the given id. The user can be found in any domain.
    * @param id
    *   the user id to get
    * @return
    *   the user
    */
  def getUser(id: Long): Option[UserDTO]

  /** Gets the user with the given username or email address in the current domain.
    * @param usernameOrEmail
    *   the username or email to get
    * @return
    *   the user
    */
  def getUser(usernameOrEmail: String): Option[UserDTO]

  /** Gets users with the given ids. The users can be found in any domain. Ids that are not found are not in the
    * returned map.
    *
    * @param ids
    *   the ids of users to get
    * @return
    *   the users, indexed by id
    */
  def getUsers(ids: Iterable[Long]): Map[Long, UserDTO]

  /** Gets users with the given usernames. Usernames that are not found are not returned in the map.
    *
    * Note: This differs from [[UserService.getUsers()]] in that it is domain aware
    *
    * @param usernames
    *   the usernames to get
    * @return
    *   users indexed by username
    */
  def getUsersByUsername(usernames: Iterable[String]): Map[String, UserDTO]

  /** Whether or not the given user id has the given right in the current domain.
    *
    * @param userId
    *   the user id to check
    * @tparam A
    *   the right to check
    * @return
    *   true if the user has the right in the domain, false otherwise
    */
  // Should be on RightService, but RightService isn't en-trait-ened
  def userHasDomainRight[A <: Right: ClassTag](userId: Long): Boolean

  def queryActiveUsers: QueryBuilder
end UserService

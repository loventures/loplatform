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

package loi.cp.user.web

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.user.UserService
import scalaz.\/
import scalaz.std.list.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*

@Service
class UserWebUtils(userService: UserService):

  def loadUser(id: Long): String \/ UserDTO =
    userService.getUser(id).toRightDisjunction(s"No such user $id")

  /** @return
    *   all the users in `ids` or a message saying which ones are missing
    */
  def loadUsers(ids: List[Long]): String \/ List[UserDTO] =
    val users = userService.getUsers(ids)
    ids
      .traverse(id => users.get(id).toSuccessNel(id))
      .leftMap(missing => s"No such users: ${missing.toList.mkString(",")}")
      .toDisjunction
end UserWebUtils

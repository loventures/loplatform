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

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.service.user.UserState
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.user.SudoAdminRight
import loi.cp.admin.right.{CourseAdminRight, UserAdminRight}
import loi.cp.course.right.ManageCoursesReadRight
import loi.cp.user.UserRootApi.Transition
import scalaz.*
import scaloi.GetOrCreate

@Controller(value = "users", root = true, category = Controller.Category.USERS)
@RequestMapping(path = "users")
trait UserRootApi extends ApiRootComponent:
  @RequestMapping(path = "self", method = Method.GET)
  @Secured(allowAnonymous = true)
  def getSelf: Option[UserComponent]

  @RequestMapping(method = Method.GET)
  @Secured(value = Array(classOf[UserAdminRight], classOf[CourseAdminRight], classOf[ManageCoursesReadRight]))
  def getUsers(query: ApiQuery): ApiQueryResults[UserComponent]

  @RequestMapping(path = "{id}", method = Method.GET)
  @Secured(Array(classOf[UserAdminRight], classOf[CourseAdminRight], classOf[ManageCoursesReadRight]))
  def getUser(@PathVariable("id") id: Long): Option[UserComponent]

  @RequestMapping(method = Method.POST)
  @Secured(Array(classOf[UserAdminRight]))
  def create(@RequestBody user: UserComponent.Init): ErrorResponse \/ UserComponent

  @RequestMapping(path = "{id}", method = Method.PUT)
  @Secured(Array(classOf[UserAdminRight]))
  def update(@PathVariable("id") id: Long, @RequestBody user: UserComponent.Init): ErrorResponse \/ UserComponent

  @RequestMapping(path = "{id}", method = Method.DELETE)
  @Secured(Array(classOf[UserAdminRight]))
  def delete(@PathVariable("id") id: Long): ErrorResponse \/ Unit

  @RequestMapping(method = Method.DELETE)
  @Secured(Array(classOf[UserAdminRight]))
  def deleteBatch(
    @QueryParam(value = "id", required = true, decodeAs = classOf[Long]) ids: List[Long]
  ): ErrorResponse \/ Unit

  @RequestMapping(path = "{id}/transition", method = Method.POST)
  @Secured(Array(classOf[UserAdminRight]))
  def transition(@PathVariable("id") id: Long, @RequestBody transition: Transition): ErrorResponse \/ Unit

  @RequestMapping(path = "transition", method = Method.POST)
  @Secured(Array(classOf[UserAdminRight]))
  def transitionBatch(
    @QueryParam(value = "id", decodeAs = classOf[Long]) id: List[Long],
    @RequestBody transition: Transition
  ): ErrorResponse \/ Unit

  @RequestMapping(path = "{id}/sudo", method = Method.POST)
  @Secured(Array(classOf[SudoAdminRight]))
  def sudo(@PathVariable("id") id: Long, @QueryParam(required = false) returnUrl: Option[String]): ErrorResponse \/ Unit

  @RequestMapping(path = "{id}/logout", method = Method.POST)
  @Secured(Array(classOf[UserAdminRight]))
  def logout(@PathVariable("id") id: Long): ErrorResponse \/ Unit

  @RequestMapping(path = "domainRoles", method = Method.GET)
  @Secured(Array(classOf[UserAdminRight]))
  def getDomainRoles: List[UserRootApi.Role]

  @RequestMapping(path = "adminReport", method = Method.GET)
  @Secured(Array(classOf[UserAdminRight]))
  def downloadAdminReport(request: WebRequest): WebResponse

  def getOrCreateUser(user: UserComponent.Init): GetOrCreate[UserComponent]
end UserRootApi

object UserRootApi:

  case class Transition(state: UserState)

  case class Role(id: Long, name: String, admin: Boolean, superior: Boolean)

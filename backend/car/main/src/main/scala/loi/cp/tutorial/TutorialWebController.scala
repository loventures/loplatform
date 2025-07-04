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

package loi.cp.tutorial

import argonaut.Json
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.exception.ResourceNotFoundException
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.AdminRight
import loi.cp.domain.DomainStorageService
import loi.cp.user.UserStorageService

@Component
@Controller(root = true, value = "tutorial-web-controller")
class TutorialWebController(
  ci: ComponentInstance,
  domainDto: => DomainDTO,
  domainStorageService: DomainStorageService,
  userDto: => UserDTO,
  userStorageService: UserStorageService,
) extends BaseComponent(ci)
    with ApiRootComponent:

  @RequestMapping(method = Method.GET, path = "tutorials")
  def getTutorials: Json =
    domainStorageService.get[TutorialAdminData](domainDto).tutorials

  @RequestMapping(method = Method.GET, path = "tutorials/status")
  def getTutorialStatus: Map[String, TutorialInfo] =
    userStorageService.get[TutorialUserData](userDto).tutorials

  @RequestMapping(method = Method.PUT, path = "tutorials/{name}/status")
  def setStatus(@PathVariable("name") name: String, @RequestBody status: TutorialStatus): Map[String, TutorialInfo] =

    if name.length > 256 then throw new ResourceNotFoundException("not a valid tutorial name")

    val result = userStorageService.modify[TutorialUserData](userDto)(tutorialData =>
      tutorialData.copy(tutorials = tutorialData.tutorials.updated(name, TutorialInfo(status)))
    )

    result.tutorials
  end setStatus

  @Secured(value = Array(classOf[AdminRight]))
  @RequestMapping(method = Method.PUT, path = "admin/tutorials")
  def setTutorials(@RequestBody tutorials: Json): Unit =
    domainStorageService.modify[TutorialAdminData](domainDto)(tutorialData => tutorialData.copy(tutorials = tutorials))
end TutorialWebController

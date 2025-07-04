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

package loi.cp.integration

import com.learningobjects.cpxp.component.annotation.{Controller, PathVariable, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method, WebResponse}
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.{LtiAdminRight, ViewLtiToolRight}
import loi.cp.ltitool.LtiToolComponent

@Controller(value = "ltiTools", root = true, category = Controller.Category.API_SUPPORT)
trait LtiToolRootComponent extends ApiRootComponent:

  @RequestMapping(path = "ltiTools", method = Method.POST)
  @Secured(Array(classOf[LtiAdminRight]))
  def addLtiTool(@RequestBody createLtiToolDto: CreateLtiToolDto): LtiToolComponent

  @RequestMapping(path = "ltiTools/{id}", method = Method.PUT)
  @Secured(Array(classOf[LtiAdminRight]))
  def updateLtiTool(
    @PathVariable("id") id: Long,
    @RequestBody createLtiToolDto: CreateLtiToolDto
  ): Option[LtiToolComponent]

  @RequestMapping(path = "ltiTools", method = Method.GET)
  @Secured(Array(classOf[LtiAdminRight], classOf[ViewLtiToolRight]))
  def getLtiTools(query: ApiQuery): ApiQueryResults[LtiToolComponent]

  @RequestMapping(path = "ltiTools/{id}", method = Method.GET)
  @Secured(Array(classOf[LtiAdminRight], classOf[ViewLtiToolRight]))
  def getLtiTool(@PathVariable("id") id: Long): Option[LtiToolComponent]

  @RequestMapping(path = "ltiTools/{id}", method = Method.DELETE)
  @Secured(Array(classOf[LtiAdminRight]))
  def deleteLtiTool(@PathVariable("id") id: Long): WebResponse
end LtiToolRootComponent

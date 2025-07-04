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

package loi.cp.course

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ErrorResponse, Method}
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.de.authorization.{Secured, SecuredService}
import loi.authoring.project.AccessRestriction
import loi.authoring.security.right.EditSettingsAnyProjectRight
import loi.authoring.web.AuthoringApiWebUtils
import loi.cp.config.ConfigurationRootApi.ConfigOut
import loi.cp.config.{ConfigurationKey, ConfigurationKeyBinding}
import scalaz.\/
import scalaz.syntax.either.*
import scalaz.syntax.functor.*
import scalaz.syntax.std.option.*

/** Specialization of ConfigurationRootApi for CoursePreferences */
@Component
@Controller(value = "course-config", root = true)
@Secured(allowAnonymous = true) // we do our own security
class CourseConfigurationWebController(
  ci: ComponentInstance,
  courseConfigurationService: CourseConfigurationService,
  securedService: SecuredService,
  webUtils: AuthoringApiWebUtils,
)(implicit componentService: ComponentService)
    extends BaseComponent(ci)
    with ApiRootComponent:

  @RequestMapping(path = "config/coursePreferences/item/{item}", method = Method.GET)
  def getItemConfig(@PathVariable("item") item: Long): ErrorResponse \/ ConfigOut =
    for
      _     <- ensurePermitted(_.read(), item)
      group <- item.component_?[CourseComponent].toRightDisjunction(ErrorResponse.notFound)
    yield
      val config = courseConfigurationService.getGroupDetail(CoursePreferences, group)
      ConfigOut.create(config)

  // We have two request mappings `{key}/item/{item}` and `{key}/{item}` that do the almost the same thing.
  // Why? No idea.
  // Why is `{key}/{item}` suffixed with "nonAdmin"? No idea.
  // But I have to override ConfigurationRootApi's shenanigans. When in Rome...
  // So in theory only an admin ever should know about the stacking of configs, a human just needs the effective.
  // But even then it seems wrong that course config is allowAnonymous
  @RequestMapping(path = "config/coursePreferences/{item}", method = Method.GET)
  def getItemConfigNonAdmin(@PathVariable("item") item: Long): ErrorResponse \/ JsonNode =
    getItemConfig(item).map(_.effective)

  @RequestMapping(path = "config/coursePreferences/project/{id}", method = Method.GET)
  def getProjectConfig(@PathVariable("id") projectId: Long): ErrorResponse \/ ConfigOut =
    for _ <- ensurePermitted(_.read(), projectId)
    yield
      val config = courseConfigurationService.getProjectDetail(CoursePreferences, projectId)
      ConfigOut.create(config)

  @RequestMapping(path = "config/coursePreferences/{item}", method = Method.PUT)
  def setGroupConfig(@PathVariable("item") item: Long, @RequestBody value: Option[JsonNode]): ErrorResponse \/ Unit =
    for
      _      <- ensurePermitted(_.write(), item)
      group  <- item.component_?[CourseComponent].toRightDisjunction(ErrorResponse.notFound)
      result <-
        courseConfigurationService.setGroupConfig(CoursePreferences, group, value).leftMap(ErrorResponse.badRequest)
    yield ()

  @RequestMapping(path = "config/coursePreferences/project/{id}", method = Method.PUT)
  def setProjectConfig(
    @PathVariable("id") projectId: Long,
    @RequestBody value: Option[JsonNode]
  ): ErrorResponse \/ Unit =
    val project = webUtils.projectOrThrow404(projectId, AccessRestriction.projectMemberOr[EditSettingsAnyProjectRight])
    courseConfigurationService
      .setProjectConfig(CoursePreferences, project.id, value)
      .leftMap(ErrorResponse.badRequest)
      .void
  end setProjectConfig

  private def ensurePermitted(
    security: ConfigurationKeyBinding => Secured,
    item: Long
  ): ErrorResponse \/ Unit =
    val binding = ConfigurationKey.configBindingA[CoursePreferences.type]
    if securedService.isPermitted(security(binding), Some(item)) then ().right
    else ErrorResponse.forbidden.left
end CourseConfigurationWebController

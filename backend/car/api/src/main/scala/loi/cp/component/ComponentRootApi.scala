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

package loi.cp.component

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.annotation.{Controller, PathVariable, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method, WebResponse}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.AdminRight

import scala.annotation.meta.getter

@Controller(value = "components", root = true, category = Controller.Category.API_SUPPORT)
@Secured(Array(classOf[AdminRight]))
@RequestMapping(path = "components")
trait ComponentRootApi extends ApiRootComponent:
  import ComponentRootApi.*

  @RequestMapping(method = Method.GET)
  def getComponents(query: ApiQuery): ApiQueryResults[ComponentComponent]

  @RequestMapping(path = "{identifier}", method = Method.GET)
  def getComponent(@PathVariable("identifier") identifier: String): Option[ComponentComponent]

  /** Apply the specified configuration to the component environment, ensuring that all components in the 'enable' list
    * are enabled, and all components in the 'disable' list are disabled.
    *
    * @return
    *   A {@code Configuration} object, which can be passed back in to undo the changes that were made.
    */
  @RequestMapping(path = "configure", method = Method.POST)
  def configureComponents(@RequestBody config: Configuration): Configuration

  @RequestMapping(path = "nodes", method = Method.GET)
  def getComponentRings: List[RingDTO]

  @RequestMapping(path = "toggle", method = Method.POST)
  def toggleComponent(@RequestBody toggleDTO: ToggleDTO): WebResponse

  @RequestMapping(path = "delete/{identifier}", method = Method.POST)
  def deleteArchive(@PathVariable("identifier") identifier: String): WebResponse

  @RequestMapping(path = "install", method = Method.POST)
  def installArchive(@RequestBody installDTO: InstallDTO): WebResponse

  @RequestMapping(path = "setConfig", method = Method.POST)
  def setConfig(@RequestBody configDTO: ConfigDTO): WebResponse
end ComponentRootApi

object ComponentRootApi:
  case class Configuration(enable: List[String], disable: List[String])
  case class ToggleDTO(identifier: String, enabled: Boolean)
  case class InstallDTO(uninstall: Boolean, uploadInfo: UploadInfo)
  case class ConfigDTO(id: String, config: String)

  sealed abstract class Node(@(JsonProperty @getter)("type") val tpe: String)

  final case class RingDTO(label: String, children: Seq[ArchiveDTO]) extends Node("ring")

  final case class ArchiveDTO(
    branch: String,
    buildDate: String,   // Instant
    buildNumber: String, // Int
    children: Seq[PackageDTO],
    disabled: Boolean,
    id: String,
    label: String,
    removable: Boolean,
    revision: String,
    version: String,
  ) extends Node("archive")

  final case class PackageDTO(label: String, children: Seq[ComponentDTO]) extends Node("package")

  final case class ComponentDTO(
    configuration: Option[String], // Option[JsonNode]
    description: Option[String],
    disabled: Boolean,
    id: String,
    label: String,
    version: String,
  ) extends Node("component")
end ComponentRootApi

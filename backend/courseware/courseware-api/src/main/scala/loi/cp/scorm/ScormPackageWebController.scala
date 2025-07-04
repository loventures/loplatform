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

package loi.cp.scorm

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.de.authorization.Secured
import loi.cp.course.right.ManageLibrariesAdminRight
import scalaz.\/

@Controller(value = "scorm/package", root = true)
@RequestMapping(path = "scorm/package")
@Secured(Array(classOf[ManageLibrariesAdminRight]))
trait ScormPackageWebController extends ApiRootComponent:
  import ScormPackageWebController.*

  @RequestMapping(method = Method.POST)
  def generateScormPackage(@RequestBody request: ScormPackageRequest): ErrorResponse \/ String

  @RequestMapping(method = Method.GET, path = "{id}")
  def downloadScormPackage(@PathVariable("id") id: String): ErrorResponse \/ WebResponse

  @RequestMapping(method = Method.POST, path = "batch", async = true)
  def generateScormPackages(@RequestBody request: BatchScormPackageRequest): String

  @RequestMapping(method = Method.GET, path = "batch/{guid}")
  def downloadScormPackages(@PathVariable("guid") guid: String): ErrorResponse \/ WebResponse
end ScormPackageWebController

object ScormPackageWebController:
  final case class ScormPackageRequest(
    offeringId: Long,
    systemId: Long,
    scormFormat: ScormFormat
  )
  final case class BatchScormPackageRequest(
    productCodes: List[String],
    systemId: Long,
    scormFormat: ScormFormat
  )
end ScormPackageWebController

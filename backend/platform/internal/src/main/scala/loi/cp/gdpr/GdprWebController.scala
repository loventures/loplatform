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

package loi.cp.gdpr

import com.github.tototoshi.csv.CSVReader
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.de.authorization.Secured
import loi.cp.overlord.OverlordRight
import scala.util.Using
import scalaz.std.list.*
import scaloi.syntax.option.*

@Component
@Controller(root = true)
@Secured(Array(classOf[OverlordRight]))
private[gdpr] class GdprWebController(
  val componentInstance: ComponentInstance,
  gdprService: GdprService,
) extends ApiRootComponent
    with ComponentImplementation:

  @RequestMapping(path = "gdpr/inactiveUserCount", method = Method.GET)
  def countInactiveUsers(@QueryParam("minors") upload: Option[UploadInfo]): Int =
    gdprService.countInactiveUsers(upload.foldZ(parseMinors))

  @RequestMapping(
    path = "gdpr/purgeInactiveUsers",
    method = Method.POST,
    async = true,
  )
  def purgeInactiveUsers(@QueryParam("minors") upload: Option[UploadInfo]): Unit =
    gdprService.purgeInactiveUsers(upload.foldZ(parseMinors))

  private def parseMinors(upload: UploadInfo): List[String] =
    Using.resource(CSVReader.open(upload.getFile)) { csv =>
      csv.all().map(_.head)
    }
end GdprWebController

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

package loi.cp.zip

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.learningobjects.cpxp.component.annotation.{Controller, PathVariable, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.MediaAdminRight
import loi.cp.right.RightBinding
import scalaz.\/

import java.lang as jl

@Controller(value = "zip-sites", root = true)
@RequestMapping(path = "zipSites")
@Secured(Array(classOf[ZipSiteAdminRight]))
trait ZipSiteApi extends ApiRootComponent:
  import ZipSiteApi.*

  @RequestMapping(method = Method.GET)
  def queryZipSites(aq: ApiQuery): ApiQueryResults[ZipSite]

  @RequestMapping(path = "{zipSiteId}", method = Method.GET)
  def getZipSite(@PathVariable("zipSiteId") id: Long): Option[ZipSite]

  @RequestMapping(method = Method.POST)
  def createZipSite(@RequestBody site: ZipSiteInit): ErrorResponse \/ ZipSite

  @RequestMapping(path = "{zipSiteId}", method = Method.PUT)
  def updateZipSite(@PathVariable("zipSiteId") id: Long, @RequestBody site: ZipSiteUpdate): ErrorResponse \/ ZipSite

  @RequestMapping(path = "{zipSiteId}", method = Method.DELETE)
  def deleteZipSite(@PathVariable("zipSiteId") id: Long): ErrorResponse \/ NoContentResponse
end ZipSiteApi

@RightBinding(name = "Zip Site Administrator", description = "Manage zip sites for a domain.")
abstract class ZipSiteAdminRight extends MediaAdminRight

object ZipSiteApi:

  /* These are both instances of the same data structure, parameterized on
   * Id and Option, respectively, but doing so saddens Jackson for now. */

  final case class ZipSiteInit(
    name: String,
    path: String,
    site: UploadInfo,
    disabled: Option[Boolean] = Some(true),
  )

  final case class ZipSiteUpdate(
    name: Option[String],
    path: Option[String],
    site: Option[UploadInfo],
    @JsonDeserialize(contentAs = classOf[jl.Long])
    revision: Option[Long],
    @JsonDeserialize(contentAs = classOf[jl.Boolean])
    disabled: Option[Boolean],
  )
end ZipSiteApi

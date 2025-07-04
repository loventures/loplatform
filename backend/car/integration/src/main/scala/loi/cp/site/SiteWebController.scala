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

package loi.cp.site

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQueries, ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.dto.Ontology
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.site.SiteFinder
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.IntegrationAdminRight
import scalaz.\/
import scalaz.syntax.std.option.*

//noinspection ScalaUnusedSymbol
@Component
@Controller(root = true)
@Secured(value = Array(classOf[IntegrationAdminRight]))
class SiteWebController(
  val componentInstance: ComponentInstance,
  siteService: SiteService,
)(implicit is: ItemService, ontology: Ontology)
    extends ApiRootComponent
    with ComponentImplementation:
  @DeIgnore
  protected def this() = this(null, null)(using null, null)

  @RequestMapping(path = "sites/{id}", method = Method.GET)
  def getSite(
    @PathVariable("id") id: Long,
  ): Option[SiteDto] =
    getSites(ApiQuery.byId(id)).asOption()

  @RequestMapping(path = "sites", method = Method.GET)
  def getSites(
    query: ApiQuery,
  ): ApiQueryResults[SiteDto] =
    getSiteFinders(query).map(SiteDto.apply)

  @RequestMapping(path = "sites", method = Method.POST)
  def createSite(
    @RequestBody site: NewSite,
  ): ErrorResponse \/ SiteDto =
    siteService
      .getOrCreateSite(site.siteId, site.name, site.restricted)
      .map(SiteDto.apply)
      .createdOr(ErrorResponse.unprocessable(DuplicateSiteId(site.siteId)))

  @RequestMapping(path = "sites/{id}", method = Method.PUT)
  def updateSite(
    @PathVariable("id") id: Long,
    @RequestBody site: EditSite,
  ): ErrorResponse \/ SiteDto =
    for finder <- getSiteFinders(ApiQuery.byId(id)).asOption \/> ErrorResponse.notFound
    yield
      finder.name = site.name
      finder.restricted = site.restricted
      SiteDto(finder)

  @RequestMapping(path = "sites/{id}", method = Method.DELETE)
  def deleteSite(
    @PathVariable("id") id: Long,
  ): Unit =
    getSiteFinders(ApiQuery.byId(id)).asOption.foreach(is.delete)

  private def getSiteFinders(query: ApiQuery): ApiQueryResults[SiteFinder] =
    ApiQueries.queryFinder[SiteFinder](siteService.querySites(), query)
end SiteWebController

private final case class DuplicateSiteId(duplicateSiteId: String)

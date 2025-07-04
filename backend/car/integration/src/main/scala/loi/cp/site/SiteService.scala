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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.dto.{FacadeChild, FacadeCondition}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.folder.FolderFacade
import com.learningobjects.cpxp.service.query.{QueryBuilder, QueryService, Function as QFunction}
import com.learningobjects.cpxp.service.site.SiteFinder
import loi.cp.admin.FolderParentFacade
import scaloi.GetOrCreate

@Service
class SiteService(domain: => DomainDTO)(implicit
  fs: FacadeService,
  qs: QueryService,
):
  def getOrCreateSite(siteId: String, name: String, restricted: Boolean): GetOrCreate[SiteFinder] =
    siteFolder.getOrCreateSiteBySiteId(siteId) { finder =>
      finder.name = name
      finder.restricted = restricted
    }

  def querySites(): QueryBuilder =
    siteFolder.queryChildren[SiteFinder]

  def siteFolder: SiteFolderFacade =
    domain.facade[FolderParentFacade].findFolderByType(SiteFolderFacade.Type).facade[SiteFolderFacade]
end SiteService

trait SiteFolderFacade extends FolderFacade:
  @FacadeChild(SiteFinder.Type)
  def getOrCreateSiteBySiteId(@FacadeCondition(value = SiteFinder.SiteId, function = QFunction.LOWER) siteId: String)(
    init: SiteFinder => Unit
  ): GetOrCreate[SiteFinder]

object SiteFolderFacade:
  final val Type = "site"

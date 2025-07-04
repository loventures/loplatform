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

package loi.cp.admin_pages

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.facade.FacadeService
import loi.cp.admin.AdminSiteService

import scala.jdk.CollectionConverters.*

@Component
class AdminPagesApiImpl(val componentInstance: ComponentInstance, _adminSiteService: AdminSiteService)(implicit
  fs: FacadeService
) extends AdminPagesApi
    with ComponentImplementation:

  override def getAdminPages: AdminPagesInfo =
    val adPages = _adminSiteService.getAdminPages.asScala.view
      .mapValues(comps =>
        comps.asScala
          .map(comp =>
            AdminPage(
              description = Option(comp.getDescription),
              icon = Option(comp.getIcon),
              identifier = Option(comp.getIdentifier),
              name = Option(comp.getName)
            )
          )
          .toList
      )
      .toMap
    AdminPagesInfo(adminPages = adPages)
  end getAdminPages
end AdminPagesApiImpl

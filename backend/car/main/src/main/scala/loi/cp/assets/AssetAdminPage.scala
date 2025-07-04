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

package loi.cp.assets

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.{HtmlResponse, WebResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.HostingAdminRight
import loi.cp.admin.{AdminPageBinding, AdminPageComponent}

/** An overridden Admin Page Binding.
  *
  * TODO: Genericize this so that multiple tools can override this single component.
  */

@AdminPageBinding(
  group = "domain",
  secured = new Secured(value = Array(classOf[HostingAdminRight]))
)
@Component(
  name = "Asset Admin Tool",
  description = """Front-end component not yet deployed""",
  enabled = false
)
class AssetAdminPage(
  val componentInstance: ComponentInstance
) extends AdminPageComponent
    with ComponentImplementation:
  override def renderAdminPage(): WebResponse =
    HtmlResponse(this, "index.html") // the file in the deployed bundle
end AssetAdminPage

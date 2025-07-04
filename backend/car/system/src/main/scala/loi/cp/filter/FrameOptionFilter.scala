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

package loi.cp.filter

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.{FilterBinding, FilterComponent, FilterInvocation}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import loi.cp.config.ConfigurationService
import loi.cp.security.SecuritySettings
import loi.cp.security.SecuritySettings.FrameOption

@Component
@FilterBinding(priority = 800)
final class FrameOptionFilter(val componentInstance: ComponentInstance)(implicit
  cs: ConfigurationService
) extends FilterComponent
    with ComponentImplementation:
  import FrameOptionFilter.*

  override def filter(
    request: HttpServletRequest,
    response: HttpServletResponse,
    invocation: FilterInvocation
  ): Boolean =
    frameOptions.foreach(option => response.addHeader(FrameOptionHeader, option.toString))
    true

  def frameOptions = FrameOption(SecuritySettings.config.getDomain.frameOptions)
end FrameOptionFilter
object FrameOptionFilter:
  final val FrameOptionHeader = "X-Frame-Options"

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

package loi.cp.cdn

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.upgrade.ClusterSupport
import com.learningobjects.cpxp.util.CdnUtils

@Component
class CdnRootApiImpl(val componentInstance: ComponentInstance) extends CdnRootApi with ComponentImplementation:

  private final val logger = org.log4s.getLogger

  val sysInfo = ClusterSupport.clusterSystemInfo

  override def get: Int =
    sysInfo.getCdnVersion()

  override def refresh: Unit =
    logger debug s"CDN Version Before: " + sysInfo.getCdnVersion()
    CdnUtils.incrementCdnVersion()
    logger debug s"CDN Version After: " + sysInfo.getCdnVersion()
end CdnRootApiImpl

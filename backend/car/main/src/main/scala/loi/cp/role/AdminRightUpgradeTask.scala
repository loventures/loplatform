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

package loi.cp.role

import com.learningobjects.cpxp.service.integration.IntegrationWebService
import com.learningobjects.cpxp.startup.StartupTaskScope.AnyDomain
import com.learningobjects.cpxp.startup.{StartupTask, StartupTaskBinding}
import loi.cp.admin.right.{AdminRight, HostingAdminRight}

import scala.jdk.CollectionConverters.*

@StartupTaskBinding(version = 20171030, taskScope = AnyDomain)
class AdminRightUpgradeTask(iws: IntegrationWebService) extends StartupTask:
  override def run(): Unit =
    val adminName   = classOf[AdminRight].getName
    val hostingName = classOf[HostingAdminRight].getName
    for
      system     <- iws.getExternalSystems.asScala
      rightsJson <- Option(system.getRights)
    do system.setRights(rightsJson.replaceAll(adminName, hostingName))

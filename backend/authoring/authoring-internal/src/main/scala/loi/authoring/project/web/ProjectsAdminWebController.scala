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

package loi.authoring.project.web

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.service.domain.DomainFacade
import com.learningobjects.cpxp.service.overlord.OverlordWebService
import com.learningobjects.de.authorization.Secured
import loi.authoring.security.right.{AccessAuthoringAdminAppRight, CopyAnyProjectVersionRight}

import scala.jdk.CollectionConverters.*

@Component
@Controller(root = true, value = "authoring/projects/admin")
@Secured(Array(classOf[AccessAuthoringAdminAppRight]))
private[web] class ProjectsAdminWebController(
  ci: ComponentInstance,
  overlordWebService: OverlordWebService,
) extends BaseComponent(ci)
    with ApiRootComponent:

  @RequestMapping(path = "authoring/admin/domains", method = Method.GET)
  @Secured(Array(classOf[CopyAnyProjectVersionRight]))
  def getDomains: Seq[TargetDomainInfo] =
    overlordWebService.getAllDomains.asScala.toSeq
      .map(TargetDomainInfo.apply)
end ProjectsAdminWebController

private case class TargetDomainInfo(id: Long, name: String)
private object TargetDomainInfo:
  def apply(domain: DomainFacade) = new TargetDomainInfo(domain.getId, domain.getName)

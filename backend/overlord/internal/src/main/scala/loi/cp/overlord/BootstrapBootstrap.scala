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

package loi.cp.overlord

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.Component as Bootstraps
import com.learningobjects.cpxp.component.AbstractComponent as AbstractBootstrap
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainWebService}
import loi.cp.bootstrap.Bootstrap
import loi.cp.domain.OverlordDomainRootApi as Bootstrapper
import scaloi.syntax.OptionOps.*

/** A bootstrap phase that executes another well-known bootstrap script.
  */
@Bootstraps
class BootstrapBootstrap(
  domainWebService: DomainWebService,
  globalDomain: DomainDTO,
) extends AbstractBootstrap:

  @Bootstrap("core.domain.bootstrap")
  def bootstrap(bootstrap: BootstrapBootstrap.Bootstrap): Unit =
    val domain = Boxtion(domainWebService.getDomainById(bootstrap.domain))
      .getOrElse {
        throw new IllegalArgumentException(s"Unknown domain: ${bootstrap.domain}")
      }

    try
      component[Bootstrapper]
        .bootstrap(domain, bootstrap.profile, bootstrap.config)
    finally domainWebService.setupContext(globalDomain.id)
  end bootstrap
end BootstrapBootstrap

object BootstrapBootstrap:

  final case class Bootstrap(domain: String, profile: String, config: JsonNode)

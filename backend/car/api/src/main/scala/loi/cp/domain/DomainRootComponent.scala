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

package loi.cp.domain

import com.learningobjects.cpxp.component.annotation.{Controller, RequestMapping, Service}
import com.learningobjects.cpxp.component.util.LazySeq
import com.learningobjects.cpxp.component.web.{ApiRootComponent, CacheOptions, Method}
import com.learningobjects.cpxp.service.login.LoginComponent
import com.learningobjects.cpxp.util.Out
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.AdminRight

@Service
@Controller(value = "domain", root = true)
@RequestMapping(path = "domain")
trait DomainRootComponent extends ApiRootComponent:
  @Secured(allowAnonymous = true)
  @RequestMapping(method = Method.GET)
  def getDomain: DomainComponent

  @Secured(allowAnonymous = true)
  @RequestMapping(path = "loginMechanisms", method = Method.GET)
  def loginMechanisms(cacheOptions: Out[CacheOptions]): LazySeq[? <: LoginComponent]

  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "settings", method = Method.GET)
  def getDomainSettings: DomainSettingsComponent

  @Secured(allowAnonymous = true)
  @RequestMapping(path = "test", method = Method.POST, async = true)
  def testAsync: DomainComponent
end DomainRootComponent

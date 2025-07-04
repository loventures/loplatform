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

package loi.cp.reverseproxy

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.QueryService

@Component
class ReverseProxyRoot(
  val componentInstance: ComponentInstance,
)(implicit domain: DomainDTO, fs: FacadeService, qs: QueryService)
    extends ReverseProxyRootComponent
    with ComponentImplementation:
  import ReverseProxyRoot.*

  override def get(q: ApiQuery): ApiQueryResults[ReverseProxyComponent] =
    ApiQuerySupport
      .query(parent().queryReverseProxies, q, classOf[ReverseProxyComponent])

  override def get(id: Long): Option[ReverseProxyComponent] =
    parent().getReverseProxy(id)

  override def create[T <: ReverseProxyComponent](reverseProxy: T): T =
    parent().addReverseProxy(reverseProxy)

  private def parent(): ReverseProxyParentFacade =
    domain
      .getFolderByType(ReverseProxyFolderType)
      .facade[ReverseProxyParentFacade]
end ReverseProxyRoot

object ReverseProxyRoot:
  val ReverseProxyFolderType = "reverseProxies"

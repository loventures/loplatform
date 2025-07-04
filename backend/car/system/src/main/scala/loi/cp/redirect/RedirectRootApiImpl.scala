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

package loi.cp.redirect

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults, ApiQuerySupport}
import com.learningobjects.cpxp.component.web.{ErrorResponse, NoContentResponse, WebResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.QueryService
import scalaz.\/
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scaloi.syntax.AnyOps.*

@Component
class RedirectRootApiImpl(val componentInstance: ComponentInstance)(implicit
  fs: FacadeService,
  qs: QueryService,
  domain: DomainDTO
) extends RedirectRootApi
    with ComponentImplementation:
  import RedirectRootApiImpl.*

  override def get(apiQuery: ApiQuery): ApiQueryResults[Redirect] =
    ApiQuerySupport.query(redirectFolder.queryRedirects, apiQuery, classOf[Redirect])

  override def get(id: Long): Option[Redirect] =
    get(ApiQuery.byId(id)).asOption

  override def create(redirect: Redirect): ErrorResponse \/ Redirect =
    incrementGeneration()
    redirectFolder.addRedirect(redirect).right

  override def delete(id: Long): WebResponse =
    get(id).fold[WebResponse](ErrorResponse.notFound) { redirect =>
      incrementGeneration()
      redirect.delete()
      NoContentResponse
    }

  override def update(id: Long, redirect: Redirect): ErrorResponse \/ Redirect =
    for existing <- get(id) \/> ErrorResponse.notFound
    yield
      incrementGeneration()
      existing <| { _.update(redirect) }

  private def incrementGeneration(): Unit =
    redirectFolder.setGeneration(Option(redirectFolder.getGeneration).fold(1L)(1L + _))
end RedirectRootApiImpl

object RedirectRootApiImpl:
  val RedirectFolderType = "redirect"

  def redirectFolder(implicit domain: DomainDTO, fs: FacadeService, qs: QueryService): RedirectParentFacade =
    domain.getFolderByType(RedirectRootApiImpl.RedirectFolderType).facade[RedirectParentFacade]

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

package loi.cp.subtenant

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.{ApiQueries, ApiQuery, ApiQueryResults, PredicateOperator}
import com.learningobjects.cpxp.component.web.{ErrorResponse, NoContentResponse, WebResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.attachment.AttachmentComponent
import loi.cp.subtenant.Subtenant.SubtenantDTO
import scalaz.\/
import scalaz.syntax.std.option.*
import scaloi.syntax.AnyOps.*
import scaloi.syntax.BooleanOps.*

@Component
class SubtenantRootApiImpl(val componentInstance: ComponentInstance)(implicit
  domain: DomainDTO,
  user: UserDTO,
  fs: FacadeService
) extends SubtenantRootApi
    with ComponentImplementation:

  def subtenantFolder: SubtenantParentFacade = domain.facade[SubtenantParentFacade]

  override def get(id: Long): Option[Subtenant] =
    get(ApiQuery.byId(id, classOf[Subtenant])).asOption

  override def get(apiQuery: ApiQuery): ApiQueryResults[Subtenant] =
    ApiQueries.query[Subtenant](subtenantFolder.querySubtenants, sublet(apiQuery))

  override def getName(id: Long): Option[SubtenantName] = get(id).map(SubtenantName.apply)

  override def getNames(apiQuery0: ApiQuery): ApiQueryResults[SubtenantName] =
    val query = new ApiQuery.Builder(apiQuery0).addPropertyMappings(classOf[Subtenant]).build()
    val res   = get(query)
    res.map(SubtenantName.apply)

  private def sublet(query: ApiQuery): ApiQuery =
    user.subtenantId.fold(query) { id =>
      new ApiQuery.Builder(query).addPrefilter("id", PredicateOperator.EQUALS, id.toString).build()
    }

  override def getLogo(id: Long): Option[AttachmentComponent] =
    get(id).flatMap(sub => sub.getLogo)

  override def create(subtenant: Subtenant): ErrorResponse \/ Subtenant =
    val dto = SubtenantDTO.apply(subtenant)
    val sub = subtenantFolder.getOrCreateSubtenantByTenantId(subtenant.getTenantId, dto).createdOr {
      ErrorResponse.validationError(Subtenant.TenantIdProperty, subtenant.getTenantId)("Duplicate tenant id.")
    }
    sub

  override def delete(id: Long): WebResponse =
    get(id).fold[WebResponse](ErrorResponse.notFound) { subtenant =>
      subtenant.delete()
      NoContentResponse
    }

  override def update(id: Long, subtenant: Subtenant): ErrorResponse \/ Subtenant =
    for
      // does the subtenant exist
      existing <- get(id) \/> ErrorResponse.notFound
      // can i lock the folder
      _        <- subtenantFolder.lock(true) \/> ErrorResponse.serverError
      // is the tenant id available
      _        <- tenantIdAvailable(id, subtenant.getTenantId) \/> duplicateTenantIdError(subtenant.getTenantId)
    yield existing <| { _.update(SubtenantDTO.apply(subtenant)) }

  // Is this tenant id available
  private def tenantIdAvailable(id: Long, tenantId: String): Boolean =
    subtenantFolder.findSubtenantByTenantId(tenantId).forall(_.getId.longValue == id)

  private def duplicateTenantIdError(tenantId: String): ErrorResponse =
    ErrorResponse.validationError(Subtenant.TenantIdProperty, tenantId)("Duplicate tenant id.")
end SubtenantRootApiImpl

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

package loi.cp.notification

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQueries, ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.context.ContextId

@Component
class SubscriptionRootApiImpl(val componentInstance: ComponentInstance)(implicit
  fs: FacadeService,
  ss: SubscriptionService,
  currentUser: UserDTO
) extends SubscriptionRootApi
    with ComponentImplementation:
  import SubscriptionRootApi.*

  override def get(q: ApiQuery): ApiQueryResults[Subscription] =
    ApiQueries.query[Subscription](currentUser.facade[NotificationParentFacade].querySubscribes, q)

  override def interest(request: InterestRequest): InterestLevel =
    InterestLevel(ss.interest(currentUser, ContextId(request.context), SubscriptionPath(request.path)))
end SubscriptionRootApiImpl

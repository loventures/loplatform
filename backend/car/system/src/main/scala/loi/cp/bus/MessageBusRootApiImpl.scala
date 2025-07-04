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

package loi.cp.bus

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults, ApiQuerySupport}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.QueryService

@Component
class MessageBusRootApiImpl(val componentInstance: ComponentInstance)(implicit
  fs: FacadeService,
  qs: QueryService,
  domain: DomainDTO,
  cs: ComponentService
) extends MessageBusRootApi
    with ComponentImplementation:
  import MessageBusRootApiImpl.*

  override def getMessageBus(id: Long): Option[MessageBus] = id.tryComponent[MessageBus]

  override def queryMessageBuses(apiQuery: ApiQuery): ApiQueryResults[MessageBus] =
    ApiQuerySupport.query(busFolder.queryBuses, apiQuery, classOf[MessageBus])

  override def pauseMessageBus(id: Long): Unit =
    id.tryComponent[MessageBus]
      .filter(_.getState == MessageBusState.Active)
      .foreach(_.setState(MessageBusState.Paused))

  override def resumeMessageBus(id: Long): Unit =
    id.tryComponent[MessageBus]
      .filter(_.getState != MessageBusState.Active)
      .foreach(_.setState(MessageBusState.Active))

  override def stopMessageBus(id: Long): Unit =
    id.tryComponent[MessageBus]
      .filter(_.getState != MessageBusState.Disabled)
      .foreach(_.setState(MessageBusState.Disabled))
end MessageBusRootApiImpl

object MessageBusRootApiImpl:
  val MessageBusFolderType = "messageBus"

  // Find the message bus folder
  def busFolder(implicit domain: DomainDTO, fs: FacadeService, qs: QueryService): MessageBusParentFacade =
    domain
      .getFolderByType(MessageBusFolderType)
      .facade[MessageBusParentFacade]

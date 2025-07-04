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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.util.Misc.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.QueryService
import com.learningobjects.cpxp.util.EntityContext
import com.learningobjects.cpxp.util.EntityContextOps.*
import loi.cp.integration.SystemComponent
import scalaz.syntax.std.boolean.*

@Service
class MessageBusServiceImpl(ec: => EntityContext)(implicit
  domain: () => DomainDTO,
  fs: FacadeService,
  qs: QueryService,
  mbw: MessageBusWorker
) extends MessageBusService:

  // Publish a message for the target system
  def publishMessage(system: SystemComponent[?], body: AnyRef): Unit =
    val bus = messageBus(system)
    // drop messages for disabled buses
    if bus.getState != MessageBusState.Disabled then
      val active = bus.getState == MessageBusState.Active
      // throw the message into the bus
      val msg    = domain.addFacade[BusMessageFacade] { m =>
        m.setState(active.fold(BusMessageState.Queued, BusMessageState.Ready))
        m.setScheduled(now)
        m.setAttempts(0)
        m.setType(body.getClass.getName)
        m.setBody(body)
        m.setBus(bus)
      }
      if active then
        ec afterCommit {
          mbw.offer(bus.getId, msg.getId)
        }
    end if
  end publishMessage

  // Get or create a message bus
  private def messageBus(system: SystemComponent[?]): MessageBusFacade =
    MessageBusRootApiImpl.busFolder.getOrCreateBusBySystem(system) { b =>
      b.setState(MessageBusState.Active)
      b.setScheduled(now)
      b.setStatistics(BusStatistics.Zero)
    }
end MessageBusServiceImpl

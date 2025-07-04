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

package loi.authoring.index

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import loi.authoring.index.ReindexServiceImpl.ReindexCommand
import loi.cp.bus.*

@Component
@MessageSenderBinding
class IndexerMessageSender(val componentInstance: ComponentInstance, reindexService: ReindexService)
    extends MessageSender[EsSystem, ReindexCommand]
    with ComponentImplementation:

  override def sendMessage(system: EsSystem, command: ReindexCommand, yieldr: YieldTx): DeliveryResult =
    DeliveryResult ofTransientlyFallible { // TODO: distinguish transient failures?
      reindexService.execute(command)      // TODO: yield during the I/O part...
    }

  override def onDropped(message: ReindexCommand, failure: DeliveryFailure): Unit = ()
end IndexerMessageSender

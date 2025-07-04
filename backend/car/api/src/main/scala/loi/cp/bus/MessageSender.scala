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

import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.Instrument
import com.learningobjects.cpxp.component.registry.Bound
import loi.cp.integration.SystemComponent

/** A component responsible for sending messages of particular types from the message bus to a particular system type.
  */
@Bound(classOf[MessageSenderBinding])
@Instrument
trait MessageSender[S <: SystemComponent[?], M] extends ComponentInterface:

  /** Send a message to the specified system.
    * @param system
    *   the external system
    * @param message
    *   the message to send
    * @param yieldr
    *   yield the current database transaction to compute a result. this is specified as part of the signature in order
    *   to make it clear that the caller expects this method to yield the transaction, otherwise it could be surprising
    */
  def sendMessage(system: S, message: M, yieldr: YieldTx): DeliveryResult

  /** Side effects in here *boom boom*
    * @param message
    */
  def onDropped(message: M, failure: DeliveryFailure): Unit
end MessageSender

/** Until we have nice things, yield the current transaction to compute a result. Use this to make external calls
  * without holding onto the database.
  */
trait YieldTx:

  /** Compute a value without a database transaction. */
  def apply[A](a: => A): A

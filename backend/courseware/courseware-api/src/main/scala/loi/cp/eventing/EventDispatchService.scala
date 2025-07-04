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

package loi.cp.eventing

import org.log4s.Logger

import scala.util.control.NonFatal
import scala.util.{Failure, Try}

/** A service to dispatch events for an assessment type.
  *
  * @tparam P
  *   the parameter type
  * @tparam E
  *   the event type
  */
trait EventDispatchService[P, E]:

  /** Sends the given event to all known handlers in the system.
    *
    * @param params
    *   the parameters the actions were performed in.
    * @param event
    *   the event to dispatch
    */
  def dispatchEvent(params: P, event: E): Unit
end EventDispatchService

abstract class AbstractEventDispatchService[P, E] extends EventDispatchService[P, E]:
  val logger: Logger = org.log4s.getLogger(getClass)

  override def dispatchEvent(params: P, event: E): Unit =

    // A Failure on the handler's part does not constitute a failure on the overall transaction, nor should it prevent
    // other handlers from running
    handlers foreach { handler =>
      try
        val result: Try[Unit] = handler.onEvent(params, event)

        result match
          // Intentionally only log the failure and toss it
          case f: Failure[?] => handleFailure(wasCaught = true, handler, event, f)
          case _             =>
      catch case NonFatal(unexpected) => handleFailure(wasCaught = false, handler, event, Failure(unexpected))
    }

  private def handleFailure(wasCaught: Boolean, handler: EventHandler[P, E], event: E, failure: Failure[?]): Unit =
    // We should not kill the transaction, but we have to warn people that something went wrong
    val exceptionMessage: String =
      if wasCaught then s"Exception (${failure.exception})"
      else
        // Bigger warning if the handler didn't catch the issue
        s"UNEXPECTED EXCEPTION (${failure.exception})"

    logger.error(failure.exception)(s"$exceptionMessage handling event $event with $handler")

  protected def handlers: Seq[? <: EventHandler[P, E]]
end AbstractEventDispatchService

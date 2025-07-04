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

package loi.cp.presence

import jakarta.servlet.annotation.WebListener
import jakarta.servlet.http.{HttpSessionEvent, HttpSessionListener}

import com.learningobjects.cpxp.event.SessionStreamEvent
import com.learningobjects.cpxp.scala.actor.CpxpActorSystem

/** An http session listener that broadcasts about session destruction.
  */
@WebListener
final class PresenceHttpSessionListener extends HttpSessionListener:

  /** Do nothing upon session creation. */
  override def sessionCreated(se: HttpSessionEvent): Unit = ()

  /** Broadcast on the event stream about session destruction. */
  override def sessionDestroyed(se: HttpSessionEvent): Unit =
    CpxpActorSystem.system.eventStream publish SessionStreamEvent(
      se.getSession.getId,
      SessionStreamEvent.Message.Destroyed
    )
end PresenceHttpSessionListener

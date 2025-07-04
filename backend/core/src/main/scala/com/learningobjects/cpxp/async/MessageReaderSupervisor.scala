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

package com.learningobjects.cpxp.async

import org.apache.pekko.actor.*
import com.learningobjects.cpxp.scala.actor.CpxpActorSystem

import java.util.logging.{Level, Logger}
import scala.concurrent.duration.Duration

object MessageReaderSupervisor:
  case class Create(props: Props, name: String)
  case class Stop(name: String)

  val logger = Logger.getLogger(classOf[MessageReaderSupervisor].getName)

  lazy val localActor: ActorRef = CpxpActorSystem.system.actorOf(Props(new MessageReaderSupervisor), "messageReaders")

/** Manages the lifecycle of [[MessageReader]] actors.
  *
  * MessageReader actors live only to match [[Queue]] with [[jakarta.servlet.AsyncEvent]] s and their underlying
  * resources. MessageReader actor lifecycle ends immediately should any error in the evaluation of events should
  * happen.
  */
class MessageReaderSupervisor extends Actor:
  import com.learningobjects.cpxp.async.MessageReaderSupervisor.*

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 0, withinTimeRange = Duration.Inf, loggingEnabled = false) { case e: Throwable =>
      logger.log(Level.WARNING, s"Unexpected Message Reader Failure: ${e.getMessage}", e)
      SupervisorStrategy.Stop
    }

  def receive: Actor.Receive = {
    case Create(props, name) => context.actorOf(props, name); ()
    case Stop(name)          => context.child(name).foreach(ref => ref ! PoisonPill)
  }
end MessageReaderSupervisor

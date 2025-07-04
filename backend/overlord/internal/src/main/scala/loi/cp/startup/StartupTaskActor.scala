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

package loi.cp.startup

import org.apache.pekko.actor.{Actor, ActorRef, Props, Terminated}
import com.learningobjects.cpxp.scala.actor.TaggedActors.*
import com.learningobjects.cpxp.scala.actor.{ClusterActors, CpxpActorSystem}
import com.learningobjects.cpxp.startup.{TaskIdentifier, TaskState}

import scala.collection.mutable
import scalaz.{@@, Tag}

/** An actor responsible for broadcasting the current state of the startup task service.
  */
class StartupTaskActor extends Actor:
  import StartupTaskActor.*

  /** Current status. */
  private var status: Option[StartupStatus] = None

  /** Subscribed actors. */
  private val subscribers = mutable.Set.empty[ActorRef]

  /** Log startup information.
    */
  override def preStart(): Unit =
    logger info s"Startup task singleton started"

  /** Get the actor message handler.
    * @return
    *   the actor message handler
    */
  override def receive: Receive = {
    case Hollo             => ()
    case UpdateStatus(s)   => onUpdateStatus(s)
    case Subscribe         => onSubscribe()
    case GetStatus         => onGetStatus()
    case Terminated(actor) => onTerminated(actor)
  }

  /** Handle a startup status update.
    *
    * @param s
    *   the updated status
    */
  private def onUpdateStatus(s: Option[StartupStatus]): Unit =
    logger debug s"Startup task status update: $s"
    subscribers foreach { _ ! UpdateStatus(s) }
    if s.forall(_.startup) then status = s

  /** Handle a status request.
    */
  private def onGetStatus(): Unit =
    logger debug s"Startup task get status: ${sender()}"
    sender() ! UpdateStatus(status)

  /** Handle a subscription request.
    */
  private def onSubscribe(): Unit =
    logger debug s"Startup task subscribe: ${sender()}"
    subscribers.add(sender())
    context watch sender()
    sender() ! UpdateStatus(status)

  /** Handle a subscriber's death.
    *
    * @param actor
    *   the subscriber
    */
  private def onTerminated(actor: ActorRef): Unit =
    logger debug s"Startup task unsubscribe: $actor"
    subscribers.remove(actor)
    ()
end StartupTaskActor

/** Startup task actor companion.
  */
object StartupTaskActor:

  /** The logger. */
  private final val logger = org.log4s.getLogger

  /** A startup task actor reference. */
  type Ref = ActorRef @@ StartupTaskActor

  /** The cluster actor reference. */
  lazy val clusterActor: Ref =
    Tag.of[StartupTaskActor](
      ClusterActors.singleton(Props(new StartupTaskActor), "startupTask")(using CpxpActorSystem.system)
    )

  /** Tell the cluster actor that startup tasks are idle.
    */
  def tellIdle(): Unit = clusterActor ! UpdateStatus(None)

  /** Tell the cluster actor that startup tasks are running.
    *
    * @param status
    *   the startup status
    */
  def tellStatus(status: StartupStatus): Unit =
    clusterActor ! UpdateStatus(Some(status))

  /** A hello.
    */
  case object Hollo

  /** A subscription message. Subscribers will receive {UpdateStatus} messages.
    */
  case object Subscribe

  /** Get status.
    */
  case object GetStatus

  /** A status update message.
    * @param status
    *   the new status
    */
  case class UpdateStatus(status: Option[StartupStatus])

  /** Startup status information.
    *
    * @param domain
    *   the domain being upgraded, or else the system
    * @param task
    *   the startup task
    * @param threadId
    *   the thread identifier
    * @param state
    *   the task state
    * @param startup
    *   whether this is part of system startup and should be persisted
    */
  case class StartupStatus(
    domain: Option[Long],
    task: TaskIdentifier,
    threadId: Long,
    state: Option[TaskState],
    startup: Boolean
  )
end StartupTaskActor

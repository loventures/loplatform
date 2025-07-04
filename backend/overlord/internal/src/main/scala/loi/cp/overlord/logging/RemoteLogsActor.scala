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

package loi.cp.overlord
package logging

import java.util.Date
import java.util.logging.*
import scala.concurrent.duration.*
import org.apache.pekko.actor.{Actor, ActorRef, Props}
import org.apache.pekko.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.scala.actor.CpxpActorSystem
import com.learningobjects.cpxp.service.overlord.ExternalOverlordService
import com.learningobjects.cpxp.util.ManagedUtils
import scaloi.syntax.AnyOps.*

/** An actor which listens on a pub-sub topic for requests for log files. */
class RemoteLogsActor(eos: ExternalOverlordService, sm: ServiceMeta) extends Actor:
  import RemoteLogsActor.*
  import context.dispatcher
  override def preStart(): Unit =
    DistributedPubSub.get(context.system).mediator ! DistributedPubSubMediator.Subscribe(topic, self)

  override def receive: Receive = {
    case GetLogs(time, node) if node.forall(_ == localhost) =>
      doGetLogs(time)
    case ResetLevel(loggerName)                             => onResetLevel(loggerName)
    case level: AlterLevel                                  =>
      Logger.getLogger(level.name).setLevel(level.level)
      level.expiresIn foreach { expires =>
        context.system.scheduler.scheduleOnce(expires.minutes, self, RemoteLogsActor.ResetLevel(level.name))
      }
  }

  private def onResetLevel(loggerName: String): Unit =
    Logger.getLogger(loggerName).setLevel(null)

  private def doGetLogs(time: Date): Unit =
    sender() ! ManagedUtils.perform(() => LogPath(eos.getNodeName, eos.getLogFile(time)))

  private def localhost = sm.getLocalHost
end RemoteLogsActor

object RemoteLogsActor:

  /** The topic on which this actor listens */
  final val topic = "RemoteLogsActor"

  private var _instance: ActorRef = scala.compiletime.uninitialized

  /** The instance for this node */
  def instance: Option[ActorRef]                                          = Option(_instance)
  def initialize(eos: ExternalOverlordService, sm: ServiceMeta): ActorRef =
    CpxpActorSystem.system.actorOf(Props(new RemoteLogsActor(eos, sm))) <| (actor => _instance = actor)

  /** A request to get logs for `time` on the node `node`.
    *
    * @param time
    *   the time to get logs for
    * @param node
    *   if defined, the only node to get logs from
    */
  case class GetLogs(time: Date, node: Option[String] = None)

  case class ResetLevel(loggerName: String)

  /** A response to `GetLogs`.
    *
    * @param node
    *   this node's name
    * @param logs
    *   an s3 log file path
    */
  case class LogPath(node: String, path: String)
end RemoteLogsActor

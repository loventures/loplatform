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

import org.apache.pekko.actor.{Actor, ActorRef, PoisonPill}
import org.apache.pekko.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

import scala.collection.{immutable, mutable}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/** An actor to aggregate logs from other nodes */
class LocalLogsActor(expectedNodes: Int)(implicit ec: ExecutionContext) extends Actor:
  import LocalLogsActor.*

  private var asker: ActorRef = scala.compiletime.uninitialized
  private val responses       = mutable.Map.empty[String, String]

  override def receive: Receive = {
    case AskForLogs(time, delay)             =>
      asker = sender()
      DistributedPubSub.get(context.system).mediator !
        DistributedPubSubMediator.Publish(RemoteLogsActor.topic, RemoteLogsActor.GetLogs(time))
      context.system.scheduler.scheduleOnce(delay, self, Finish)
    case RemoteLogsActor.LogPath(node, path) =>
      responses += (node -> path)
      if responses.size >= expectedNodes then
        /* We have as many logses as we expect... no point in keeping waiting */
        self ! Finish
    case Finish                              =>
      asker ! Logs(responses.toMap)
      self ! PoisonPill
  }
end LocalLogsActor

object LocalLogsActor:

  /** Broadcast a log request to all nodes in the cluster, and respond with `Logs`.
    *
    * @param time
    *   the date for which to get logs
    * @param waitFor
    *   how long to wait for the other app servers
    */
  case class AskForLogs(time: Date, waitFor: FiniteDuration)

  /** A response to `AskForLogs`.
    *
    * @param logs
    *   a map from node name to s3 log file path
    */
  case class Logs(logs: immutable.Map[String, String])

  private case object Finish
end LocalLogsActor

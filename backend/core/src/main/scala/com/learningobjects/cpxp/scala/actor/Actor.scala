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

package com.learningobjects.cpxp.scala.actor

import org.apache.pekko.actor.*
import org.apache.pekko.cluster.Cluster
import org.apache.pekko.cluster.routing.{ClusterRouterGroup, ClusterRouterGroupSettings}
import org.apache.pekko.routing.BroadcastGroup

import java.util.Map as jMap
import java.util.concurrent.TimeoutException
import java.util.logging.Logger
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try

object Actor extends Actor:
  override def logger: Logger = Logger.getLogger(classOf[Actor].getName)

object CpxpActorSystem:
  private var cpxpSystem = Option.empty[ActorSystem]

  def system: ActorSystem = cpxpSystem.orNull

  private val _cbs = mutable.Buffer.empty[ActorSystem => Unit]

  def onActorSystem(f: ActorSystem => Unit): Unit = synchronized {
    cpxpSystem.fold[Unit](_cbs += f)(f)
  }

  def setActorSystem(as: ActorSystem): Unit = synchronized {
    cpxpSystem = Some(as)
    _cbs.foreach(_(as))
    _cbs.clear()
  }

  def clearActorSystem(): Unit = synchronized {
    cpxpSystem = None
  }

  def updateStatus(status: jMap[String, Any], actorSystem: ActorSystem): Unit =
    status.put(s"pekko.Cluster.MemberCount.${actorSystem.name}", Cluster(actorSystem).state.members.size)
end CpxpActorSystem

trait Actor:

  def logger: Logger

  def localSystem: Option[ActorSystem] = Option(CpxpActorSystem.system)

  def createRouterBroadcastGroup(router: ActorRef, system: ActorSystem): ActorRef =
    logger.warning(s"Creating a Broadcast Router for ${router.path}")
    val proxySettings = ClusterRouterGroup(
      BroadcastGroup(Set(router.path.toStringWithoutAddress)),
      ClusterRouterGroupSettings(
        totalInstances = 20,
        routeesPaths = List(router.path.toStringWithoutAddress),
        allowLocalRoutees = true
      )
    ).props()
    logger.warning(s"Broadcaster Config: ${proxySettings.routerConfig}")
    system.actorOf(proxySettings, s"${router.path.name}Group")
  end createRouterBroadcastGroup

  implicit class ActorPathOps(path: ActorPath):
    def clustered(implicit actorSystem: ActorSystem): ActorPath =
      val cluster = Try {
        val cluster = Cluster(actorSystem)
        ActorPath.fromString(path.toStringWithAddress(cluster.selfAddress))
      }
      cluster.getOrElse(path)

  def retry[T](future: => Future[T], nr_retries: Int, timeout: Duration): T =
    try Await.result(future, timeout)
    catch
      case concTimeout: TimeoutException =>
        if nr_retries == 0 then throw concTimeout
        else retry(future, nr_retries - 1, timeout)
end Actor

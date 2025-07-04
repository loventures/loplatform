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

package com.learningobjects.cpxp.async.sysinfo

import org.apache.pekko.actor.*
import org.apache.pekko.cluster.Cluster
import org.apache.pekko.cluster.ClusterEvent.*
import com.learningobjects.cpxp.BaseServiceMeta
import com.learningobjects.cpxp.async.messages.router.*
import com.learningobjects.cpxp.async.{Router, Subscribable}
import com.learningobjects.cpxp.scala.actor.CpxpActorSystem
import com.sun.management.GarbageCollectionNotificationInfo

import java.lang.management.ManagementFactory
import java.util.Date
import javax.management.openmbean.CompositeData
import javax.management.{Notification, NotificationEmitter, NotificationListener}
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

class SystemInformationRouter
    extends Actor
    with Router
    // with ActorLogging
    with Subscribable:
  val activeListeners =
    mutable.Set[(NotificationEmitter, SysInfoMBeanListener)]()
  val domainCluster   = Cluster get context.system

  var lastGC: Option[GarbageCollectionEvent] = None

  val log = org.apache.pekko.event.Logging(context.system, this)

  context.system.scheduler.scheduleWithFixedDelay(1.second, 1.second, self, ComputeMemInfo)
  context.system.eventStream.subscribe(self, classOf[DeadLetter])

  override def preStart(): Unit =
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
    registerJmxListeners()

  override def postStop(): Unit =
    super.postStop()
    cluster.unsubscribe(self)
    unRegisterJmxListeners()

  override def receive: Receive = receiveSubscription orElse {
    case GetOrCreateChannel(channel) =>
      sender() ! QueuePath(self.path)

    case gcMessage: GarbageCollectionEvent =>
      lastGC = Some(gcMessage)
    case member: MemberEvent               =>
      val event = ClusterMemberEvent(member, new Date)
      subscribers foreach { _ ! event }
    case ComputeMemInfo                    =>
      val mem = RuntimeStateEvent.currentState
      subscribers foreach { _ ! mem }
    case dl: DeadLetter                    =>
      subscribers foreach { _ ! DeadLetterMessage(dl, new Date) }
  }

  override def onSubscribe(sub: ActorRef): Unit =
    context watch sub
    subscribers += sub

    val hostMessage = HostEvent(
      hostName = BaseServiceMeta.getServiceMeta.getLocalHost,
      actorSystemAddress = domainCluster.selfAddress,
      timestamp = new Date,
    )
    sub ! hostMessage

    sub ! ClusterStateEvent(cluster.state, new Date)
  end onSubscribe

  private def cluster = Cluster(context.system)

  def unRegisterJmxListeners(): Unit =
    for (emitter, listener) <- activeListeners do emitter.removeNotificationListener(listener)

  def registerJmxListeners(): Unit =
    val emitters  = ManagementFactory.getPlatformMBeanServer.queryMBeans(null, null).asScala.collect {
      case emitter: NotificationEmitter => emitter
    }
    val listeners =
      for emitter <- emitters
      yield
        val newListener = new SysInfoMBeanListener(self)
        emitter.addNotificationListener(newListener, null, null)
        (emitter, newListener)
    activeListeners ++= listeners
    ()
  end registerJmxListeners

  class SysInfoMBeanListener(router: ActorRef) extends NotificationListener:
    def handleNotification(note: Notification, handback: scala.Any) =
      note match
        case notification
            if notification.getType == GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION =>
          val userInfo                                  = notification.getUserData match
            case cd: CompositeData => cd
          val gcInfo: GarbageCollectionNotificationInfo =
            GarbageCollectionNotificationInfo.from(userInfo)
          val gcDate                                    = new Date(notification.getTimeStamp)
          val gcId                                      = notification.getSequenceNumber
          val gcMessage                                 = GarbageCollectionEvent(
            gcId,
            gcDate,
            gcInfo.getGcInfo.getDuration,
            gcInfo.getGcInfo.getStartTime,
            gcInfo.getGcInfo.getEndTime,
            gcInfo.getGcInfo.getMemoryUsageBeforeGc.toString,
            gcInfo.getGcInfo.getMemoryUsageAfterGc.toString,
            gcInfo.getGcAction,
            gcInfo.getGcCause
          )
          router ! gcMessage
        case _ =>
          val jmxInfo =
            JMXEvent(`type` = note.getType, message = note.getMessage, timestamp = new Date(note.getTimeStamp))
          router ! jmxInfo
  end SysInfoMBeanListener
end SystemInformationRouter

object SystemInformationRouter:
  lazy val localInstance: ActorRef =
    CpxpActorSystem.system.actorOf(Props(new SystemInformationRouter), "sysInfo")

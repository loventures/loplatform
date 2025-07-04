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

import org.apache.pekko.actor.{Address, DeadLetter}
import org.apache.pekko.cluster.ClusterEvent.{CurrentClusterState, MemberEvent}
import com.learningobjects.cpxp.async.Event

import java.util.{Calendar, Date}

case object ComputeMemInfo

case class GarbageCollectionEvent(
  id: Long,
  timestamp: Date,
  duration: Long,
  startTime: Long,
  endTime: Long,
  memBefore: String,
  memAfter: String,
  action: String,
  cause: String,
  channel: String = gcChannelId
) extends Event

case class JMXEvent(`type`: String, message: String, channel: String = jmxChannelId, timestamp: Date, id: Long = 0)
    extends Event

case class HostEvent(
  hostName: String,
  actorSystemAddress: Address,
  timestamp: Date,
  channel: String = hostChannelId,
  id: Long = 0L,
) extends Event

case class ClusterStateEvent(
  clusterState: CurrentClusterState,
  timestamp: Date,
  channel: String = clusterChannelId,
  id: Long = 0L,
) extends Event

case class ClusterMemberEvent(
  clusterMemberEvent: MemberEvent,
  timestamp: Date,
  channel: String = clusterChannelId,
  id: Long = 0L,
) extends Event

case class DeadLetterMessage(
  deadLetter: DeadLetter,
  timestamp: Date,
  channel: String = deadLetterChannelId,
  id: Long = 0L,
)

object RuntimeStateEvent:
  def currentState: RuntimeStateEvent =
    val r          = Runtime.getRuntime
    val procs: Int = r.availableProcessors()
    val fm: Long   = r.freeMemory()
    val mm: Long   = r.maxMemory()
    val tm: Long   = r.totalMemory()
    val now        = Calendar.getInstance().getTime
    RuntimeStateEvent(procs, fm, mm, tm, now, timestamp = now)
case class RuntimeStateEvent(
  processors: Int,
  freeMem: Long,
  maxMem: Long,
  totalMem: Long,
  time: Date,
  channel: String = memChannelId,
  timestamp: Date,
  id: Long = 0
) extends Event

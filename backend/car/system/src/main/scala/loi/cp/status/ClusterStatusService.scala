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

package loi.cp.status

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.{PostLoad, PreShutdown, Service}

import java.util.concurrent.{Executors, TimeUnit}

/** Pings the jgroups replication every half minute. */
@Service(unique = true)
class ClusterStatusService(csc: ClusterStatusCache, sm: ServiceMeta):
  private final val scheduler = Executors.newScheduledThreadPool(1)

  @PostLoad
  def pingCache(): Unit =
    ClusterStatusService.logger.info("Scheduling jgroups status check")
    // For the first five minutes, announce every 5 seconds
    val initial = scheduler.scheduleWithFixedDelay(() => csc.invalidate(sm.getLocalHost), 5, 5, TimeUnit.SECONDS)
    // Then announce every thirty seconds
    scheduler.schedule(() => initial.cancel(false), 300, TimeUnit.SECONDS)
    scheduler.scheduleWithFixedDelay(() => csc.invalidate(sm.getLocalHost), 300, 30, TimeUnit.SECONDS)

  @PreShutdown
  def preShutdown(): Unit =
    scheduler.shutdown()
end ClusterStatusService

object ClusterStatusService:
  private final val logger = org.log4s.getLogger

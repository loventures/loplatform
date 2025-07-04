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

package loi.cp.analytics
package bus

import java.lang.Long as JLong
import java.util.Date

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentSupport}
import com.learningobjects.cpxp.scala.util.Misc.*
import com.learningobjects.cpxp.service.query.QueryService
import com.learningobjects.cpxp.util.EntityContext
import com.learningobjects.cpxp.util.EntityContextOps.*
import loi.cp.analytics.redshift.RedshiftEventSender
import loi.cp.analytics.s3.S3EventSender
import loi.cp.bus.BusStatistics

import scala.concurrent.duration.*

// The analytics bus need not use integration systems to manage settings, we should just
// create and configure analytic buses directly. But that would require wholesale lift and
// shift of the integration system editing UI...

/** Analytic bus component implementation.
  */
@Component
class AnalyticBusImpl(val componentInstance: ComponentInstance, self: AnalyticBusFacade)(implicit
  analytics: AnalyticsService,
  ec: EntityContext,
  qs: QueryService
) extends AnalyticBus
    with ComponentImplementation:

  override def getId: JLong = self.getId

  override def getSystem: Option[AnalyticsSystem[? <: AnalyticsSystem[?]]] =
    Option[AnalyticsSystem[? <: AnalyticsSystem[?]]](self.getSystem)

  override def getAnalyticsSender: Option[AnalyticsSender] =
    getSystem
      .map(_.getAnalyticSender)
      .orElse(
        self.getSenderIdentifier match
          case Some(S3EventSender.S3EventSenderIdentifier)   =>
            Some(ComponentSupport.lookupService(classOf[S3EventSender]))
          case Some(RedshiftEventSender.RedshiftEventSender) =>
            Some(ComponentSupport.lookupService(classOf[RedshiftEventSender]))
          case _                                             => None
      )

  override def getName: Option[String] = getSystem.map(_.getName).orElse(self.getSenderIdentifier)

  override def getStatistics: BusStatistics = self.getStatistics

  override def getWindowStart: Date = new Date(self.getWindowTime)

  override def getWindowSize: Int = self.getWindowIds.size

  override def getScheduled: Date = self.getScheduled

  override def getState: AnalyticBusState = self.getState

  override def getFailureCount: Long = self.getFailureCount

  override def getQueueSize: Long = AnalyticsPoller.eventCount(self)

  override def setState(state: AnalyticBusState): Unit = self.setState(state)

  override def setFailureCount(count: Long): Unit = self.setFailureCount(count)

  override def getLastMaterializedViewRefreshDate: Option[Date] = self.getLastMaterializedViewRefreshDate

  override def lock(pessimistic: Boolean): Unit = self.lock(pessimistic)

  override def refresh(timeout: Long): Boolean = self.refresh(timeout)

  override def pump(): Unit =
    // If I can't lock the bus then it's probably being processed
    // by the poller.
    if self.refresh(5.seconds.toMillis) then
      self.setScheduled(now)
      ec afterCommit {
        analytics.pumpPoller()
      }
end AnalyticBusImpl

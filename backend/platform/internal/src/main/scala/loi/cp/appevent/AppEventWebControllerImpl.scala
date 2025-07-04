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

package loi.cp.appevent

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQuerySupport, ApiQueryUtils}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.appevent.AppEventFinder
import com.learningobjects.cpxp.service.query.{Comparison, Direction, QueryBuilder, QueryService}
import loi.cp.appevent.facade.AppEventFacade
import loi.cp.appevent.impl.AppEventExecution
import scaloi.misc.TimeSource

@Component
class AppEventWebControllerImpl(val componentInstance: ComponentInstance)(implicit
  queryService: QueryService,
  appEventService: AppEventService,
  ts: TimeSource
) extends AppEventWebController
    with ComponentImplementation:
  import AppEventWebControllerImpl.*
  import AppEventWebController.*

  override def getAppEvents(query: ApiQuery): Seq[AppEventResponse] =
    ApiQuerySupport
      .getQueryBuilder(appEventQueryBuilder.setLogQuery(true), ApiQueryUtils.propertyMap[AppEventResponse](query))
      .getFacades[AppEventFacade]
      .map(toAppEventResponse) // malfeasant response collection

  override def warpTime(): Int =
    val events = appEventQueryBuilder
      .addCondition(AppEventFinder.DATA_TYPE_APP_EVENT_DEADLINE, Comparison.gt, Instant.now)
      .addCondition(AppEventFinder.DATA_TYPE_APP_EVENT_DEADLINE, Comparison.le, ts.date)
      .addCondition(AppEventFinder.DATA_TYPE_APP_EVENT_STATE, Comparison.eq, null)
      .setOrder(AppEventFinder.DATA_TYPE_APP_EVENT_DEADLINE, Direction.ASC)
      .getFacades[AppEventFacade]
    events foreach { event =>
      // TODO: Robust against failure for lingering events of old things?
      logger info s"Executing app-event: ${event.getId}:${event.getEventId} in warp time"
      new AppEventExecution(event.getId).executeAppEvent()
    }
    events.size
  end warpTime

  override def getInProgressAppEvents(instant: Instant): Seq[AppEventResponse] =
    appEventQueryBuilder
      .addCondition(AppEventFinder.DATA_TYPE_APP_EVENT_STARTED, Comparison.lt, instant.plus(1, ChronoUnit.MINUTES))
      .addCondition(AppEventFinder.DATA_TYPE_APP_EVENT_FINISHED, Comparison.gt, instant.minus(1, ChronoUnit.MINUTES))
      .getFacades[AppEventFacade]
      .map(toAppEventResponse)

  override def getQueuedAppEvents(instant: Instant): Seq[AppEventResponse] =
    appEventQueryBuilder
      .addCondition(AppEventFinder.DATA_TYPE_APP_EVENT_CREATED, Comparison.lt, instant.plus(1, ChronoUnit.MINUTES))
      .addCondition(AppEventFinder.DATA_TYPE_APP_EVENT_STARTED, Comparison.gt, instant.minus(1, ChronoUnit.MINUTES))
      .getFacades[AppEventFacade]
      .map(toAppEventResponse)

  override def getAppEventStats(startTime: Date, endTime: Date): AppEventStatistics =
    val appEvents: Seq[AppEventResponse] = appEventQueryBuilder
      .addCondition(AppEventFinder.DATA_TYPE_APP_EVENT_CREATED, Comparison.gt, startTime)
      .addCondition(AppEventFinder.DATA_TYPE_APP_EVENT_CREATED, Comparison.lt, endTime)
      .getFacades[AppEventFacade]
      .map(toAppEventResponse)

    val appEventById: Map[EventId, AppEventResponse] = appEvents.map(e => e.id -> e).toMap

    val eventsByStartStatus: Map[Boolean, Seq[AppEventResponse]] = appEvents.groupBy(_.processingStartTime.isDefined)
    val queuedAppEvents: Seq[AppEventResponse]                   = eventsByStartStatus.getOrElse(false, Nil)
    val inProgressAppEvents: Seq[AppEventResponse]               = eventsByStartStatus.getOrElse(true, Nil)
    val finishedAppEvents: Seq[AppEventResponse]                 = appEvents
      .filter(e => e.processingStartTime.isDefined && e.processingEndTime.isDefined)
      .filter(e => e.processingEndTime.get.isBefore(endTime.toInstant))

    val processingTimesById: Map[EventId, ElapsedTime] = finishedAppEvents.map(e => e.id -> e.processingTime.get).toMap
    val avgProcessingTime: Double                      = processingTimesById.values
      .fold(0L)(_ + _)
      .doubleValue() / finishedAppEvents.size.doubleValue()

    val processingTimeStdDev: Double = standardDeviation(processingTimesById.values.toSeq, avgProcessingTime)

    val outlierProcessingTimeEvents: Seq[AppEventResponse] = processingTimesById
      .filter(_._2 > processingTimeStdDev * 3.5)
      .map({ case (id, _) =>
        appEventById(id)
      })
      .toSeq

    val queueTimesById: Map[EventId, ElapsedTime] = appEvents
      .filter(_.processingStartTime.isDefined)
      .map(e => e.id -> e.queueTime.get)
      .toMap

    val avgQueueTime: Double = queueTimesById.values
      .fold(0L)(_ + _)
      .doubleValue() / inProgressAppEvents.size.doubleValue()

    val queryTimeStdDev: Double = standardDeviation(queueTimesById.values.toSeq, avgQueueTime)

    val outlierQueuedAppEvents: Seq[AppEventResponse] = queueTimesById
      .filter(_._2 > (queryTimeStdDev * 3.5))
      .map({ case (id, _) =>
        appEventById(id)
      })
      .toSeq

    AppEventStatistics(
      inProgressAppEvents.size - finishedAppEvents.size,
      finishedAppEvents.size,
      queuedAppEvents.size,
      avgProcessingTime,
      avgQueueTime,
      appEvents.map(_.eventId).distinct,
      queuedAppEvents,
      outlierQueuedAppEvents,
      outlierProcessingTimeEvents
    )
  end getAppEventStats

  override def getStats: AppEventService.QueueStats = appEventService.getQueueStats

  private def standardDeviation(elapsedTime: Seq[ElapsedTime], mean: Double): Double =
    Math.sqrt(
      elapsedTime
        .map(e => Math.pow(e - mean, 2))
        .fold(0.0)(_ + _) / elapsedTime.size
    )

  private def appEventQueryBuilder: QueryBuilder =
    queryService.queryRoot(AppEventFinder.ITEM_TYPE_APP_EVENT)

  private def toAppEventResponse(event: AppEventFacade): AppEventResponse =
    AppEventResponse(
      event.getId,
      event.getParentId,
      event.getEventId,
      Option(event.getState).map(_.name),
      event.getCreated.toInstant,
      event.getDeadline.toInstant,
      Option(event.getProcessingStart).map(_.toInstant),
      Option(event.getProcessingEnd).map(_.toInstant)
    )
end AppEventWebControllerImpl

object AppEventWebControllerImpl:
  final val logger = org.log4s.getLogger

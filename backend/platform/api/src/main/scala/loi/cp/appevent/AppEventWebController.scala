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
import java.util.Date

import com.learningobjects.cpxp.component.annotation.{Controller, QueryParam, RequestMapping}
import com.learningobjects.cpxp.component.annotation.Controller.Category
import com.learningobjects.cpxp.component.query.ApiQuery
import com.learningobjects.cpxp.component.web.{ApiRootComponent, Method}
import com.learningobjects.cpxp.service.appevent.AppEventConstants
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.de.authorization.Secured
import com.learningobjects.de.web.Queryable
import loi.cp.admin.right.AdminRight

import scala.annotation.meta.getter

@Controller(root = true, value = "appEvents", category = Category.UNCATEGORIZED)
trait AppEventWebController extends ApiRootComponent:
  import AppEventWebController.*

  /** Query across all appevents.
    *
    * @param query
    *   The app event query.
    * @return
    *   The matching app events.
    */
  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "appEvents", method = Method.GET)
  def getAppEvents(query: ApiQuery): Seq[AppEventResponse]

  /** Run app events respecting a fanciful time sent by the client.
    * @return
    *   number of events
    */
  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "warpTime", method = Method.POST)
  def warpTime(): Int

  /** Get data back for app events that were in progress within a minute of the given time.
    *
    * @param timestamp
    *   The time that app event data will be retrieved for.
    * @return
    *   All app events that were in progress at the given time.
    */
  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "appEvents/inProgress", method = Method.GET)
  def getInProgressAppEvents(@QueryParam("at") timestamp: Instant): Seq[AppEventResponse]

  /** Get all app events that were queued but not in progress within a minute of the given time.
    *
    * @param timestamp
    *   The time that app event data will be retrieved for.
    * @return
    *   All app events that were queued to be processed at the given time.
    */
  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "appEvents/queued", method = Method.GET)
  def getQueuedAppEvents(@QueryParam("at") timestamp: Instant): Seq[AppEventResponse]

  /** Get basic statistics and outlying app events over the given time period.
    *
    * @param startTime
    * @param endTime
    * @return
    */
  @Secured(Array(classOf[AdminRight]))
  @RequestMapping(path = "appEvents/statistics", method = Method.GET)
  def getAppEventStats(@QueryParam("start") startTime: Date, @QueryParam("end") endTime: Date): AppEventStatistics

  /** @return
    *   statistics on current state of the AppEvents queue and how behind it is.
    */
  @RequestMapping(path = "appEvents/status", method = Method.GET)
  @Secured(allowAnonymous = true) def getStats: AppEventService.QueueStats
end AppEventWebController

object AppEventWebController:
  type EventId     = Long
  type ElapsedTime = Long

  type Q = Queryable @getter

  case class AppEventResponse(
    @Q(dataType = DataTypes.META_DATA_TYPE_ID) id: EventId,
    @Q(dataType = DataTypes.META_DATA_TYPE_PARENT_ID) parent_id: Long,
    @Q(dataType = AppEventConstants.DATA_TYPE_APP_EVENT_EVENT_ID) eventId: String,
    @Q(dataType = AppEventConstants.DATA_TYPE_APP_EVENT_STATE) state: Option[String],
    @Q(dataType = AppEventConstants.DATA_TYPE_APP_EVENT_CREATED) createTime: Instant,
    @Q(dataType = AppEventConstants.DATA_TYPE_APP_EVENT_DEADLINE) deadline: Instant,
    processingStartTime: Option[Instant],
    processingEndTime: Option[Instant]
  ):
    val queueTime: Option[ElapsedTime] =
      for start <- processingStartTime
      yield start.toEpochMilli - createTime.toEpochMilli

    val processingTime: Option[ElapsedTime] = for
      start <- processingStartTime
      end   <- processingEndTime
    yield end.toEpochMilli - start.toEpochMilli
  end AppEventResponse

  case class AppEventStatistics(
    inProgress: Int,
    finished: Int,
    queued: Int,
    avgProcessingTime: Double,
    avgTimeInQueue: Double,
    eventTypes: Seq[String],
    queuedEvents: Seq[AppEventResponse],
    outlierQueuedAppEvents: Seq[AppEventResponse],
    outlierProcessingEvents: Seq[AppEventResponse]
  )
end AppEventWebController

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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.operation.{DispatcherOperation, Operations}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.QueryOps.*
import com.learningobjects.cpxp.scala.util.Stopwatch.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.component.misc.AnalyticConstants
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.domain.{DomainConstants, DomainState, DomainWebService}
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.overlord.OverlordWebService
import com.learningobjects.cpxp.service.query.*
import com.learningobjects.cpxp.util.*
import loi.cp.analytics.bus.{AnalyticBus, AnalyticBusFacade, AnalyticBusState, AnalyticsBusSettings}
import loi.cp.analytics.event.Event
import loi.cp.bus.{Bus, BusFailureNotificationService, IntervalStatistics}
import loi.cp.config.ConfigurationKey.*
import loi.cp.config.ConfigurationService
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scalaz.{Tags, \/}
import scaloi.misc.Handlers.*
import scaloi.syntax.`try`.*
import scaloi.syntax.any.*
import scaloi.syntax.boolean.*
import scaloi.syntax.date.*
import scaloi.syntax.disjunction.*

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import java.util.{Date, UUID}
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.math.Ordering.Implicits.infixOrderingOps
import scala.util.Try

/** Polls the various analytic buses for events and delivers them. This operates with a single dedicated executor
  * thread.
  */
class AnalyticsPoller(executor: ScheduledExecutorService)(implicit
  configurationService: ConfigurationService,
  dws: DomainWebService,
  emailService: EmailService,
  is: ItemService,
  mapper: ObjectMapper,
  ows: OverlordWebService,
  queryService: QueryService,
  sm: ServiceMeta,
  fns: BusFailureNotificationService,
  cs: ComponentService
) extends Runnable:
  import AnalyticsPoller.*

  /** The monitor. We wait on this object to sleep between pollings. */
  private object monitor

  /** Start this analytics poller.
    */
  private def start(): Unit =
    logger.info("Starting analytics poller")
    // start the polling loop with automatic restart on error
    executor.scheduleWithFixedDelay(this, 0, FailureRetryDelay.toSeconds, TimeUnit.SECONDS)

  /** Shut down this analytics poller.
    */
  def shutdown(): Unit =
    logger.info("Stopping analytics poller")
    executor.shutdownNow()
    val terminated =
      executor.awaitTermination(ShutdownWaitTime.toSeconds, TimeUnit.SECONDS)
    logger.info(s"Stopped analytics poller: $terminated")

  /** Process analytics messages.
    */
  override def run(): Unit =
    treither {
      logger.debug("Analytics poller started")
      while !executor.isShutdown do
        logger.debug("Polling buses")
        val delta = if sm.isDas then
          // poll the buses, return the time the next bus is scheduled
          val next = ManagedUtils.perform(
            new DispatcherOperation[Date](
              () => processAllBuses(),
              "AnalyticsPoller",
              classOf[AnalyticsPoller].getName + "/processAllBuses"
            )
          )
          // sleep until the next bus is scheduled
          next - now
        else 30.seconds
        if delta.length > 0 then
          logger.debug(s"Waiting $delta")
          try
            monitor.synchronized {
              monitor `wait` delta.toMillis
            }
          catch ignoring[InterruptedException]
      end while
      logger.debug("Analytics poller shut down")
    } -<| { e =>
      logger.warn(e)("Error polling for analytics events")
    }

  /** Force the analytics poller to check for new events.
    */
  private[analytics] def pump(): Unit = monitor.synchronized {
    monitor.notifyAll()
  }

  /** Poll all buses for events to be processed.
    * @return
    *   when the buses should next be polled; the earliest scheduled date
    */
  private def processAllBuses(): Date =
    ows.asOverlord(() =>
      val pi = AnalyticsBusSettings.Key.getDomain.pollRate.seconds
      for bus <- availableBuses do processBus(bus, pi)
      Option(nextBusScheduled).getOrElse(now + pi)
    )

  /** Attempt to lock a bus and then process its events.
    * @param bus
    *   the bus to process
    * @param pollInterval
    *   the poll interval
    */
  private def processBus(bus: AnalyticBusFacade, pollInterval: Duration): Unit =
    treither {
      logger.debug(s"Polling bus ${bus.getId}")
      // pessimistically lock the bus; if successful, and the bus is still scheduled, process it
      for
        _ <- acquireBus(bus)                        // attempt to acquire the bus
        _ <- processScheduledBus(bus, pollInterval) // attempt to deliver messages
      do {
        // TODO: figure out how we configure when to delete delivered and what retention period
        // if events were delivered then commit and delete
        // deleteAnalytics(bus.getRootId)
      }
      ManagedUtils.commit()
    } -<| { e =>
      logger.warn(e)(s"Bus processing error: ${bus.getId}")
      ManagedUtils.rollback()
      val failures = bus.getFailureCount
      if failures < MaxFailures then
        bus.setFailureCount(1 + failures)
        bus.setScheduled(now + pollInterval * (1L << failures).toDouble)
      else
        logger warn s"Auto-pausing bus ${bus.getId} after fatal error"
        bus.setState(AnalyticBusState.Paused)
        fns.emailFailure(toBus(bus), repeated = false, Left(e))
    }
    EntityContext.flushClearAndCommit() // always commit
    Current.clear() // !!!!! hulk smashes cornucopia of thread-local stuff but ~maybe~ NOT what you'd expect... BEWARE
  end processBus

  /** Attempts to lock a bus and validate that it is ready for processing.
    * @param bus
    *   the analytics bus
    * @return
    *   the bus if it is ready for processing
    */
  private def acquireBus(bus: AnalyticBusFacade): Option[AnalyticBusFacade] =
    (bus.refresh(LockTimeout.toMillis) && !now.before(bus.getScheduled))
      .option(bus)

  /** Process a bus that is locked and ready for delivery.
    * @param bus
    *   the bus to process
    * @return
    *   delivered events, if successful
    */
  private def processScheduledBus(bus: AnalyticBusFacade, pollInterval: Duration): Option[Long] =
    // grab a chunk of analytic events
    val analytics         = getAnalytics(bus)
    // and their ids
    val analyticIds       = analytics.map(_.id.longValue)
    // attempt to deliver any events that were retrieved
    val (result, elapsed) = profiled {
      // success if no events were found or bus disabled or delivery actually succeeded
      if analyticIds.isEmpty || (bus.getState == AnalyticBusState.Disabled) then DeliveryResult.success()
      else deliverEvents(bus, analytics)
    }

    // update the bus state at the end
    val level = if analyticIds.isEmpty then org.log4s.Debug else org.log4s.Info
    logger(level)(
      s"Processed ${analyticIds.size} analytics on bus ${bus.getId} in $elapsed, success: ${result.isSuccess}"
    )

    result match
      case DeliverySuccess(didRefreshMaterializedViews) =>
        // if successful then compute a new lookback window
        val (windowTime, windowIds) = getWindow(bus, analytics)
        // compute interval statistics
        /*_*/
        val statistics              = IntervalStatistics.monoid.zero.copy(
          delivered = analyticIds.size,
          millis = elapsed.toMillis,
          queued = Tags.MaxVal(eventCount(bus, windowTime, windowIds.size)),
          executions = if analyticIds.nonEmpty then 1 else 0,
        )
        /*_*/
        bus.setScheduled(now + pollInterval)
        bus.setWindowTime(windowTime.getTime)
        bus.setWindowIds(windowIds)
        bus.setStatistics(bus.getStatistics + statistics)
        bus.setFailureCount(0)
        didRefreshMaterializedViews <|? bus.setLastMaterializedViewRefreshDate(Some(new Date()))
        Some(analyticIds.size.toLong) // delivery success

      case TransientFailure(failure) =>
        /*_*/
        val statistics = IntervalStatistics.monoid.zero.copy(
          failed = 1, // this statistic is really a bit bogus with retry vs skip behaviour
          millis = elapsed.toMillis,
          queued = Tags.MaxVal(eventCount(bus, new Date(bus.getWindowTime), bus.getWindowIds.size))
        )
        /*_*/
        val failures   = bus.getFailureCount
        bus.setFailureCount(1 + failures)
        bus.setStatistics(bus.getStatistics + statistics)
        if failures < MaxFailures then bus.setScheduled(now + pollInterval * (1L << failures).toDouble)
        else
          logger warn s"Auto-pausing bus ${bus.getId} after repeated failures"
          bus.setState(AnalyticBusState.Paused)
          fns.emailFailure(toBus(bus), repeated = true, Left(failure))
        None // no delivery

      case PermanentFailure(th) =>
        throw th // caller handles this
    end match
  end processScheduledBus

  /** Get the next chunk of analytics to process for an analytic bus. Analytics are considered in date order subsequent
    * to the lookback window. An analytic is considered processed if it occurred before the lookback window or is in the
    * set of lookback Ids. We use this lookback mechanism allow for delayed insertion of events (e.g. a transaction
    * taken a minute to commit).
    * @note
    *   while this returns `Analytics`, they are backed by a case class and completely detached from the hibernate
    *   session, and thus safe for use without a transaction
    * @param bus
    *   the analytic bus
    * @return
    *   a chunk of analytic Ids and the corresponding JSON encoded events
    */
  private def getAnalytics(bus: AnalyticBusFacade): Seq[AnalyticDto] =
    val window    = new Date(bus.getWindowTime)
    val windowIds = bus.getWindowIds
    logger.debug(s"Polling bus ${bus.getId}, ${bus.getScheduled}, $window, ${windowIds.size}")
    val analytics = queryService
      .queryRoot(bus.getRootId, AnalyticConstants.ITEM_TYPE_ANALYTIC)
      .addCondition(AnalyticConstants.DATA_TYPE_ANALYTIC_TIME, Comparison.ge, window)
      .addCondition(AnalyticConstants.DATA_TYPE_ANALYTIC_TIME, Comparison.lt, now)
      .addCondition(
        BaseCondition.ofIterable(DataTypes.META_DATA_TYPE_ID, Comparison.unequalAll, windowIds)
      ) // unequalAll generates less inefficient hibernate sql
      .setOrder(AnalyticConstants.DATA_TYPE_ANALYTIC_TIME, Direction.ASC)
      .setOrder(DataTypes.META_DATA_TYPE_ID, Direction.ASC)
      .setLimit(bus.getConfiguration.maxEvents)
      .setDataProjection(
        BaseDataProjection.ofData(
          DataTypes.META_DATA_TYPE_ID,
          AnalyticConstants.DATA_TYPE_ANALYTIC_GUID,
          AnalyticConstants.DATA_TYPE_ANALYTIC_TIME,
          AnalyticConstants.DATA_TYPE_ANALYTIC_DATA_JSON
        )
      )
      .getValues[Array[AnyRef]]
    logger.debug(s"Found ${analytics.size} events")

    def toDto(id: Number, guid: String, time: Date, ev: => Event): Try[AnalyticDto] =
      Try(ev) map { data =>
        AnalyticDto(
          id = id.longValue,
          guid = UUID `fromString` guid,
          date = time,
          eventData = data
        )
      }

    // so the bus doesn't pause before we implement proper event versioning
    analytics.flatMap {
      // This is what Hibernate should and used to return based on the column definition
      case Array(id: Number, guid: String, time: Date, ev: ObjectNode) =>
        toDto(id, guid, time, mapper.treeToValue(ev, classOf[Event]))
          .tapFailure(e => logger.warn(e)(s"DROPPED EVENT event[$guid], data:${mapper.writeValueAsString(ev)}"))
          .toOption
      // This is what Hibernate 5.4 and its grand fsckulence has started returning instead
      case Array(id: Number, guid: String, time: Date, ev: String)     =>
        toDto(id, guid, time, mapper.readValue(ev, classOf[Event]))
          .tapFailure(e => logger.warn(e)(s"DROPPED EVENT event[$guid], data:$ev"))
          .toOption
    }
  end getAnalytics

  /** Get a new lookback window. This window may be less than the default lookback window size if events occur at such a
    * rate that the lookback Id list would be too large.
    * @param bus
    *   the analytic bus
    * @param analyticIds
    *   the analytics that were just processed
    * @return
    *   the new lookback window and Ids
    */
  private def getWindow(bus: AnalyticBusFacade, analytics: Seq[AnalyticDto]): (Date, Seq[Long]) =
    val currentTime    = now
    // compute an initial lookback window. if we are processing a backlog of old events then we
    // use the timestamp of the most recent event, otherwise 5 minutes ago.
    val lastTimestamp  = analytics.lastOption.cata(_.date, currentTime)
    val fiveMinutesAgo = currentTime - LookbackWindow
    val newWindow      = fiveMinutesAgo min lastTimestamp
    if lastTimestamp < fiveMinutesAgo then
      // there is no chance that a node will add events for five minutes ago so we
      // just look for events >= the last event date and not equal to one of these events
      (lastTimestamp, analytics.dropWhile(_.date < lastTimestamp).map(_.id))
    else
      // next, pull which of our known pks fall within this new candidate window
      val newEvents   = queryService
        .queryRoot(bus.getRootId, AnalyticConstants.ITEM_TYPE_ANALYTIC)
        .addCondition(
          BaseCondition
            .ofIterable(DataTypes.META_DATA_TYPE_ID, Comparison.equalsAny, bus.getWindowIds ++ analytics.map(_.id))
        )
        .addCondition(AnalyticConstants.DATA_TYPE_ANALYTIC_TIME, Comparison.ge, newWindow)
        .addCondition(AnalyticConstants.DATA_TYPE_ANALYTIC_TIME, Comparison.lt, currentTime)
        .setOrder(AnalyticConstants.DATA_TYPE_ANALYTIC_TIME, Direction.DESC) // i want the most recent chunk
        .setLimit(LookbackIDCount)
        .setDataProjection(
          BaseDataProjection.ofData(
            DataTypes.META_DATA_TYPE_ID,
            AnalyticConstants.DATA_TYPE_ANALYTIC_TIME,
          )
        )
        .getValues[Array[AnyRef]]
        .collect { case Array(id: Number, time: Date) =>
          id.longValue -> time
        }
      // finally, if our PK list was truncated, foreshorten the lookback window to just match the
      // window of the returned Ids
      val finalWindow =
        if newEvents.size == LookbackIDCount then new Date(1L + newEvents.last._2.getTime)
        else newWindow
      (finalWindow, newEvents.map(_._1))
    end if
  end getWindow

  /** Delete any delivered analytics for a domain. This will delete any that are older than the furthest lookback
    * window. Note that if there are no analytics buses or any bus is suspended then analytics will not be deleted.
    * @param domain
    *   the domain
    */
  private def deleteAnalytics(domain: Long): Throwable \/ Unit =
    treither {
      // find the furthest lookback window of all buses, suspended or active
      val minWindow = minimumWindow(domain)
      logger.debug(s"Deleting delivered events: ${new Date(minWindow)}")
      val deleted   =
        jpaql"DELETE FROM AnalyticFinder WHERE root.id = $domain AND timestamp < $minWindow"
          .executeUpdate()
      logger.debug(s"Deleted $deleted events")
    } -<| { e =>
      logger.warn(e)("Error deleting events")
    }

  /** Deliver events to a bus.
    * @param bus
    *   the analytic bus
    * @param analytics
    *   the analytic events
    * @return
    *   success or failure
    */
  private def deliverEvents(bus: AnalyticBusFacade, analytics: Seq[Analytic]): DeliveryResult =
    logger.info(s"Processing ${analytics.size} events on ${bus.getId}")
    Operations.asDomain(bus.getRootId, eventDeliverer(bus, analytics)) <| {
      case DeliverySuccess(_)  =>
        logger.info("Delivered events")
      case PermanentFailure(e) =>
        logger.warn(e)("Permanent failure sending analytic events")
      case TransientFailure(e) =>
        logger.warn(e)("Transient failure sending analytic events")
    }
  end deliverEvents

  /** An event delivery operation.
    * @param bus
    *   the analytic bus
    * @param events
    *   the JSON analytic events
    * @return
    *   success or a failure
    */
  private def eventDeliverer(bus: AnalyticBusFacade, events: Seq[Analytic]): Operation[DeliveryResult] =
    () =>
      bus.component[AnalyticBus].getAnalyticsSender match
        case None         =>
          logger.warn(
            s"bus with id: ${bus.getId} does not have a valid sender implementation or connector, skipping processing with this bus"
          )
          DeliveryResult.success()
        case Some(sender) =>
          InstrumentedProxy(sender).sendAnalytics(events, bus.getConfiguration, bus.getLastMaterializedViewRefreshDate)

  private def toBus(abf: AnalyticBusFacade): Bus = new Bus:
    override def getBusName: String =
      Option[AnalyticsSystem[? <: AnalyticsSystem[?]]](abf.getSystem)
        .map(_.getName)
        .orElse(abf.getSenderIdentifier)
        .getOrElse("<deleted>")

    override def getId: Long = abf.getId

    override def getRootId: Long = abf.getRootId
end AnalyticsPoller

/** Analytics poller singleton.
  */
object AnalyticsPoller:

  /** Start a new analytics poller.
    * @return
    *   the analytics poller
    */
  def start(implicit
    domainWebService: DomainWebService,
    queryService: QueryService,
    facadeService: FacadeService,
    emailService: EmailService,
    is: ItemService,
    sm: ServiceMeta,
    mapper: ObjectMapper,
    configurationService: ConfigurationService,
    overlordWebService: OverlordWebService,
    fns: BusFailureNotificationService,
    cs: ComponentService
  ): AnalyticsPoller =
    new AnalyticsPoller(Executors.newSingleThreadScheduledExecutor(threadFactory)) <| {
      _.start()
    }

  /** A thread factory with a nice name. */
  private def threadFactory =
    new ThreadFactoryBuilder().setNameFormat("AnalyticsPoller-%d").build()

  private def normalDomains(implicit qs: QueryService): QueryBuilder = qs
    .queryAllDomains(DomainConstants.ITEM_TYPE_DOMAIN)
    .addCondition(DomainConstants.DATA_TYPE_DOMAIN_STATE, Comparison.eq, DomainState.Normal)

  /** Return the buses that are active and scheduled for processing. */
  private def availableBuses(implicit ows: OverlordWebService, qs: QueryService): Seq[AnalyticBusFacade] =
    queryBuses
      .addJoinQuery(DataTypes.META_DATA_TYPE_ROOT_ID, normalDomains)
      .addCondition(AnalyticConstants.DATA_TYPE_ANALYTIC_BUS_SCHEDULED, Comparison.le, now)
      .addCondition(AnalyticConstants.DATA_TYPE_ANALYTIC_BUS_STATE, Comparison.eq, AnalyticBusState.Active)
      .getFacadeList(classOf[AnalyticBusFacade])
      .asScala
      .toSeq

  /** Get the next active bus scheduled date. */
  private def nextBusScheduled(implicit ows: OverlordWebService, qs: QueryService): Date =
    queryBuses
      .addJoinQuery(DataTypes.META_DATA_TYPE_ROOT_ID, normalDomains)
      .addCondition(AnalyticConstants.DATA_TYPE_ANALYTIC_BUS_STATE, Comparison.eq, AnalyticBusState.Active)
      .setDataProjection(
        BaseDataProjection.ofAggregateData(AnalyticConstants.DATA_TYPE_ANALYTIC_BUS_SCHEDULED, Function.MIN)
      )
      .getResult[Date]

  /** Get the furthest back lookback window of any bus on this domain, whether active or suspended. */
  private def minimumWindow(domain: Long)(implicit qs: QueryService): Long =
    qs.queryRoot(domain, AnalyticConstants.ITEM_TYPE_ANALYTIC_BUS)
      .setDataProjection(
        BaseDataProjection.ofAggregateData(AnalyticConstants.DATA_TYPE_ANALYTIC_BUS_WINDOW_TIME, Function.MIN)
      )
      .getResult[Long]

  /** Query all analytics buses. */
  private def queryBuses(implicit qs: QueryService): QueryBuilder =
    qs.queryAllDomains(AnalyticConstants.ITEM_TYPE_ANALYTIC_BUS)

  /** Count the remaining events for a given bus to a limit of [MaxCount+1]. */
  def eventCount(bus: AnalyticBusFacade)(implicit qs: QueryService): Long =
    eventCount(bus, new Date(bus.getWindowTime), bus.getWindowIds.size)

  /** Count the remaining events for a given bus with a specified lookback window subject to a limit of [MaxCount+1]. */
  def eventCount(bus: AnalyticBusFacade, window: Date, idCount: Int)(implicit qs: QueryService): Long =
    sql"""SELECT COUNT(*) FROM
          (SELECT time FROM AnalyticFinder
            WHERE root_id = ${bus.getRootId}
            AND time >= $window AND time < $now
            LIMIT ${MaxCount + idCount + 1}) AS time""".getSingleResult.asInstanceOf[Number].longValue - idCount

  /** Now. */
  private def now: Date = new Date()

  /** The logger. */
  private val logger = org.log4s.getLogger

  /** Delay before retry on polling failure. */
  private val FailureRetryDelay = 15.seconds

  // (MaxEvents + LookbackIDCount) must be < max # parameters to a SQL query (~30K)

  /** Shutdown wait time. */
  private val ShutdownWaitTime = 20.seconds

  /** Bus lock timeout. */
  private val LockTimeout = 1.second

  /** Maximum lookback window for late-inserted events. */
  private val LookbackWindow = 5.minutes

  /** Max Ids to cache in our lookback window. */
  private val LookbackIDCount = 500

  /** Max failures before bus auto-pauses. */
  private val MaxFailures = 9

  /** Maximum number of events to count. */
  private val MaxCount = 10000
end AnalyticsPoller

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

package loi.cp.bus

import argonaut.Json
import com.fasterxml.jackson.databind.node.ObjectNode
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.ComponentSupport
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.util.ComponentUtils
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.util.Stopwatch
import com.learningobjects.cpxp.service.component.misc.MessageBusConstants.*
import com.learningobjects.cpxp.service.domain.DomainWebService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query
import com.learningobjects.cpxp.service.query.*
import com.learningobjects.cpxp.util.ManagedUtils
import de.tomcat.juli.LogMeta
import loi.cp.integration.SystemComponent
import loi.cp.worker.AbstractWorker
import org.apache.commons.lang3.exception.ExceptionUtils
import scalaz.std.list.*
import scalaz.syntax.semigroup.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.tuple.*
import scalaz.syntax.traverse.*
import scaloi.misc.TimeSource
import scaloi.syntax.BooleanOps.*
import scaloi.syntax.CollectionBoxOps.*
import scaloi.syntax.CollectionOps.*
import scaloi.syntax.DateOps.*
import scaloi.syntax.OptionOps.*

import java.util.Date
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

/** Worker for delivering message bus messages.
  */
@Service(unique = true)
class MessageBusWorker(implicit
  dws: DomainWebService,
  fs: FacadeService,
  qs: QueryService,
  sm: ServiceMeta,
  ts: TimeSource,
  fns: BusFailureNotificationService
) extends AbstractWorker:

  import MessageBusWorker.*

  /** The logger. */
  override protected def logger = org.log4s.getLogger

  /** How often to poll the database. */
  override protected def pollInterval = 15.seconds

  /** How often to keep queued messages alive in the database. */
  override protected def keepaliveInterval = 5.minutes

  /** How long after which to consider messages abandoned. */
  override protected def abandonInterval = 15.minutes

  /** Deliver a single bus message. */
  override protected def execute(id: Long): Unit =
    for
      msg <- id.facade_?[BusMessageFacade]
      if msg.getState == BusMessageState.Queued // sanity check
      bus = msg.getBus
      if bus.getState == MessageBusState.Active
    do
      logger.info(s"Delivering message $id for bus ${bus.getId}")
      dws.setupContext(bus.getRootId)
      val (result, stats) = processMessage(bus.getSystem, msg, ts.date)
      bus.refresh(true)
      bus.setStatistics(bus.getStatistics + stats + failureEmail(bus, msg -> result))

  /** Keep sequence of bus messages alive, owned by this appserver. */
  override protected def keepalive(ids: Seq[Long]): Unit =
    fs.getFacades(ids.boxInside().asJava, classOf[BusMessageFacade]).asScala foreach { msg =>
      if msg.getState == BusMessageState.Queued then msg.setScheduled(ts.date)
    }

  /** Poll for messages ready for execution from the database. */
  override protected def poll(): Unit =
    eligibleBuses foreach { bus =>
      LogMeta.domain(bus.getRootId)
      // set up domain context, verify domain and system are ok
      if Try(dws.setupContext(bus.getRootId)).isFailure then
        logger.info(s"Deactivating bus ${bus.getId} on invalid domain")
        bus.setState(MessageBusState.Disabled)
      else if Option(Try[SystemComponent[?]](bus.getSystem).getOrElse(null)).forall(_.getDisabled) then
        logger.info(s"Deactivating bus ${bus.getId} for inactive system")
        bus.setState(MessageBusState.Disabled)
      else if !isStopped then
        rescheduleMessages(bus)
        processBus(bus)
    }

  /** Reschedule abandoned messages for execution by the database poller. */
  private def rescheduleMessages(bus: MessageBusFacade): Unit =
    // Any queued messages with an ancient schedule time have likely been
    // abandoned (e.g. appserver failure) and should be moved to ready-state.
    val abandoned = abandonedMessages(bus)
    if abandoned.nonEmpty then
      logger.info(s"Rescheduling ${abandoned.size} abandoned messages for bus ${bus.getId}")
      abandoned foreach { msg =>
        msg.setState(BusMessageState.Ready)
      }
      ManagedUtils.commit()
  end rescheduleMessages

  /** Process an eligible message bus. */
  private def processBus(bus: MessageBusFacade): Unit =
    // Deliver next messages on this bus and sum statistics.
    val msgs  = readyMessages(bus).toList
    val level = msgs.nonEmpty.fold(org.log4s.Info, org.log4s.Debug)

    logger(level)(s"Delivering ${msgs.size} messages for bus ${bus.getId}")

    // If the bus is shut down then stop processing early
    val (msgResults, msgStats) = msgs
      .flatMap(m => isStopped.noption(processMessage(bus.getSystem, m, ts.date).mapElements(m -> _, identity)))
      .unzip

    LogMeta.remove(LogMeta.Event)
    val ready                 = queueLength(bus, BusMessageState.Ready)
    val queued                = queueLength(bus, BusMessageState.Queued)
    val s: IntervalStatistics = msgStats.suml ⊹ IntervalStatistics.queued(ready + queued)

    logger(level)(
      s"Message bus ${bus.getSystem.getName}: ${s.queued} queued, ${s.delivered} delivered, ${s.failed} failed, ${s.dropped} dropped, ${s.millis} millis."
    )

    // Update bus statistics
    bus.refresh(true)
    bus.setStatistics(bus.getStatistics + s + failureEmail(bus, msgResults*))
    val delay =
      if ready > 0 then
        // if there's work, back off as long as it took to send this
        // round of messages, so a slow target won't starve other buses
        s.millis.milliseconds
      else if queued > 0 then
        // if there's queued work, check back in a minute to update the stats
        1.minute
      else
        // if there's no work, check back rarely in case a message is added and abandoned
        abandonInterval
    bus.setScheduled(ts.date + delay)

    logger(level)(s"Bus ${bus.getId} rescheduled for ${bus.getScheduled}")
  end processBus

  /** If there is a failure and a failure email has not recently been sent, send a failure email return the current
    * time.
    */
  private def failureEmail(bus: MessageBusFacade, msgResults: (BusMessageFacade, DeliveryResult)*): Option[Date] =
    bus.getStatistics.lastEmail.forall(ts.date - _ >= EmailInterval) flatOption {
      msgResults.toSeq findMapf {
        case (_, PermanentFailure(e))                                              =>
          fns.emailFailure(Bus(bus), repeated = false, Left(e))
          ts.date
        case (msg, TransientFailure(f)) if msg.getState == BusMessageState.Dropped =>
          fns.emailFailure(Bus(bus), repeated = true, f)
          ts.date
      }
    }

  /** Process a message and return the interval statistics. This method commits the transaction upon completion.
    */
  private def processMessage(
    system: SystemComponent[?],
    msg: BusMessageFacade,
    start: Date
  ): (DeliveryResult, IntervalStatistics) =
    LogMeta.domain(msg.getRootId)
    LogMeta.event(msg.getId)
    logger.debug(s"Delivering message $msg")
    // Start the clock
    val timer                  = new Stopwatch
    // Process the messages
    val (result, senderAndMsg) = deliverMessage(system, msg) match
      case Failure(t) => (PermanentFailure(t), None)
      case Success(d) => (d._1, Some((d._2, d._3)))
    val elapsed                = timer.elapsed
    val stats                  = result match // just in case the sender does not log its own failures
      case Delivered                     =>
        LogMeta.let(
          "elapsedMillis" -> Json.jNumber(elapsed.toMillis),
        )(logger.info(s"Delivered message $msg in $elapsed"))

        // On success set delivered
        msg.setAttempts(1 + msg.getAttempts)
        msg.setState(BusMessageState.Delivered)
        msg.setScheduled(ts.date) // for posterity
        IntervalStatistics.Delivered
      case t @ TransientFailure(failure) =>
        failure match
          case Left(e)                                    =>
            logger.warn(e)(s"Transient error delivering message ${msg.getId}")
          case Right(FailureInformation(req, Left(resp))) =>
            LogMeta.let(
              "requestUrl"          -> Json.jString(req.url),
              "requestMethod"       -> Json.jString(req.method),
              "requestBody"         -> Json.jString(req.body), // oftentimes this string is some JSON-LD but /shrug
              "responseStatus"      -> Json.jNumber(resp.status),
              "responseContentType" -> Json.jString(resp.contentType),
              "responseBody"        -> Json.jString(resp.body.getOrElse("<none>")),
            )(logger.warn(s"Transient failure delivering message ${msg.getId}"))
          case Right(FailureInformation(req, Right(ex)))  =>
            val rootCause = ExceptionUtils.getRootCause(ex)
            LogMeta.let(
              "requestUrl"    -> Json.jString(req.url),
              "requestMethod" -> Json.jString(req.method),
              "requestBody"   -> Json.jString(req.body), // oftentimes this string is some JSON-LD but /shrug
            )(logger.warn(rootCause)(s"Transient failure delivering message ${msg.getId}"))
        end match

        msg.setAttempts(1 + msg.getAttempts)
        if msg.getAttempts < MaxAttempts then
          // On failure, reschedule this event with exponential backoff
          msg.setState(BusMessageState.Ready)
          msg.setScheduled(start + (1L << (msg.getAttempts * 2L)).seconds)
          IntervalStatistics.Failed
        else
          // Drop if max failures occur
          msg.setState(BusMessageState.Dropped)
          msg.setScheduled(ts.date) // for posterity
          senderAndMsg.foreach(sam => notifyDropped(t)(sam._1, sam._2))
          IntervalStatistics.Dropped
        end if
      case p @ PermanentFailure(e)       =>
        logger.warn(e)(s"Permanent error delivering message ${msg.getId}")
        msg.setState(BusMessageState.Dropped)
        msg.setScheduled(ts.date) // for posterity
        senderAndMsg.foreach(sam => notifyDropped(p)(sam._1, sam._2))
        IntervalStatistics.Dropped
    // Commit the transaction
    ManagedUtils.commit()
    result -> (IntervalStatistics.elapsed(elapsed.toMillis) |+| stats)
  end processMessage

  private def notifyDropped(failure: DeliveryFailure)(sender: AnyMessageSender, message: Any) =
    sender.onDropped(message, failure)

  // Deliver individual message. This will typically only be called for a single message.
  private def deliverMessage(
    system: SystemComponent[?],
    msg: BusMessageFacade
  ): Try[(DeliveryResult, AnyMessageSender, Any)] =
    for
      msgClass <- Try(ComponentSupport.loadClass(msg.getType))
      sender   <- getSender(system.getClass, msgClass)
                    .toTry(DeliveryException(s"Unsupported message: ${msg.getType}"))
      body     <- Try(deserialize(msg.getBody, msgClass))
      sent     <- Try(sender.sendMessage(system, body, yieldr))
    yield (sent, sender, body)

  type AnyMessageSender = MessageSender[SystemComponent[?], Any]

  private def getSender(system: Class[?], msg: Class[?]): Option[AnyMessageSender] =
    Option(ComponentSupport.lookup(classOf[AnyMessageSender], system, msg))

  private def deserialize[A](json: ObjectNode, clas: Class[A]) =
    val mapper = ComponentUtils.getObjectMapperIup
    mapper.treeToValue(json, clas)

  /** Gruesome transaction yield. TODO: DI of transaction. */
  private def yieldr = new YieldTx:

    /** Compute a value without a database transaction. */
    override def apply[A](a: => A): A =
      ManagedUtils.end()
      try
        a
      finally
        ManagedUtils.begin()

  // Total number of active messages in the queue.
  private def queueLength(bus: MessageBusFacade, state: BusMessageState): Long =
    queryMessages(bus)
      .addCondition(DATA_TYPE_BUS_MESSAGE_STATE, "eq", state)
      .getAggregateResult(Function.COUNT)
      .longValue

  // Next chunk of undelivered messages for this bus.
  private def readyMessages(bus: MessageBusFacade): Seq[BusMessageFacade] =
    queryMessages(bus)
      .addCondition(DATA_TYPE_BUS_MESSAGE_STATE, "eq", BusMessageState.Ready)
      .addCondition(DATA_TYPE_BUS_MESSAGE_SCHEDULED, "le", ts.date)
      .addOrder(query.BaseOrder.byData(DATA_TYPE_BUS_MESSAGE_SCHEDULED, query.Direction.ASC))
      .setLimit(DefaultBatchSize)
      .getFacades[BusMessageFacade]

  // All abandoned messages for this bus.
  private def abandonedMessages(bus: MessageBusFacade): Seq[BusMessageFacade] =
    queryMessages(bus)
      .addCondition(DATA_TYPE_BUS_MESSAGE_STATE, "eq", BusMessageState.Queued)
      .addCondition(DATA_TYPE_BUS_MESSAGE_SCHEDULED, "le", ts.date - abandonInterval)
      .getFacades[BusMessageFacade]

  // All of the scheduled active buses
  private def eligibleBuses: Seq[MessageBusFacade] =
    qs.queryAllDomains(ITEM_TYPE_MESSAGE_BUS)
      .addCondition(DATA_TYPE_MESSAGE_BUS_STATE, "eq", MessageBusState.Active)
      .addCondition(DATA_TYPE_MESSAGE_BUS_SCHEDULED, "le", ts.date)
      .getFacades[MessageBusFacade]

  private def queryMessages(bus: MessageBusFacade): QueryBuilder =
    qs.queryRoot(bus.getRootId, ITEM_TYPE_BUS_MESSAGE)
      .addCondition(DATA_TYPE_BUS_MESSAGE_BUS, "eq", bus)
end MessageBusWorker

object MessageBusWorker:

  private final val DefaultBatchSize = 64

  // 4 seconds, 16 seconds .. 18 hours
  private final val MaxAttempts = 9L

  private final val EmailInterval = 5.minutes

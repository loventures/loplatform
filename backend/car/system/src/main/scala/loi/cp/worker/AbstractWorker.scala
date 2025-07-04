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

package loi.cp.worker

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.operation.DispatcherOperation
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.util.ManagedUtils
import de.tomcat.juli.LogMeta
import scaloi.data.UnboundedBlockingFairKeyedQueue
import scaloi.misc.Handlers.ignoring
import scaloi.misc.TimeSource
import scaloi.syntax.DateOps.*
import scaloi.syntax.DisjunctionOps.*

import scala.annotation.meta.field
import scala.concurrent.duration.*

/** Abstract superclass for classes that perform asynchronous work.
  *
  * This maintains two worker threads. One runs from an in-memory queue of submitted work, the other polls from a
  * database queue. Jobs fail over from the in-memory queue to the database if an error arises; for example, app-server
  * failure or transient work error.
  *
  * @see
  *   [[loi.cp.worker]]
  */
abstract class AbstractWorker(implicit
  sm: ServiceMeta,
  ts: TimeSource
): // should this really be an abstract superclass?
  import AbstractWorker.*

  /** The threads this uses. */
  private val threads = List(
    new Thread(() => poller(), getClass.getSimpleName + "-Poll"),
    new Thread(() => executor(), getClass.getSimpleName + "-Queue")
  )

  /** Work queue. */
  private val queue = UnboundedBlockingFairKeyedQueue.empty[Long, Long]

  /** When the queue was last empty or kept alive. */
  @(SingletonState @field)
  private var timestamp = ts.date

  /** Whether this worker is stopped. */
  @(SingletonState @field)
  private var stopped = false

  /** The monitor. */
  private object monitor

  /** Start this worker.
    */
  @PostLoad
  @SuppressWarnings(Array("unused"))
  def start(): Unit =
    logger.info("Starting worker")
    threads foreach { _.start() }

  /** Shut down this worker.
    */
  @PreUnload
  @PreShutdown
  @SuppressWarnings(Array("unused"))
  def shutdown(): Unit =
    logger.info("Stopping worker")
    stopped = true
    threads foreach { _.interrupt() }
    threads foreach { _.join(ShutdownWaitTime.toMillis) }
    logger.info(s"Stopped worker")

  /** Offer a unit of work for immediate execution.
    * @param group
    *   the work group (for fair scheduling). This is often, but does not always need to be, a domain ID.
    * @param id
    *   the work identifier This is often, but does not always need to be, an item ID.
    */
  def offer(group: Long, id: Long): Unit =
    queue.offer(group, id)

  /** Run the poller.
    */
  private def poller(): Unit =
    logger.info("Poller started")
    while !stopped do
      treither {
        // if i am currently the DAS then poll
        if sm.isDas then
          logger.debug("Polling")
          ManagedUtils.perform(new DispatcherOperation[Unit](() => poll(), categoryName, s"$transactionPrefix/poll"))
      } -<| { e =>
        logger.warn(e)("Poller error")
      }
      try
        monitor.synchronized {
          monitor `wait` pollInterval.toMillis
        }
      catch ignoring[InterruptedException]
      Current.clear()
      LogMeta.clear()
    end while
    logger.info("Poller shut down")
  end poller

  /** Run the queue processor.
    */
  private def executor(): Unit =
    logger.info("Executor started")
    while !stopped do
      treither {
        queue.take(abandonInterval / 2) match
          case Some(id) =>
            LogMeta.event(id)
            treither {
              logger.debug(s"Executing $id")
              ManagedUtils.perform(
                new DispatcherOperation[Unit](() => execute(id), categoryName, s"$transactionPrefix/execute", "id", id)
              )
              logger.debug("Execution complete")
            } -<| { e =>
              logger.warn(e)(s"Error executing $id")
            }
          case None     =>
            logger.info(s"Did not find work in ${abandonInterval / 2}.")
        end match
        keepalive()
      } -<| { e =>
        logger.warn(e)("Executor error")
      }
      Current.clear()
      LogMeta.clear()
    end while
    logger.info("Executor shut down")
  end executor

  /** Category name for reporting to APM. */
  private val categoryName = getClass.getSimpleName

  /** Transaction prefix for reporting to APM. */
  private val transactionPrefix = getClass.getName

  /** Keep the in-memory queued messages alive in the database.
    */
  private def keepalive(): Unit =
    // The queue could keep track of the oldest element in it, but either the queue
    // will periodically empty and this is good enough, or the queue will grow forever
    // and we will die from out of memory.
    val now = ts.date
    if queue.isEmpty then timestamp = now
    else if now - timestamp >= abandonInterval then
      // If i have not been able to keep the queue alive for ages i need to abandon it
      // because the jobs may have been stolen
      logger.warn("Abandoning queue due to timeout")
      queue.clear()
      timestamp = now
    else if now - timestamp >= keepaliveInterval then
      // If i have had a busy queue for a while then keep those work items alive in the database
      val values = queue.toMap.values.toSeq.flatten
      logger.info(s"Keeping ${values.size} queue items alive")
      ManagedUtils.perform(() => keepalive(values))
      timestamp = now
    end if
  end keepalive

  /** Return whether this poller has been stopped.
    */
  protected final def isStopped: Boolean = stopped

  /** Poll the database for work.
    *
    * The implementation should atomically select unexecuted work items from the database and mark them as enqueued, and
    * then call `offer` to place them in the queue. Additionally, it should find work items which appear abandoned, and
    * either resurrect them in a similar way as new work items, or mark them as failed and permanently abandoned. The
    * usual way that this is done is to have the items carry a "queue time" or "scheduled time", which `keepalive`
    * periodically updates to indicate that the work remains in the queue. If an item has a queue time that is more than
    * `abandonInterval` ago, it ought to be deemed abandoned and handled appropriately.
    */
  protected def poll(): Unit

  /** Execute a single work item
    * @param id
    *   the id of the work item
    */
  protected def execute(id: Long): Unit

  /** Keep these queued work items alive.
    * @param ids
    *   the ids of the work items
    */
  protected def keepalive(ids: Seq[Long]): Unit

  /** Pump this worker. This is `protected` with the intention that subclasses which so desire can expose it as public
    * instead, while those which do not so desire can have it remain inaccessible.
    */
  protected def pump(): Unit =
    monitor synchronized { monitor.notify() }

  /** Logger. */
  protected def logger: org.log4s.Logger

  /** How often to check the message buses in the database. */
  protected def pollInterval: FiniteDuration

  /** How often to keep memory work alive in the database. */
  protected def keepaliveInterval: FiniteDuration

  /** How long after which to assume work has been abandoned by its appserver. */
  protected def abandonInterval: FiniteDuration

  private[worker] final def queueSnapshot: Map[Long, List[Long]] =
    queue.toMap
end AbstractWorker

/** Abstract poller companion. */
object AbstractWorker:

  /** Shutdown wait time. */
  private val ShutdownWaitTime = 20.seconds

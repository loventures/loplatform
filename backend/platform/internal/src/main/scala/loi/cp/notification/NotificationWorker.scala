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

package loi.cp.notification

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.domain.DomainWebService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.notification.NotificationConstants.*
import com.learningobjects.cpxp.service.query.*
import com.learningobjects.cpxp.util.ManagedUtils
import de.tomcat.juli.LogMeta
import loi.cp.worker.AbstractWorker
import scaloi.misc.TimeSource

import scala.concurrent.duration.*
import scala.util.Try
import scala.util.control.NonFatal

/** Worker for processing notifications.
  */
@Service(unique = true)
class NotificationWorker(implicit
  dws: DomainWebService,
  fs: FacadeService,
  ns: NotifyService,
  qs: QueryService,
  sm: ServiceMeta,
  ts: TimeSource,
  cs: ComponentService
) extends AbstractWorker:
  import NotificationWorker.*

  /** The logger. */
  override protected def logger = org.log4s.getLogger

  /** How often to poll the database. */
  override protected def pollInterval = 15.seconds

  /** How often to keep JVM-queued notifications alive in the database. */
  override protected def keepaliveInterval = 5.minutes

  /** How long after which to consider notifications abandoned. */
  override protected def abandonInterval = 15.minutes

  /** Process a single notification. */
  override protected def execute(id: Long): Unit =
    id.facade_?[NotificationFacade] foreach { event =>
      logger.info(s"Processing notification $id")
      dws.setupContext(event.getRootId)
      processEvent(event)
    }

  /** Keep sequence of notifications alive, owned by this appserver. */
  override protected def keepalive(ids: Seq[Long]): Unit = ()

  /** Poll for notifications ready for execution from the database. */
  override protected def poll(): Unit =
    // domain environments take time (do they?), so pick a domain then pick events on that domain
    qs.queryAllDomains(ITEM_TYPE_NOTIFICATION)
      .addCondition(DATA_TYPE_NOTIFICATION_PROCESSED, Comparison.eq, null)
      .addOrder(BaseOrder.byData(DataTypes.META_DATA_TYPE_ID, Direction.ASC)) // TODO: order by date if index works
      .setProjection(Projection.ROOT_ID)
      .setLimit(1)
      .getValues[Long] foreach processDomain

  def processDomain(domain: Long): Unit =
    LogMeta.domain(domain)
    dws.setupContext(domain)
    val events = qs
      .queryRoot(ITEM_TYPE_NOTIFICATION)
      .addCondition(DATA_TYPE_NOTIFICATION_PROCESSED, Comparison.eq, null)
      .addOrder(BaseOrder.byData(DataTypes.META_DATA_TYPE_ID, Direction.ASC)) // TODO: order by date if index works
      .setLimit(EventBatchSize)
      .getFacades[NotificationFacade]
    logger.info(s"Processing ${events.size} events for domain $domain")
    events foreach processEvent
    Current.clear()
  end processDomain

  def processEvent(notif: NotificationFacade): Unit =
    LogMeta.domain(notif.getRootId)
    LogMeta.event(notif.getId)
    // refresh doesn't reliably return false, it happily throws and breaks the tx
    Try(notif.refresh(0)).recover({ case _ => ManagedUtils.rollback(); false }) foreach { locked =>
      if locked && notif.getProcessed.isEmpty then
        // if i can lock it and it has not been processed
        try
          // this sets processed to true upon success
          val (n, a, e) = ns.scatterEvent(notif.component[Notification])
          ManagedUtils.commit()
          logger.info(s"Scattered event ${notif.getId}: $n notifications, $a alerts, $e emails")
        catch
          case NonFatal(e) =>
            logger.warn(e)(s"Error processing event ${notif.getId}")
            ManagedUtils.rollback()
            if notif.refresh(5000) && notif.getProcessed.isEmpty then
              // lock and mark failed
              notif.setProcessed(false)
            ManagedUtils.commit()
    }
  end processEvent
end NotificationWorker

object NotificationWorker:

  private val EventBatchSize = 64

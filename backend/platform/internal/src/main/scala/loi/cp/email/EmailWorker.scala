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

package loi.cp.email

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.domain.{DomainConstants, DomainState, DomainWebService}
import com.learningobjects.cpxp.service.email.EmailFinder.*
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.*
import com.learningobjects.cpxp.util.ManagedUtils
import de.tomcat.juli.LogMeta
import loi.cp.worker.AbstractWorker
import scaloi.misc.TimeSource

import scala.concurrent.duration.*
import scala.util.Try
import scala.util.control.NonFatal

/** Worker for sending queued emails.
  */
@Service(unique = true)
class EmailWorker(implicit
  dws: DomainWebService,
  es: EmailService,
  fs: FacadeService,
  qs: QueryService,
  sm: ServiceMeta,
  ts: TimeSource,
  cs: ComponentService
) extends AbstractWorker:
  import EmailWorker.*

  /** The logger. */
  override protected def logger = org.log4s.getLogger

  /** How often to poll the database. */
  override protected def pollInterval = 15.seconds

  /** How often to keep JVM-queued emails alive in the database. */
  override protected def keepaliveInterval = 5.minutes

  /** How long after which to consider emails abandoned. */
  override protected def abandonInterval = 15.minutes

  /** Process a single email. */
  override protected def execute(id: Long): Unit =
    id.facade_?[EmailFacade] foreach { email =>
      LogMeta.domain(email.getRootId)
      LogMeta.event(email.getId)
      logger.info(s"Processing email $id")
      processEmail(email)
    }

  /** Keep sequence of emails alive, owned by this appserver. */
  override protected def keepalive(ids: Seq[Long]): Unit = ()

  /** Poll for emails ready for execution from the database. */
  override protected def poll(): Unit =
    qs.queryAllDomains(ITEM_TYPE_EMAIL)
      .addJoin(nonDeletedDomains)
      .addCondition(DATA_TYPE_EMAIL_SUCCESS, Comparison.eq, null)
      .setProjection(Projection.ID)
      .setLimit(EmailBatchSize)
      .setOrder(DataTypes.META_DATA_TYPE_ID, Direction.ASC)
      .getValues[Long] foreach execute

  /** A join to exclude deleted domains from the poller results. */
  private def nonDeletedDomains: Join =
    Join.Inner(
      DataTypes.META_DATA_TYPE_ROOT_ID,
      qs.queryAllDomains(DomainConstants.ITEM_TYPE_DOMAIN)
        .addCondition(DomainConstants.DATA_TYPE_DOMAIN_STATE, Comparison.eq, DomainState.Normal)
    )

  private def processEmail(facade: EmailFacade): Unit =
    Try(facade.refresh(0)).recover({ case _ => ManagedUtils.rollback(); false }) foreach { locked =>
      if locked && facade.getSuccess.isEmpty then
        // if i can lock it and it has not been processed
        try
          dws.setupUserContext(facade.getParentId)
          val email = facade.component[Email]
          logger.info(s"Sending email: ${email.getId} - ${email.getUser.getEmailAddress}}")
          es.sendEmail(MarshalEmailSupport.domainMessageId(email.getId), email.buildEmail)
          facade.setSent(ts.date)
          facade.setSuccess(true)
          ManagedUtils.commit()
          logger.info("Sent email")
        catch
          case NonFatal(e) =>
            logger.warn(e)(s"Error sending email ${facade.getId}")
            ManagedUtils.rollback()
            if facade.refresh(5000) && facade.getSuccess.isEmpty then
              // lock and mark failed
              facade.setSent(ts.date)
              facade.setSuccess(false)
            ManagedUtils.commit()
    }
end EmailWorker

object EmailWorker:
  private final val EmailBatchSize = 64

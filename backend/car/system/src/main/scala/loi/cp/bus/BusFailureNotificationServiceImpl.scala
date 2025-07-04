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

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainWebService
import com.learningobjects.cpxp.service.email.EmailService
import org.apache.commons.lang3.exception.ExceptionUtils
import scaloi.syntax.DisjunctionOps.*

import scala.util.Try

@Service
class BusFailureNotificationServiceImpl(
  dws: DomainWebService,
  sm: ServiceMeta,
  es: EmailService
) extends BusFailureNotificationService:

  import BusFailureNotificationServiceImpl.*

  /** Analytics infrastructure email address. */
  private val AnalyticsInfrastructure = "analytics-infrastructure@learningobjects.com"

  /** Send an email about an analytics failure. */
  override def emailFailure(bus: Bus, repeated: Boolean, failure: Either[Throwable, FailureInformation]): Unit =
    treither {

      val recipientEmail = failure match
        case Left(th)    => AnalyticsInfrastructure
        case Right(info) =>
          if info.req.url.startsWith("http://hlh123.localtunnel.me") then "hherzog@learningobjects.com"
          else AnalyticsInfrastructure

      val name    = Try(bus.getBusName).getOrElse(bus.getId.toString)
      val domain  = dws.getDomain(bus.getRootId)
      val subject = s"Bus failure: $name / ${domain.getName}"
      val intro   =
        if repeated then "A series of unfortunate transient errors occurred."
        else "A permanent fatal error occurred."

      val err = failure match
        case Left(th)    =>
          s"""
             |<div>
             |Stack Trace: <br />
             |<pre style="background: #eee; padding: 10px">${ExceptionUtils
              .getRootCauseStackTrace(th)
              .mkString("\n")}</pre>
             |</div>
           """.stripMargin
        case Right(info) =>
          s"""
             |<div>
             |Request Info: <br />
             |  url: ${info.req.url} <br />
             |  method: ${info.req.method} <br />
             |  body: <br />
             |<pre style="background: #eee; padding: 10px">${info.req.body}</pre>
             |
             |${info.resp.fold(
              resp => s"""
                 |Response Info: <br />
                 |  status: ${resp.status} <br />
                 |  contentType: ${resp.contentType} <br />
                 |  body: <br />
                 |<pre style="background: #eee; padding: 10px">${resp.body}</pre>
               """.stripMargin,
              ex =>
                s"""
                 |Stack Trace: <br />
                 |<pre style="background: #eee; padding: 10px">${ExceptionUtils
                    .getRootCauseStackTrace(ex)
                    .mkString("\n")}</pre>
               """.stripMargin
            )}
             |</div>
           """.stripMargin

      val body =
        s"""$intro
           |
           |Domain: ${domain.getName}
           |URL: https://${domain.getPrimaryHostName}/
           |Bus: $name
           |Cluster: ${sm.getCluster}
           |
           |Error:
           |  $err
         """.stripMargin
      es.sendTextEmail("noreply@" + domain.getPrimaryHostName, "Bus Failure", recipientEmail, null, subject, body, true)
    } -<| { th =>
      logger.warn(th)("Error sending bus failure email")
    }
end BusFailureNotificationServiceImpl

object BusFailureNotificationServiceImpl:

  private val logger = org.log4s.getLogger

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

package loi.cp.slack

import java.net.URI
import java.nio.charset.StandardCharsets
import jakarta.servlet.http.HttpServletResponse.*

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.annotation.Service
import scaloi.syntax.AnyOps.*
import com.learningobjects.cpxp.service.integration.IntegrationWebService
import com.learningobjects.cpxp.util.HttpUtils
import com.learningobjects.de.web.MediaType
import com.typesafe.config.{Config, ConfigException}
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils

import scala.util.{Failure, Success, Try}
import scalaz.\/

/** The Slack system implementation. */
@Service
class SlackServiceImpl(implicit
  cfg: Config,
  client: HttpClient,
  iws: IntegrationWebService,
  om: ObjectMapper
) extends SlackService:
  import SlackService.*
  import SlackServiceImpl.*

  override def postMessage(body: String, attachment: Option[SlackAttachment]): SlackResponse =
    if webhookURI.isEmpty then invalidConfiguration
    else doPost(body, attachment)(webhookURI.get)

  private def doPost(body: String, attachment: Option[SlackAttachment])(uri: URI): SlackResponse =
    Try(
      client `execute` new HttpPost <| { req =>
        req.setURI(uri)
        req.setHeader(HttpUtils.HTTP_HEADER_CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        req.setEntity(
          new StringEntity(om.writeValueAsString(WebhookRequest(body, attachment.toList)), StandardCharsets.UTF_8)
        )
      }
    ) match
      case Failure(exeption) => SendingFailedWithException(exeption)
      case Success(rsp)      =>
        val content = EntityUtils.toString(rsp.getEntity)
        (rsp.getStatusLine.getStatusCode, content.trim) match
          // see https://api.slack.com/changelog/2016-05-17-changes-to-errors-for-incoming-webhooks
          case (SC_OK, _)                          =>
            successfullySent
          case (SC_BAD_REQUEST, "invalid_payload") =>
            SendingFailedWithMessage(s"OverlordSlackSystem somehow made bad JSON.")
          case (SC_BAD_REQUEST, "user_not_found")  =>
            SendingFailedWithMessage(s"OverlordSlackSystem user was not found on the target Slack system")
          case (SC_FORBIDDEN, "action_prohibited") =>
            SendingFailedWithMessage(s"OverlordSlackSystem user needs permission to post in the channel.")
          case (SC_NOT_FOUND, "channel_not_found") =>
            SendingFailedWithMessage(s"The channel OverlordSlackSystem is configured to post in has been removed.")
          case (SC_GONE, "channel_is_archived")    =>
            SendingFailedWithMessage(s"The channel OverlordSlackSystem is configured to post in has been archived.")
          case (sc, msg)                           =>
            SendingFailedWithMessage(s"An unspecified error occurred: status code $sc, message $msg.")
        end match

  private def webhookURI: Option[URI] =
    \/.attempt(URI.create(cfg.getString("loi.cp.slack.webhook-uri"))) {
      case _: ConfigException.WrongType =>
        logger.warn("Slack webhook URI not a string")
      case _: IllegalArgumentException  =>
        logger.warn("Slack webhook URI not a valid URI")
      case _                            => /* not found, &c. */
    }.toOption
end SlackServiceImpl

object SlackServiceImpl:
  val logger = org.log4s.getLogger

  case class WebhookRequest(text: String, attachments: List[SlackAttachment], link_names: Int = 1)

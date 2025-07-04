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

import com.learningobjects.cpxp.component.annotation.Service

/** A slackful service.
  *
  * Provides overlördes the ability to speak to Slack.
  */
@Service
trait SlackService:
  import SlackService.*

  /** Post a message to slack and return the result */
  def postMessage(body: String, attachment: Option[SlackAttachment] = None): SlackResponse

object SlackService:

  /** A slackful response. */
  sealed trait SlackResponse

  /** The message was successfully sent. */
  case object SuccessfullySent extends SlackResponse

  /** No Slack system has been configured. This should not usually be considered erroneous.
    */
  case object NotConfigured extends SlackResponse

  /** An exception occurred while slacking. */
  case class SendingFailedWithException(wrappedException: Throwable) extends SlackResponse

  /** Something slackful wrong way went, and we know why. */
  case class SendingFailedWithMessage(msg: String) extends SlackResponse

  def invalidConfiguration: SlackResponse = NotConfigured
  def successfullySent: SlackResponse     = SuccessfullySent
end SlackService

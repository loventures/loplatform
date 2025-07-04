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

package loi.cp.reply

import java.util.Date

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo, JsonProperty, JsonIgnoreProperties}
import com.fasterxml.jackson.databind.annotation.JsonSerialize

/** SNS message model. In is unclear which of these schemata are SES-specific, SNS-specific and SQS-specific.
  */
@JsonIgnoreProperties(ignoreUnknown = true)
case class SnsMessage(
  @JsonProperty("Type") messageType: String,
  @JsonProperty("MessageId") messageId: String,
  @JsonProperty("TopicArn") topicArn: String,
  @JsonProperty("Subject") subject: String,
  @JsonProperty("Message") message: String,
  @JsonProperty("Timestamp") timestamp: Date,
  @JsonProperty("SignatureVersion") signatureVersion: String,
  @JsonProperty("Signature") signature: String,
  @JsonProperty("SigningCertURL") signingCertUrl: String,
  @JsonProperty("UnsubscribeURL") unsubscribeUrl: String
)

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "notificationType",
  defaultImpl = classOf[SnsUnknownNotification],
  visible = true
)
@JsonSubTypes(
  Array(
    new Type(name = "Received", value = classOf[SesReceivedNotification])
  )
)
@JsonSerialize
sealed trait SnsNotification

@JsonIgnoreProperties(ignoreUnknown = true)
case class SesReceivedNotification(
  receipt: SesReceipt,
  mail: SesMail
) extends SnsNotification

@JsonIgnoreProperties(ignoreUnknown = true)
case class SnsUnknownNotification(
  notificationType: String
) extends SnsNotification

@JsonIgnoreProperties(ignoreUnknown = true)
case class SesReceipt(
  timestamp: Date,
  processingTimeMillis: Long,
  recipients: List[String],
  spamVerdict: Option[SesVerdict],
  virusVerdict: Option[SesVerdict],
  spfVerdict: Option[SesVerdict],
  dkimVerdict: Option[SesVerdict],
  action: SesAction
)

@JsonIgnoreProperties(ignoreUnknown = true)
case class SesVerdict(
  status: String
)

object SesVerdict:
  val Pass = "PASS"

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type",
  defaultImpl = classOf[SesUnknownAction],
  visible = true
)
@JsonSubTypes(
  Array(
    new Type(name = "S3", value = classOf[S3Action])
  )
)
@JsonSerialize
sealed trait SesAction

@JsonIgnoreProperties(ignoreUnknown = true)
case class S3Action(
  topicArn: String,
  bucketName: String,
  objectKeyPrefix: String,
  objectKey: String
) extends SesAction

@JsonIgnoreProperties(ignoreUnknown = true)
case class SesUnknownAction(
  @JsonProperty("type") actionType: String
) extends SesAction

@JsonIgnoreProperties(ignoreUnknown = true)
case class SesMail(
  timestamp: Date,
  source: String,
  messageId: String,
  destination: List[String],
  headersTruncated: Boolean,
  headers: List[SesHeader],
  commonHeaders: SesCommonHeaders
)

case class SesHeader(
  name: String,
  value: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
case class SesCommonHeaders(
  returnPath: Option[String],
  from: Option[List[String]],
  replyTo: Option[List[String]],
  to: Option[List[String]],
  date: String,
  messageId: String
)

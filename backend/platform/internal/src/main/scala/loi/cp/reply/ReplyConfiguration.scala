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

import com.typesafe.config.Config

import scalaz.syntax.std.boolean.*

/** Reply configuration.
  *
  * @param queueUrl
  *   the SQS queue URL
  * @param accessKey
  *   the AWS access key
  * @param secretKey
  *   the AWS secret key
  * @param domainName
  *   the email domain name associated with the cluster
  */
case class ReplyConfiguration(
  queueUrl: String,
  accessKey: String,
  secretKey: String,
  domainName: String
)

/** Reply configuration singleton.
  */
object ReplyConfiguration:

  /** Retrieve the reply configuration from context.xml.
    * @return
    *   the reply configuration, if specified
    */
  def get(implicit config: Config): Option[ReplyConfiguration] =
    val replyConf = config.getConfig("loi.cp.reply")
    replyConf
      .getBoolean("configured")
      .option(
        ReplyConfiguration(
          replyConf.getString("queueUrl"),
          replyConf.getString("accessKey"),
          replyConf.getString("secretKey"),
          replyConf.getString("domainName")
        )
      )
  end get
end ReplyConfiguration

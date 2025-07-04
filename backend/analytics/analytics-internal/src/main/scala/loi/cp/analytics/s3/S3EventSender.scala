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

package loi.cp.analytics.s3

import com.fasterxml.jackson.databind.SerializationFeature
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.service.Current
import com.typesafe.config.Config
import loi.cp.analytics.bus.AnalyticBusConfiguration
import loi.cp.analytics.config.AnalyticsConfig
import loi.cp.analytics.config.AnalyticsS3Config.*
import loi.cp.analytics.{Analytic, AnalyticsSender, DeliveryResult}
import org.log4s.Logger
import scalaz.\/
import scalaz.std.option.*
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest

import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.{Calendar, Date}

@Service
class S3EventSender(
  cfg: Config,
  sm: ServiceMeta
) extends AnalyticsSender:

  import S3EventSender.log

  val format = new SimpleDateFormat("yyyy/MM/dd")

  override def sendAnalytics(
    events: Seq[Analytic],
    busConfig: AnalyticBusConfiguration,
    lastMaterializedViewRefreshDate: Option[Date]
  ): DeliveryResult =
    \/.attempt({
      val analyticsConfig = AnalyticsConfig.fromRootConfig(cfg)

      val s3     = analyticsConfig.events.s3.getClient(analyticsConfig.region)
      val mapper = JacksonUtils.getMapper.copy().disable(SerializationFeature.INDENT_OUTPUT)

      val jsonLines = events.to(LazyList).map { event =>
        mapper.writeValueAsString(event) + "\n"
      }
      val bytes     = jsonLines.flatMap(_.getBytes(StandardCharsets.UTF_8)).toArray

      log.info(s"Writing event Stream to s3 w/ byte length: ${bytes.length}")

      s3.putObject(
        PutObjectRequest
          .builder()
          .bucket(analyticsConfig.bucket)
          .key(s"${analyticsConfig.events.s3.prefix}/${getFilePath}")
          .contentLength(bytes.length.toLong)
          .build(),
        RequestBody.fromBytes(bytes)
      )
      DeliveryResult.success()
    })(DeliveryResult.permanentFailure).merge

  def getFilePath =
    val day = format.format(Calendar.getInstance().getTime())
    s"${sm.getCluster}/${Current.getDomain}/$day/${Instant.now.toEpochMilli}.json"
end S3EventSender

object S3EventSender:
  final val log: Logger = org.log4s.getLogger

  final val S3EventSenderIdentifier = "S3EventSender"

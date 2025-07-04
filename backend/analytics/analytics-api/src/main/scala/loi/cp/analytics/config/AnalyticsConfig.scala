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

package loi.cp.analytics.config

import com.typesafe.config.Config
import loi.cp.analytics.config.AnalyticsConfig.{RedshiftConfig, S3Config}
import loi.cp.analytics.config.AnalyticsS3Config.{AWSCredentialConfig, Absent}
import net.ceedubs.ficus.readers.ValueReader

import scala.util.Try
import scalaz.Const
import scalaz.Id.Id

case class AnalyticsConfig(
  region: String,
  bucket: String,
  events: S3Config[Option, Option],
  redshift: RedshiftConfig
)

object AnalyticsConfig:

  import net.ceedubs.ficus.Ficus.*

  case class RedshiftConfig(s3: AnalyticsS3Config[Option, Option], bucket: String, role: String)

  implicit def redshiftConfigReader: ValueReader[RedshiftConfig] = ValueReader.relative(config =>

    val s3Config = config.getConfig("s3")
    val s3       = AnalyticsS3Config(
      prefix = s3Config.getString("prefix"),
      endpoint = Try(s3Config.getString("endpoint")).toOption,
      credentials = Try(s3Config.getConfig("credentials").as[AWSCredentialConfig]).toOption
    )

    RedshiftConfig(s3, config.getString("bucket"), config.getString("role"))
  )

  case class S3Config[E[_], C[_]](s3: AnalyticsS3Config[E, C])

  implicit def optReader: ValueReader[S3Config[Option, Option]] =
    ValueReader.relative { config =>
      val s3Config = config.getConfig("s3")
      S3Config(
        AnalyticsS3Config(
          prefix = s3Config.getString("prefix"),
          endpoint = Try(s3Config.getString("endpoint")).toOption,
          credentials = Try(s3Config.getConfig("credentials").as[AWSCredentialConfig]).toOption
        )
      )
    }

  implicit def absReader: ValueReader[S3Config[Absent, Id]] =
    ValueReader.relative { config =>
      val s3Config = config.getConfig("s3")
      S3Config(
        AnalyticsS3Config[Absent, Id](
          prefix = s3Config.getString("prefix"),
          endpoint = Const[Unit, String](()),
          credentials = s3Config.getConfig("credentials").as[AWSCredentialConfig]
        )
      )
    }

  given ValueReader[AnalyticsConfig] = ValueReader.relative: config =>
    AnalyticsConfig(
      config.getString("region"),
      config.getString("bucket"),
      config.getConfig("events").as[S3Config[Option, Option]],
      config.getConfig("redshift").as[RedshiftConfig],
    )

  def fromRootConfig(cfg: Config): AnalyticsConfig =
    cfg.getConfig("loi.cp.analytics").as[AnalyticsConfig]
end AnalyticsConfig

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

import software.amazon.awssdk.services.s3.{S3Client, S3ClientBuilder, S3Configuration}
import loi.cp.analytics.config.AnalyticsS3Config.AWSCredentialConfig
import scalaz.{Const, Optional, \/}
import scalaz.syntax.either.*
import net.ceedubs.ficus.readers.ValueReader
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region

import java.net.URI

case class AnalyticsS3Config[E[_], C[_]](
  prefix: String,
  endpoint: E[String],
  credentials: C[AWSCredentialConfig]
)

object AnalyticsS3Config:

  type Absent[A] = Const[Unit, A]

  object Absent:
    implicit val optional: Optional[Absent] = new Optional[Absent]:
      override def pextract[B, A](fa: Absent[A]): \/[Absent[B], A] = fa.asInstanceOf[Const[Unit, B]].left

  case class AWSCredentialConfig(accessKeyId: String, secretKey: String)

  object AWSCredentialConfig:
    given ValueReader[AWSCredentialConfig] = ValueReader.relative: config =>
      AWSCredentialConfig(
        config.getString("accessKeyId"),
        config.getString("secretKey"),
      )

  implicit class AnalyticsS3ConfigOps[E[_]: Optional, C[_]: Optional](val s3Config: AnalyticsS3Config[E, C]):
    def getClient(region: String): S3Client =
      val builder = S3Client.builder.region(Region.of(region)).bindCredentials(s3Config.credentials)
      implicitly[Optional[E]]
        .toOption(s3Config.endpoint)
        .foreach: endpoint =>
          builder
            .serviceConfiguration(
              S3Configuration.builder.chunkedEncodingEnabled(false).pathStyleAccessEnabled(true).build
            )
            .endpointOverride(URI.create(endpoint))
      builder.build
    end getClient
  end AnalyticsS3ConfigOps

  implicit class AwsClientBuilderOps(builder: S3ClientBuilder):

    def bindCredentials[C[_]: Optional](creds: C[AWSCredentialConfig]): S3ClientBuilder =
      implicitly[Optional[C]].toOption(creds) match
        case None    => builder // don't configure credentials in favor of instance profile
        case Some(c) =>
          builder.credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(c.accessKeyId, c.secretKey))
          )
end AnalyticsS3Config

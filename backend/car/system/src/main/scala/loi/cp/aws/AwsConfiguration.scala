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

package loi.cp.aws

import cats.syntax.option.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.learningobjects.de.authorization.Secured
import loi.cp.config.{ConfigurationKey, ConfigurationKeyBinding}
import loi.cp.overlord.OverlordRight

/** Cluster-level configuration for AWS services.
  */
@JsonIgnoreProperties(ignoreUnknown = true)
case class AwsConfiguration(
  s3Failover: Boolean = false,
  cfDisabled: Boolean = false,
)

@ConfigurationKeyBinding(
  value = "aws",
  read = new Secured(value = Array(classOf[OverlordRight])),
  write = new Secured(value = Array(classOf[OverlordRight]))
)
object AwsConfiguration extends ConfigurationKey[AwsConfiguration]:
  import loi.cp.config.JsonSchema.*

  val instance: this.type = this

  override val init: AwsConfiguration = new AwsConfiguration

  override final val schema = Schema(
    title = "AWS".some,
    properties = List(
      BooleanField("s3Failover", title = "S3 Failover".some, description = Some("Fail S3 over to geographic replica")),
      BooleanField("cfDisabled", title = "CloudFront CDN Disabled".some, description = Some("Disable CloudFront CDN")),
    )
  )
end AwsConfiguration

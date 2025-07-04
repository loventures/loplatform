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

package loi.cp.structure

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.annotation.{Component, Configuration, Schema}
import com.learningobjects.cpxp.component.util.ConfigUtils.decodeConfigurationValue
import loi.cp.integration.{AbstractSystem, SystemComponent}

import java.lang.Boolean as JBoolean
import scala.jdk.CollectionConverters.*

/** * Configure a system that uploads course structures to an Amazon S3 bucket every time a course is published or
  * updated
  */

@Schema("courseStructureUpload")
trait CourseStructureUploadSystem extends SystemComponent[CourseStructureUploadSystem]:

  // if using InstanceProfile, the following three will be sufficient
  @JsonProperty def getRegion: String
  @JsonProperty def getBucket: String
  @JsonProperty def getPrefix: String // often null

  // AWS InstanceProfile is configured for EC2 instances, and EC2 only
  @JsonProperty def getUseInstanceProfile: JBoolean

  @JsonProperty def getUseAssumeRole: JBoolean
  @JsonProperty def getAssumeRoleArn: String

  // if not using InstanceProfile, access is also permitted with Access Key and Secret Access Key
  @JsonProperty def getAccessKeyId: String
  @JsonProperty def getSecretAccessKey: String

  // only used for testing, secret variable
  @JsonProperty def getCustomEndpointUrl: String
end CourseStructureUploadSystem

@Component(name = "Course Data Upload")
class CourseStructureUploadSystemImpl
    extends AbstractSystem[CourseStructureUploadSystem]
    with CourseStructureUploadSystem:
  import CourseStructureUploadSystem.*

  // this function exposed exclusively to enable testing
  def update(
    region: String,
    bucket: String,
    prefix: String,
    instanceProfile: JBoolean,
    assumeRole: JBoolean,
    assumeRoleArn: String,
    accessKey: String,
    secretKey: String,
    endpointUrl: String
  ): Unit =
    setConfiguration(
      Map(
        CONFIG_AWS_REGION           -> region,
        CONFIG_AWS_BUCKET           -> bucket,
        CONFIG_KEY_PREFIX           -> prefix,
        CONFIG_USE_INSTANCE_PROFILE -> instanceProfile,
        CONFIG_USE_ASSUME_ROLE      -> assumeRole,
        CONFIG_ASSUME_ROLE_ARN      -> assumeRoleArn,
        CONFIG_ACCESS_KEY_ID        -> accessKey,
        CONFIG_SECRET_ACCESS_KEY    -> secretKey,
        CONFIG_CUSTOM_ENDPOINT_URL  -> endpointUrl
      ).asJava
    )

  override def update(system: CourseStructureUploadSystem): CourseStructureUploadSystem =
    // all properties on the configuration column
    update(
      system.getRegion,
      system.getBucket,
      system.getPrefix,
      system.getUseInstanceProfile,
      system.getUseAssumeRole,
      system.getAssumeRoleArn,
      system.getAccessKeyId,
      system.getSecretAccessKey,
      system.getCustomEndpointUrl
    )

    super.update(system)
  end update

  @Configuration(label = "$$field_awsRegion=AWS Region", order = 0)
  override def getRegion: String =
    decodeConfigurationValue(_self.getConfiguration, CONFIG_AWS_REGION, classOf[String])

  @Configuration(label = "$$field_awsBucket=AWS Bucket", order = 1)
  override def getBucket: String =
    decodeConfigurationValue(_self.getConfiguration, CONFIG_AWS_BUCKET, classOf[String])

  @Configuration(label = "$$field_keyPrefix=Key Prefix", order = 2)
  override def getPrefix: String =
    decodeConfigurationValue(_self.getConfiguration, CONFIG_KEY_PREFIX, classOf[String])

  @Configuration(label = "$$field_awsInstanceProfile=Use AWS Instance Profile", order = 3)
  override def getUseInstanceProfile: JBoolean =
    decodeConfigurationValue(_self.getConfiguration, CONFIG_USE_INSTANCE_PROFILE, classOf[JBoolean])

  @Configuration(label = "$$field_assumeRole=Assume IAM Role", order = 4)
  override def getUseAssumeRole: JBoolean =
    decodeConfigurationValue(_self.getConfiguration, CONFIG_USE_ASSUME_ROLE, classOf[JBoolean])

  @Configuration(label = "$$field_assumeRoleArn=AWS IAM Assumed Role ARN", order = 5)
  override def getAssumeRoleArn: String =
    decodeConfigurationValue(_self.getConfiguration, CONFIG_ASSUME_ROLE_ARN, classOf[String])

  @Configuration(label = "$$field_awsKeyId=Access Key ID", order = 6)
  override def getAccessKeyId: String =
    decodeConfigurationValue(_self.getConfiguration, CONFIG_ACCESS_KEY_ID, classOf[String])

  @Configuration(label = "$$field_awsSecretKey=Secret Access Key", order = 7)
  override def getSecretAccessKey: String =
    decodeConfigurationValue(_self.getConfiguration, CONFIG_SECRET_ACCESS_KEY, classOf[String])

  override def getCustomEndpointUrl: String =
    decodeConfigurationValue(_self.getConfiguration, CONFIG_CUSTOM_ENDPOINT_URL, classOf[String])
end CourseStructureUploadSystemImpl

object CourseStructureUploadSystem:
  final val CONFIG_USE_INSTANCE_PROFILE = "useInstanceProfile"
  final val CONFIG_USE_ASSUME_ROLE      = "useAssumeRole"
  final val CONFIG_ASSUME_ROLE_ARN      = "assumeRoleArn"
  final val CONFIG_AWS_REGION           = "awsRegion"
  final val CONFIG_AWS_BUCKET           = "awsBucket"
  final val CONFIG_KEY_PREFIX           = "prefix"
  final val CONFIG_ACCESS_KEY_ID        = "accessKeyId"
  final val CONFIG_SECRET_ACCESS_KEY    = "secretAccessKey"
  final val CONFIG_CUSTOM_ENDPOINT_URL  = "customEndpointUrl"
end CourseStructureUploadSystem

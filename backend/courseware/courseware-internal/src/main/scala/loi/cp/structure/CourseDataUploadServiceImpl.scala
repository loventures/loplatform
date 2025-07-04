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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.integration.SystemFinder
import com.learningobjects.cpxp.service.query.{Comparison, QueryService}
import scalaz.std.string.*
import scaloi.syntax.option.*
import scaloi.syntax.ʈry.*
import software.amazon.awssdk.auth.credentials.{
  AwsBasicCredentials,
  AwsSessionCredentials,
  InstanceProfileCredentialsProvider,
  StaticCredentialsProvider
}
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.{S3Client, S3Configuration}
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest

import java.net.URI
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import scala.util.Try

@Service
class CourseDataUploadServiceImpl(
  queryService: QueryService,
) extends CourseDataUploadService:
  import CourseDataUploadServiceImpl.*

  // the id of us, the vendor
  private val vendorId = "DifferenceEngine"

  // Deliberately let the user choose their provider
  private def getAssumeRoleCredentialProvider(config: CourseDataUploadConfig) =

    val stsClient = StsClient
      .builder()
      .region(Region.of(config.region))
      .credentialsProvider(
        if config.useInstanceProfile then
          InstanceProfileCredentialsProvider.builder().asyncCredentialUpdateEnabled(true).build()
        else StaticCredentialsProvider.create(AwsBasicCredentials.create(config.accessKeyId, config.secretAccessKey))
      )
      .build()

    try
      val roleResponse = stsClient
        .assumeRole(
          AssumeRoleRequest.builder.roleArn(config.assumeRoleArn).roleSessionName("CourseStructureSession").build
        )

      AwsSessionCredentials.create(
        roleResponse.credentials.accessKeyId,
        roleResponse.credentials.secretAccessKey,
        roleResponse.credentials.sessionToken
      )
    finally stsClient.close()
  end getAssumeRoleCredentialProvider

  private def getCredentialsProvider(config: CourseDataUploadConfig) =
    if config.useInstanceProfile then // AWS instance profile
      if config.useAssumeRole then StaticCredentialsProvider.create(getAssumeRoleCredentialProvider(config))
      else InstanceProfileCredentialsProvider.builder().asyncCredentialUpdateEnabled(true).build()
    else                              // In case people want this, also easier for testing
    if config.useAssumeRole then StaticCredentialsProvider.create(getAssumeRoleCredentialProvider(config))
    else StaticCredentialsProvider.create(AwsBasicCredentials.create(config.accessKeyId, config.secretAccessKey))

  // Given the properly ordained file and json, put it on S3
  private def pushDataToS3(s3Client: S3Client, config: CourseDataUploadConfig, filePath: String, json: String): Unit =
    logger.info(s"Uploading $filePath to ${config.name}...")
    val bytes = json.getBytes(StandardCharsets.UTF_8)
    s3Client.putObject(
      PutObjectRequest.builder().bucket(config.bucket).key(filePath).contentLength(bytes.length.toLong).build,
      RequestBody.fromBytes(bytes)
    )

  override def getActiveUploadConfigs: List[CourseDataUploadConfig] =
    queryService
      .queryRoot(SystemFinder.ITEM_TYPE_SYSTEM)
      .addCondition(
        SystemFinder.DATA_TYPE_COMPONENT_IDENTIFIER,
        Comparison.eq,
        classOf[CourseStructureUploadSystemImpl].getName
      )
      .getComponents[CourseStructureUploadSystem]
      .filterNot(_.getDisabled)
      .map(toConfig)
      .toList

  override def buildS3Client(config: CourseDataUploadConfig): S3Client =

    logger.info(s"building s3 client for upload configuration ${config.name}")

    val builder = S3Client.builder
      .region(Region.of(config.region))
      .credentialsProvider(getCredentialsProvider(config))

    OptionNZ(config.customEndpointUrl).foreach: endpoint =>
      builder
        .serviceConfiguration(S3Configuration.builder.chunkedEncodingEnabled(false).pathStyleAccessEnabled(true).build)
        .endpointOverride(URI.create(endpoint))

    builder.build
  end buildS3Client

  override def uploadDataToS3(data: String, courseId: String, fileName: String, timeStamp: Date)(
    s3Client: S3Client,
    config: CourseDataUploadConfig
  ): Try[Unit] =

    val prefix = config.prefix.nzMap(_.trim) match
      case Some(prefix) if prefix.endsWith("/") => prefix
      case Some(prefix)                         => prefix + "/"
      case None                                 => ""

    val key = s"$prefix$vendorId/${dateFormatter.format(timeStamp)}/$courseId/$fileName"

    Try(pushDataToS3(s3Client, config, key, data))
      .tapFailure(e => logger.error(e)(s"Error uploading to S3 ${config.name}"))
  end uploadDataToS3
end CourseDataUploadServiceImpl

object CourseDataUploadServiceImpl:
  private final val logger = org.log4s.getLogger

  private final val dateFormatter = new SimpleDateFormat("MMddyyyyHHmm")

  def toConfig(system: CourseStructureUploadSystem): CourseDataUploadConfig = new CourseDataUploadConfig(
    system.getName,
    system.getRegion,
    system.getBucket,
    Option(system.getPrefix),
    Option(system.getUseInstanceProfile).isTrue,
    Option(system.getUseAssumeRole).isTrue,
    system.getAssumeRoleArn,
    system.getAccessKeyId,
    system.getSecretAccessKey,
    system.getCustomEndpointUrl
  )
end CourseDataUploadServiceImpl

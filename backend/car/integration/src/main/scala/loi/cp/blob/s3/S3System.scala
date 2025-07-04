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

package loi.cp.blob.s3

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.util.ConfigUtils
import com.learningobjects.cpxp.component.util.ConfigUtils.decodeConfigurationValue
import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.util.ChunkingOutputStream
import loi.cp.blob.BlobStorageSystemComponent
import loi.cp.integration.AbstractSystem
import org.log4s.getLogger as logger
import scalaz.syntax.id.*
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest

import java.io.{IOException, InputStream, OutputStream}
import java.lang
import scala.jdk.CollectionConverters.*
import scala.util.{Either, Using}

@Schema("s3BucketStorage")
trait S3SystemComponent extends BlobStorageSystemComponent[S3SystemComponent]:
  @JsonProperty def getAccessKeyId: String
  @JsonProperty def getSecretAccessKey: String
  @JsonProperty def getChunkSize: lang.Integer

@Component(name = "AWS S3 Blob Storage")
class S3System extends AbstractSystem[S3SystemComponent] with S3SystemComponent:
  import S3System.*

  @Configuration(label = "$$field_awsKeyId=Access Key ID", order = 0)
  override def getAccessKeyId: String =
    decodeConfigurationValue(_self.getConfiguration, CONFIG_ACCESS_KEY_ID, classOf[String])

  @Configuration(label = "$$field_awsSecretKey=Secret Access Key", order = 1)
  override def getSecretAccessKey: String =
    decodeConfigurationValue(_self.getConfiguration, CONFIG_SECRET_ACCESS_KEY, classOf[String])

  @Configuration(label = "$$field_s3chunkSize=Chunk Size (Bytes)", order = 2)
  override def getChunkSize: Integer =
    decodeConfigurationValue(_self.getConfiguration, CONFIG_CHUNK_SIZE, classOf[lang.Integer])

  override def update(system: S3SystemComponent): S3SystemComponent =
    ConfigUtils.encodeConfiguration(
      Map(
        CONFIG_ACCESS_KEY_ID     -> system.getAccessKeyId,
        CONFIG_SECRET_ACCESS_KEY -> system.getSecretAccessKey,
        CONFIG_CHUNK_SIZE        -> system.getChunkSize
      ).asJava
    ) |> _self.setConfiguration
    super.update(system)

  override def writeTo[R](blobName: String, uiTx: (UploadInfo, R) => R)(
    block: OutputStream => R
  ): Either[IOException, R] =
    try
      Using.resource(S3Client.builder.credentialsProvider(creds).build): client =>
        Using.resource(MultipartUpload(client, _self.getName, blobName)): upload =>
          upload.open()
          val cos = ChunkingOutputStream(getChunkSize, ()): (file, _) =>
            upload.addPart(file)
          Right(block(cos))
    catch
      case e: IOException =>
        logger.error(e)(s"Failed to write $blobName to bucket ${_self.getName}")
        Left(e)

  override def readFrom[T](blobName: String)(block: InputStream => T): Either[IOException, T] =
    try
      Using.resource(S3Client.builder.credentialsProvider(creds).build): client =>
        Using.resource(client.getObject(GetObjectRequest.builder().bucket(_self.getName).key(blobName).build)): is =>
          Right(block(is))
    catch
      case e: IOException =>
        logger.error(e)(s"Failed to read $blobName from bucket ${_self.getName}")
        Left(e)

  private def creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(getAccessKeyId, getSecretAccessKey))

end S3System

object S3System:
  private final val CONFIG_SECRET_ACCESS_KEY = "secretAccessKey"
  private final val CONFIG_ACCESS_KEY_ID     = "keyId"
  private final val CONFIG_CHUNK_SIZE        = "chunkSize"

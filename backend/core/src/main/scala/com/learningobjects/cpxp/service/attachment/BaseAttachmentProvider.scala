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

package com.learningobjects.cpxp.service.attachment

import com.google.common.base.Supplier
import com.google.common.io.ByteSource
import com.learningobjects.cpxp.service.blob.BlobPutLocation
import com.learningobjects.cpxp.util.*
import com.learningobjects.de.web.MediaType
import org.jclouds.aws.domain.SessionCredentials
import org.jclouds.blobstore.domain.Blob
import org.jclouds.blobstore.{BlobStore, BlobStoreContext}
import org.jclouds.domain.{Credentials, Location}
import org.jclouds.http.HttpRequest
import org.jclouds.location.reference.LocationConstants
import org.jclouds.{Constants, ContextBuilder}
import org.slf4j.{Logger, LoggerFactory}
import scalaz.std.string.*
import scaloi.syntax.option.*
import software.amazon.awssdk.auth.credentials.*
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.{S3Client, S3Configuration}

import java.io.File
import java.net.URI
import java.time.Duration
import java.util.Properties
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import scala.util.{Failure, Try}

class BaseAttachmentProvider(
  val name: String,
  val providerType: String,
  properties: Properties,
  val container: String,
  awsRegion: String,
  pathPrefix: String
) extends AttachmentProvider:

  import BaseAttachmentProvider.*

  val identity: String   = properties.getProperty(Constants.PROPERTY_IDENTITY, "")
  val credential: String = properties.getProperty(Constants.PROPERTY_CREDENTIAL, "")

  private def awsCredentialsProvider: AwsCredentialsProvider =
    if (identity != "" && credential != "") && isS3 then
      StaticCredentialsProvider.create(AwsBasicCredentials.create(identity, credential))
    else DefaultCredentialsProvider.builder.asyncCredentialUpdateEnabled(true).build

  private def endpointConfig: Option[String] =
    OptionNZ(properties.getProperty(s"$providerType.${LocationConstants.ENDPOINT}"))

  override lazy val s3Client: S3Client =
    val builder = S3Client.builder
      .region(Region.of(awsRegion))
      .credentialsProvider(awsCredentialsProvider)
    endpointConfig.foreach: endpoint =>
      builder
        .serviceConfiguration(S3Configuration.builder.chunkedEncodingEnabled(false).pathStyleAccessEnabled(true).build)
        .endpointOverride(URI.create(endpoint))
    builder.build
  end s3Client

  lazy val presigner: S3Presigner =
    val builder = S3Presigner.builder
      .region(Region.of(awsRegion))
      .credentialsProvider(awsCredentialsProvider)
    endpointConfig.foreach: endpoint =>
      builder
        .serviceConfiguration(S3Configuration.builder.chunkedEncodingEnabled(false).pathStyleAccessEnabled(true).build)
        .endpointOverride(URI.create(endpoint))
    builder.build
  end presigner

  lazy val credentialsSupplier: Supplier[Credentials] = new Supplier[Credentials]():
    val cred = Option.when(isS3)(awsCredentialsProvider.resolveCredentials())

    override def get: Credentials =
      cred
        .collect:
          case sessionCredential: AwsSessionCredentials =>
            SessionCredentials.builder
              .accessKeyId(sessionCredential.accessKeyId)
              .secretAccessKey(sessionCredential.secretAccessKey)
              .sessionToken(sessionCredential.sessionToken)
              .build
          case basicAWSCredentials: AwsBasicCredentials =>
            new Credentials(basicAWSCredentials.accessKeyId, basicAWSCredentials.secretAccessKey)
        .getOrElse(new Credentials(identity, credential))

  private lazy val context = ContextBuilder
    .newBuilder(providerType)
    .credentialsSupplier(credentialsSupplier)
    .overrides(properties)
    .buildView(classOf[BlobStoreContext])

  lazy val blobStore: BlobStore = context.getBlobStore

  private lazy val region: Option[Location] = Option
    .apply(properties.getProperty(REGION_KEY))
    .map(region =>
      val locations = safeAttemptBlobstoreOp[mutable.Set[? <: Location]](
        blobStore.listAssignableLocations().asScala,
        S3Meta.repeatableEmpty
      )
      locations
        .find(_.getId == region)
        .getOrElse(throw new RuntimeException(s"Unknown bucket region ${properties.getProperty(REGION_KEY)}"))
    )

  def initializeContainer(): Unit =
    region match
      case Some(r) =>
        safeAttemptBlobstoreOp[Boolean](blobStore.createContainerInLocation(r, container), S3Meta.repeatableEmpty)
      case None    =>
        safeAttemptBlobstoreOp[Boolean](blobStore.createContainerInLocation(null, container), S3Meta.repeatableEmpty)

  // "aws-s3" is the artifact id of org.apache.jclouds.provider:aws-s3 and specifying that
  // artifact id is how you tell jclouds which cloud configuration you want to use.
  // One can also choose "s3", which is the artifact id of org.apache.jclouds.api:s3, but
  // as hinted by the group id, "s3" is a jclouds API, not a jclouds Provider.
  // The difference: https://jclouds.apache.org/start/concepts/
  // One could use "s3" but you would have a different jclouds configuration than "aws-s3"
  // For example, "s3" will use AWS Signature Version 2 for signing requests but "aws-s3"
  // will use AWS Signature Version 4.
  override def isS3: Boolean = "aws-s3" == providerType

  override def blobExists(path: String): Boolean =
    safeAttemptBlobstoreOp[Boolean](blobStore.blobExists(container, path), S3Meta.repeatableEmpty)

  override def getBlob(blobName: String): Blob =
    Option
      .apply(safeAttemptBlobstoreOp[Blob](blobStore.getBlob(container, blobName), S3Meta.repeatableEmpty))
      .getOrElse(throw new IllegalStateException(s"Blob $blobName not found in provider $name"))

  override def putBlob(blobName: String, source: ByteSource): Unit =
    val length = source.size
    safeAttemptBlobstoreOp[String](
      blobStore.putBlob(
        container,
        blobStore
          .blobBuilder(blobName)
          .payload(source)
          .contentLength(length)
          .build
      ),
      S3Meta.repeatableBounded(length)
    )
  end putBlob

  override def putBlob(
    path: String,
    file: File
  ): Unit =
    val length = file.length
    safeAttemptBlobstoreOp[String](
      blobStore
        .putBlob(
          container,
          blobStore
            .blobBuilder(path)
            .payload(file)
            .contentLength(length)
            .build
        ),
      S3Meta.repeatableBounded(length)
    )
  end putBlob

  // Return trailing / on domain name since S3's list-contained-objects
  // is actually list-prefixed-objects.  Very dangerous if one domain's
  // ID is the prefix of another...
  override def getNameFor(domain: String, blobName: String): String =
    s"$pathPrefix$domain${if blobName == null then "/" else s"/$blobName"}"

  /** @return
    *   a signed URL for a specified blob.
    */
  override def getDirectUrl(
    blobInfo: BlobInfo,
    method: String,
    disposition: String,
    fileName: String,
    mimeType: String,
    expires: Long,
  ): Try[String] =

    if isS3 then
      amazonS3Url(
        blobInfo = blobInfo,
        method = method,
        disposition = disposition,
        fileName = fileName,
        mimeType = mimeType,
        expires = expires,
      )
    else Failure(ProviderDoesNotSupportDirectUrl(name))
    end if
  end getDirectUrl

  private def amazonS3Url(
    blobInfo: BlobInfo,
    method: String,
    disposition: String,
    fileName: String,
    mimeType: String,
    expires: Long,
  ) =

    val maxAge = (expires - System.currentTimeMillis) / 1000L // eesh
    // TODO: Is this bogus, giving an S3 signature valid for this long?

    Try {
      val getRequest = GetObjectRequest
        .builder()
        .bucket(container)
        .key(blobInfo.getBlobName)
        .responseCacheControl(s"private,max-age=$maxAge")
        .responseContentType(mimeType)
        .responseContentDisposition(disposition)
        .build()

      val presignRequest = GetObjectPresignRequest
        .builder()
        .signatureDuration(Duration.ofHours(1))
        .getObjectRequest(getRequest)
        .build()

      val presigned = presigner.presignGetObject(presignRequest)

      logger.info("URL, {}", presigned.url)

      presigned.url.toExternalForm
    } recoverWith { case NonFatal(e) =>
      logger.warn("Encountered this", e)
      Failure(e)
    }
  end amazonS3Url

  def buildPutUrl(
    path: String,
    contentType: MediaType,
    contentLength: Long,
    expires: Long
  ): BlobPutLocation =
    if isS3 then
      val blob    = safeAttemptBlobstoreOp[Blob](
        blobStore
          .blobBuilder(path)
          .forSigning()
          .contentLength(contentLength)
          .contentType(contentType.toGuavaMediaType)
          .build(),
        S3Meta.repeatableEmpty
      )
      val request = safeAttemptBlobstoreOp[HttpRequest](
        blobStore.getContext.getSigner
          .signPutBlob(container, blob, expires),
        S3Meta.repeatableEmpty
      )
      BlobPutLocation(request.getEndpoint, name)
    else throw new RuntimeException(s"buildPutUrl() for non-s3 provider $name not supported")

  /** @param f
    *   a jclouds blobStore function
    */
  private def safeAttemptBlobstoreOp[A](
    f: => A,
    meta: S3Meta
  ): A = BlobStoreUtils.attemptBlobStoreOperation[A](() => f, meta, S3Statistics.apply(name))
end BaseAttachmentProvider

object BaseAttachmentProvider:
  private val REGION_KEY     = "region"
  private val logger: Logger = LoggerFactory.getLogger(classOf[BaseAttachmentProvider].getName)

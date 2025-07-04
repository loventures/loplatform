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

package de.changelog

import cats.effect.Sync
import cats.syntax.all.*
import de.changelog.App.{PrBranchRe, Release}
import de.common.getenv
import org.apache.commons.codec.digest.DigestUtils
import org.http4s.Headers
import org.http4s.headers.{`Content-Disposition`, `Content-Type`}
import org.http4s.implicits.http4sHeaderSyntax
import org.typelevel.ci.CIString
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.{GetUrlRequest, PutObjectRequest}
import software.amazon.awssdk.services.s3.S3Client

import java.io.{File, FileInputStream}
import java.net.URL
import java.nio.charset.StandardCharsets
import scala.util.Using

private[changelog] class S3(
  bucket: String,
  s3: S3Client,
):

  import S3.log

  def publish[F[_]: Sync](file: File, headers: Headers, branch: String, name: String): F[URL] = Sync[F].delay {
    val filename   = for
      cd <- headers.get[`Content-Disposition`]
      fn <- cd.parameters.get(CIString("filename"))
    yield fn
    val suffix     = filename.map(_.replaceAll(".*\\.", ".")).getOrElse("")
    val md5        = Using.resource(new FileInputStream(file))(DigestUtils.md5Hex)
    val prefix     = branch match
      case PrBranchRe(id) => s"pr/$id"
      case Release        => s"release/${name.replace(' ', '_')}"
      case _              => s"$branch/${name.replace(' ', '_')}"
    val key        = s"$prefix/$md5$suffix"
    log.info(s"Publishing s3:$bucket/$key")
    val putRequest = PutObjectRequest.builder.bucket(bucket).key(key).contentLength(file.length)
    headers.get[`Content-Type`].foreach(ct => putRequest.contentType(ct.value))
    headers.get[`Content-Disposition`].foreach(cd => putRequest.contentDisposition(cd.value))
    s3.putObject(putRequest.build, RequestBody.fromFile(file))
    s3.utilities.getUrl(GetUrlRequest.builder.bucket(bucket).key(key).build)
  }

  def publishIndex[F[_]: Sync](html: String, branch: String, name: String): F[URL] = Sync[F].delay {
    val prefix     = branch match
      case PrBranchRe(id) => s"pr/$id"
      case Release        => s"release/${name.replace(' ', '_')}"
      case _              => s"$branch/${name.replace(' ', '_')}"
    val key        = s"$prefix/index.html"
    log.info(s"Publishing s3:$bucket/$key")
    val bytes      = html.getBytes(StandardCharsets.UTF_8)
    val putRequest = PutObjectRequest.builder
      .bucket(bucket)
      .key(key)
      .contentType("text/html; charset=UTF-8")
      .contentLength(bytes.length.toLong)
      .build
    s3.putObject(putRequest, RequestBody.fromBytes(bytes))
    s3.utilities.getUrl(GetUrlRequest.builder.bucket(bucket).key(key).build)
  }
end S3

// morally this should be Resourced and all, and the Klient hold this too, but ffs
private[changelog] object S3:
  private final val log = org.log4s.getLogger

  val BucketEnv = "SLACK_S3_BUCKET"
  val KeyEnv    = "SLACK_S3_KEY"
  val SecretEnv = "SLACK_S3_SECRET"

  def getClient[F[_]: Sync]: F[S3] =
    for
      bucket <- getenv[F](BucketEnv)
      key    <- getenv[F](KeyEnv)
      secret <- getenv[F](SecretEnv)
      creds   = StaticCredentialsProvider.create(AwsBasicCredentials.create(key, secret))
      s3      = S3Client.builder.region(Region.US_EAST_1).credentialsProvider(creds).build
    yield new S3(bucket, s3)
end S3

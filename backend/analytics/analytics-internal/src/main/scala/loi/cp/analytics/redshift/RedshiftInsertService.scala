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

package loi.cp.analytics.redshift

import argonaut.*
import argonaut.Argonaut.*
import cats.data.{Kleisli, NonEmptyList}
import cats.syntax.applicativeError.*
import cats.syntax.functor.*
import cats.syntax.list.*
import clots.data.Cleisli
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.util.GuidUtil
import com.typesafe.config.Config
import doobie.*
import doobie.implicits.*
import loi.cp.analytics.config.AnalyticsConfig
import scalaz.std.option.*
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.{DeleteObjectRequest, PutObjectRequest}

import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

@Service
class RedshiftInsertService(
  config: Config,
  serviceMeta: ServiceMeta,
):

  implicit private val logger: org.log4s.Logger = org.log4s.getLogger

  def upsert[A <: Product: EncodeJson](
    tgtTableName: String,
    rowsA: NonEmptyList[A],
    idColNameHead: String,
    idColNameTail: String*
  ): Kleisli[ConnectionIO, DomainDTO, Unit] =

    // who dares use a case class without any parameters
    val cols = rowsA.head.productElementNames.toList.toNel.get.map(Fragment.const0(_))
    val rows = rowsA.map(_.asJson)

    val tmpTableName = tgtTableName + "_stage"
    val tgtTable     = Fragment.const0(tgtTableName)
    val tmpTable     = Fragment.const0(tmpTableName)
    val idCols       = NonEmptyList(idColNameHead, idColNameTail.toList).map(Fragment.const0(_))
    val idColsEqual  = Fragments.and(idCols.map(col => fr"$tmpTable.$col = $tgtTable.$col"))

    for
      _ <- Kleisli.liftF(sql"CREATE TEMP TABLE $tmpTable (LIKE $tgtTable)".update.run)
      _ <- copyIntoTable(tmpTableName, rows)
      _ <- Kleisli.liftF(sql"MERGE INTO $tgtTable USING $tmpTable ON $idColsEqual REMOVE DUPLICATES".update.run)
    yield ()
  end upsert

  /** Copies `rows` into `table` using Redshift's COPY from JSON format. The S3 file is automatically deleted upon copy
    * success, kept upon copy failure.
    *
    * @return
    *   the key of the deleted s3 object that was used to transfer data to redshift
    */
  def copyIntoTable(tableName: String, rows: NonEmptyList[Json]): Kleisli[ConnectionIO, DomainDTO, String] =

    implicit val analyticsConfig: AnalyticsConfig = AnalyticsConfig.fromRootConfig(config)

    val bucket = analyticsConfig.redshift.bucket
    val table  = Fragment.const0(tableName)

    for
      key <- Cleisli.fromFunction[ConnectionIO, DomainDTO](sendToS3(tableName, rows, analyticsConfig))
      _   <- Kleisli.liftF(
               fr"""copy $table
                   |from 's3://${Fragment.const0(bucket)}/${Fragment.const0(key)}'
                   |iam_role '${Fragment.const0(analyticsConfig.redshift.role)}'
                   |json 'auto'
                   |roundec""".stripMargin.update.run.void
                 .adaptErr({ case cause =>
                   new RedshiftCopyException(analyticsConfig.region, bucket, key, tableName, cause)
                 })
             )
    yield
      deleteS3Object(key)
      key
    end for
  end copyIntoTable

  // passing in domainDto because the constructor's is non-strict and won't have a value when the IO runs
  private def sendToS3(label: String, rows: NonEmptyList[Json], analyticsConfig: AnalyticsConfig)(
    domainDto: DomainDTO
  ): String =
    val rand    = GuidUtil.shortGuid()
    val time    = Instant.now()
    val prefix  = analyticsConfig.redshift.s3.prefix
    val day     = RedshiftInsertService.ObjectKeyDateFormat.format(Date.from(time))
    val cluster = serviceMeta.getCluster
    val rootId  = domainDto.id
    val key     = s"$prefix/$cluster/$rootId/$day/$label-$rand-${time.toEpochMilli}.json"

    val string = rows.foldLeft(new StringBuilder): (sb, json) =>
      sb.append(json).append('\n')
    val bytes  = string.toString.getBytes(StandardCharsets.UTF_8)

    val s3 = analyticsConfig.redshift.s3.getClient(analyticsConfig.region)
    s3.putObject(
      PutObjectRequest.builder
        .bucket(analyticsConfig.redshift.bucket)
        .key(key)
        .contentLength(bytes.length.toLong)
        .build,
      RequestBody.fromBytes(bytes)
    )

    key
  end sendToS3

  private def deleteS3Object(key: String)(implicit analyticsConfig: AnalyticsConfig): Unit =

    val s3 = analyticsConfig.redshift.s3.getClient(analyticsConfig.region)

    try s3.deleteObject(DeleteObjectRequest.builder.bucket(analyticsConfig.redshift.bucket).key(key).build)
    catch
      case e: Exception =>
        logger.warn(e)(
          s"failed to delete s3 object s3://${analyticsConfig.redshift.bucket}/$key (${analyticsConfig.region})"
        )
  end deleteS3Object
end RedshiftInsertService

object RedshiftInsertService:
  final val ObjectKeyDateFormat = new SimpleDateFormat("yyyy/MM/dd")

class RedshiftCopyException(
  val region: String,
  val bucket: String,
  val key: String,
  val destination: String,
  cause: Throwable
) extends RuntimeException(s"failed to copy s3://$bucket/$key ($region) into $destination", cause)

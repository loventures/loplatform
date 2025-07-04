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

import argonaut.Argonaut.*
import argonaut.*
import cats.data.{Kleisli, NonEmptyList}
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import doobie.*
import doobie.implicits.*

import java.sql.Timestamp

@Service
class RsEnrollmentDao(
  insertService: RedshiftInsertService
):

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  def upsert(enrollments: NonEmptyList[RsEnrollment]): Kleisli[ConnectionIO, DomainDTO, Unit] =
    val rows = enrollments.distinctBy(_.id)
    insertService.upsert("enrollment", rows, "id")

  final def upsert(
    enrollmentHead: RsEnrollment,
    enrollmentTail: RsEnrollment*
  ): Kleisli[ConnectionIO, DomainDTO, Unit] =
    upsert(NonEmptyList.of(enrollmentHead, enrollmentTail*))

  def delete(enrollmentIds: NonEmptyList[Long]): Kleisli[ConnectionIO, DomainDTO, Unit] =

    val jsons = enrollmentIds.map(id => Json("enrollmentid" := id))

    for
      _ <- Kleisli.liftF(fr"create temp table enrollmentdelete_stage(enrollmentid int8)".update.run)
      _ <- insertService.copyIntoTable("enrollmentdelete_stage", jsons)
      _ <-
        Kleisli.liftF(
          fr"delete from enrollment using enrollmentdelete_stage where enrollment.id = enrollmentdelete_stage.enrollmentid".update.run
        )
    yield ()
  end delete
end RsEnrollmentDao

/** The enrollment table in Redshift.
  */
case class RsEnrollment(
  id: Long,
  userId: Long,
  sectionId: Long,
  role: String,
  disabled: Boolean,
  startTime: Option[Timestamp],
  endTime: Option[Timestamp],
  dataSource: Option[String]
)

object RsEnrollment:

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""CREATE TABLE enrollment(
         |  id INT8 PRIMARY KEY,
         |  userid INT8 REFERENCES usr(id),
         |  sectionid INT8 REFERENCES section(id),
         |  role VARCHAR(64) NOT NULL,
         |  disabled BOOLEAN NOT NULL,
         |  starttime TIMESTAMP,
         |  endtime TIMESTAMP,
         |  datasource VARCHAR(255)
         |)""".stripMargin.update.run

  implicit val encodeJsonForRsEnrollment: EncodeJson[RsEnrollment] = EncodeJson(a =>
    Json(
      "id"         := a.id,
      "userid"     := a.userId,
      "sectionid"  := a.sectionId,
      "role"       := a.role,
      "disabled"   := a.disabled,
      "starttime"  := a.startTime.map(RedshiftSchema.RedshiftDateFormat.format),
      "endtime"    := a.endTime.map(RedshiftSchema.RedshiftDateFormat.format),
      "datasource" := a.dataSource
    )
  )
end RsEnrollment

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
class RsGradeDao(
  insertService: RedshiftInsertService
):

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  def upsert(grades: NonEmptyList[RsGrade]): Kleisli[ConnectionIO, DomainDTO, Unit] =

    val jsons = grades.distinctBy(a => (a.userId, a.sectionId, a.edgePath)).map(_.asJson)

    val tmpTableName = "grade_stage"
    val tmpTable     = Fragment.const0(tmpTableName)

    for
      _ <- Kleisli.liftF(sql"CREATE TEMP TABLE $tmpTable (LIKE grade)".update.run)
      _ <- insertService.copyIntoTable(tmpTableName, jsons)

      _ <- Kleisli.liftF(
             sql"""update grade set
                  |  pointsawarded = $tmpTable.pointsawarded,
                  |  pointspossible = $tmpTable.pointspossible,
                  |  percentage = $tmpTable.percentage,
                  |  forcredit = $tmpTable.forcredit,
                  |  time = $tmpTable.time
                  |from $tmpTable
                  |where grade.userid = $tmpTable.userid
                  |  and grade.sectionid = $tmpTable.sectionid
                  |  and grade.edgepath = $tmpTable.edgepath """.stripMargin.update.run
           )
      _ <- Kleisli.liftF(
             sql"""delete from $tmpTable using grade
                  |where $tmpTable.userid = grade.userid
                  |  and $tmpTable.sectionid = grade.sectionid
                  |  and $tmpTable.edgepath = grade.edgepath""".stripMargin.update.run
           )

      _ <- Kleisli.liftF(sql"insert into grade select * from $tmpTable".update.run)
    yield ()
    end for
  end upsert

  def delete(gradeIds: NonEmptyList[RsGrade.Key]): Kleisli[ConnectionIO, DomainDTO, Unit] =

    val jsons = gradeIds.map(_.asJson)

    for
      _ <- Kleisli.liftF(sql"""create temp table unsetgrade_stage(
                              |  userid int8,
                              |  sectionid int8,
                              |  edgepath varchar(32)
                              |)""".stripMargin.update.run)
      _ <- insertService.copyIntoTable("unsetgrade_stage", jsons)
      _ <- Kleisli.liftF(sql"""delete from grade
                              |using unsetgrade_stage
                              |where grade.userid = unsetgrade_stage.userid
                              |  and grade.sectionid = unsetgrade_stage.sectionid
                              |  and grade.edgepath = unsetgrade_stage.edgepath""".stripMargin.update.run)
    yield {}
    end for
  end delete
end RsGradeDao

/** The grade table in Redshift. This is a fact table that measures grades.
  */
case class RsGrade(
  userId: Long,
  sectionId: Long,
  edgePath: String,
  assetId: Long,
  time: Timestamp,
  forCredit: Boolean,
  pointsAwarded: BigDecimal,
  pointsPossible: BigDecimal,
  percentage: BigDecimal
):
  lazy val key: RsGrade.Key = RsGrade.Key(userId, sectionId, edgePath)
end RsGrade

object RsGrade:

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""CREATE TABLE grade(
         |  userid INT8 REFERENCES usr(id),
         |  sectionid INT8 REFERENCES section(id),
         |  edgepath VARCHAR(32),
         |  assetid INT8 NOT NULL REFERENCES asset(id),
         |  time TIMESTAMP NOT NULL,
         |  forcredit BOOLEAN NOT NULL,
         |  pointsawarded DECIMAL(18,2) NOT NULL,
         |  pointspossible DECIMAL(18,2) NOT NULL,
         |  percentage DECIMAL(18,2) NOT NULL,
         |  PRIMARY KEY (userid, sectionid, edgepath)
         |)
         |""".stripMargin.update.run

  implicit val encodeJsonForRsGrade: EncodeJson[RsGrade] = EncodeJson(a =>
    Json(
      "userid"         := a.userId,
      "sectionid"      := a.sectionId,
      "edgepath"       := a.edgePath,
      "assetid"        := a.assetId,
      "time"           := RedshiftSchema.RedshiftDateFormat.format(a.time),
      "forcredit"      := a.forCredit,
      "pointsawarded"  := a.pointsAwarded,
      "pointspossible" := a.pointsPossible,
      "percentage"     := a.percentage,
    )
  )

  /** The primary key of [[RsGrade]], useful for deleting rows from the grade table.
    */
  case class Key(userId: Long, sectionId: Long, edgePath: String)

  object Key:
    implicit val encodeJsonForRsGradeKey: EncodeJson[Key] = EncodeJson(a =>
      Json(
        "userid"    := a.userId,
        "sectionid" := a.sectionId,
        "edgepath"  := a.edgePath
      )
    )
end RsGrade

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
import argonaut.{EncodeJson, *}
import cats.data.{Kleisli, NonEmptyList}
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import doobie.*
import doobie.implicits.*

import java.sql.Timestamp

@Service
class RsAttemptDao(
  insertService: RedshiftInsertService
):

  def upsert(attempts: NonEmptyList[RsAttempt]): Kleisli[ConnectionIO, DomainDTO, Unit] =
    val rows = attempts.distinctBy(_.id)
    insertService.upsert("attempt", rows, "id")

  def upsert(attemptHead: RsAttempt, attemptTail: RsAttempt*): Kleisli[ConnectionIO, DomainDTO, Unit] =
    upsert(NonEmptyList.of(attemptHead, attemptTail*))
end RsAttemptDao

/** The attempt table in Redshift. */
case class RsAttempt(
  id: Long,
  userId: Long,
  sectionId: Long,
  edgePath: String,
  assetId: Long,
  state: String,
  valid: Boolean,
  manualScore: Boolean,
  createTime: Timestamp,
  submitTime: Option[Timestamp],
  scoreTime: Option[Timestamp],
  scorePointsAwarded: Option[BigDecimal],
  scorePointsPossible: Option[BigDecimal],
  scorePercentage: Option[BigDecimal],
  scorerUserId: Option[Long],
  timeToScoreHours: Option[Int],
  timeToScoreDays: Option[Int],
  updateTime: Option[Timestamp], // only Option because it was added later
  maxMinutes: Option[Int],
  autoSubmitted: Option[Boolean],
)

object RsAttempt:

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""CREATE TABLE attempt(
         |  id INT8 PRIMARY KEY,
         |  userid INT8 NOT NULL REFERENCES usr(id),
         |  sectionid INT8 NOT NULL REFERENCES section(id),
         |  edgepath VARCHAR(32) NOT NULL,
         |  assetid INT8 NOT NULL REFERENCES asset(id),
         |  state VARCHAR(32) NOT NULL,
         |  valid BOOLEAN NOT NULL,
         |  manualscore BOOLEAN NOT NULL,
         |  createtime TIMESTAMP NOT NULL,
         |  submittime TIMESTAMP,
         |  scoretime TIMESTAMP,
         |  scorepointsawarded DECIMAL(18,2),
         |  scorepointspossible DECIMAL(18,2),
         |  scorepercentage DECIMAL(18,2),
         |  scoreruserid INT8 REFERENCES usr(id),
         |  timetoscorehours INT,
         |  timetoscoredays INT,
         |  updatetime TIMESTAMP,
         |  maxminutes INT,
         |  autosubmitted BOOLEAN
         |)""".stripMargin.update.run

  implicit val encodeJsonForRsAttempt: EncodeJson[RsAttempt] = EncodeJson(a =>
    Json(
      "id"                  := a.id,
      "userid"              := a.userId,
      "sectionid"           := a.sectionId,
      "edgepath"            := a.edgePath,
      "assetid"             := a.assetId,
      "state"               := a.state,
      "valid"               := a.valid,
      "manualscore"         := a.manualScore,
      "createtime"          := RedshiftSchema.RedshiftDateFormat.format(a.createTime),
      "submittime"          := a.submitTime.map(RedshiftSchema.RedshiftDateFormat.format),
      "scoretime"           := a.scoreTime.map(RedshiftSchema.RedshiftDateFormat.format),
      "scorepointsawarded"  := a.scorePointsAwarded,
      "scorepointspossible" := a.scorePointsPossible,
      "scorepercentage"     := a.scorePercentage,
      "scoreruserid"        := a.scorerUserId,
      "timetoscorehours"    := a.timeToScoreHours,
      "timetoscoredays"     := a.timeToScoreDays,
      "updatetime"          := a.updateTime.map(RedshiftSchema.RedshiftDateFormat.format),
      "maxminutes"          := a.maxMinutes,
      "autosubmitted"       := a.autoSubmitted
    )
  )
end RsAttempt

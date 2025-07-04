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
import cats.syntax.functor.*
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import doobie.*
import doobie.implicits.*

import java.sql.Timestamp

@Service
class RsProgressOverTimeDao(
  insertService: RedshiftInsertService
):

  def insertAll(progressValues: NonEmptyList[RsProgressOverTime]): Kleisli[ConnectionIO, DomainDTO, Unit] =
    insertService.copyIntoTable("progressovertime", progressValues.map(_.asJson)).void

// the only difference between progress-over-time and plain progress is granularity
// progress-over-time is historical, immutable, granularity is section-user-edgepath-time
// progress is current state, mutable, granularity is section-user-edgepath
// Skipped is only optional because it was added later.
final case class RsProgressOverTime(
  sectionId: Long,
  userId: Long,
  edgePath: String,
  assetId: Long,
  time: Timestamp,
  completions: Int,
  total: Int,
  visited: Int,
  testedOut: Int,
  skipped: Option[Int],
  forCreditGrades: Option[Int],
  forCreditGradesPossible: Option[Int],
  percentage: BigDecimal,
):
  // is inheritance for avoiding this
  val rsProgress: RsProgress = RsProgress(
    sectionId,
    userId,
    edgePath,
    assetId,
    completions,
    total,
    visited,
    testedOut,
    skipped,
    forCreditGrades,
    forCreditGradesPossible,
    Some(percentage),
    Some(time)
  )
end RsProgressOverTime

object RsProgressOverTime:

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""CREATE TABLE progressovertime(
         |  sectionid INT8 NOT NULL REFERENCES section(id),
         |  userid INT8 NOT NULL REFERENCES usr(id),
         |  edgepath VARCHAR(32) NOT NULL,
         |  assetid INT8 NOT NULL REFERENCES asset(id),
         |  time TIMESTAMP SORTKEY NOT NULL,
         |  completions INT4 NOT NULL,
         |  total INT4 NOT NULL,
         |  visited INT4 NOT NULL,
         |  testedout INT4 NOT NULL,
         |  skipped INT4 NOT NULL,
         |  forcreditgrades INT4 NOT NULL,
         |  forcreditgradespossible INT4 NOT NULL,
         |  percentage DECIMAL(18, 2) NOT NULL
         |)""".stripMargin.update.run

  implicit val encodeJsonForRsProgressOverTime: EncodeJson[RsProgressOverTime] = EncodeJson(a =>
    Json(
      "sectionid"               := a.sectionId,
      "userid"                  := a.userId,
      "edgepath"                := a.edgePath,
      "assetid"                 := a.assetId,
      "time"                    := RedshiftSchema.RedshiftDateFormat.format(a.time),
      "completions"             := a.completions,
      "total"                   := a.total,
      "visited"                 := a.visited,
      "testedout"               := a.testedOut,
      "skipped"                 := a.skipped,
      "forcreditgrades"         := a.forCreditGrades,
      "forcreditgradespossible" := a.forCreditGradesPossible,
      "percentage"              := a.percentage,
    )
  )
end RsProgressOverTime

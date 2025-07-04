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
class RsProgressDao(
  insertService: RedshiftInsertService
):

  def upsert(progressValues: NonEmptyList[RsProgress]): Kleisli[ConnectionIO, DomainDTO, Unit] =
    val rows = progressValues.distinctBy(p => (p.sectionId, p.userId, p.edgePath))
    insertService.upsert("progress", rows, "sectionid", "userid", "edgepath")

/** The progress table in Redshift */
// see also RsProgressOverTime
// percentage, updateTime, and skipped are only Option because they were added after
// percentage is never null owing to a backfill
final case class RsProgress(
  sectionId: Long,
  userId: Long,
  edgePath: String,
  assetId: Long,
  completions: Int,
  total: Int,
  visited: Int,
  testedOut: Int,
  skipped: Option[Int],
  forCreditGrades: Option[Int],
  forCreditGradesPossible: Option[Int],
  percentage: Option[BigDecimal],
  updateTime: Option[Timestamp]
)

object RsProgress:

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""CREATE TABLE progress(
         |  sectionid INT8 NOT NULL REFERENCES section(id),
         |  userid INT8 NOT NULL REFERENCES usr(id),
         |  edgepath VARCHAR(32) NOT NULL DISTKEY,
         |  PRIMARY KEY(sectionid, userid, edgepath),
         |  assetid INT8 NOT NULL REFERENCES asset(id),
         |  completions INT4 NOT NULL,
         |  total INT4 NOT NULL,
         |  visited INT4 NOT NULL,
         |  testedout INT4 NOT NULL,
         |  skipped INT4 NOT NULL,
         |  forcreditgrades INT4 NOT NULL,
         |  forcreditgradespossible INT4 NOT NULL,
         |  percentage DECIMAL(18, 2),
         |  updatetime TIMESTAMP
         |)""".stripMargin.update.run

  implicit val encodeJsonForRsProgress: EncodeJson[RsProgress] = EncodeJson(a =>
    Json(
      "sectionid"               := a.sectionId,
      "userid"                  := a.userId,
      "edgepath"                := a.edgePath,
      "assetid"                 := a.assetId,
      "completions"             := a.completions,
      "total"                   := a.total,
      "visited"                 := a.visited,
      "testedout"               := a.testedOut,
      "skipped"                 := a.skipped,
      "forcreditgrades"         := a.forCreditGrades,
      "forcreditgradespossible" := a.forCreditGradesPossible,
      "percentage"              := a.percentage,
      "updatetime"              := a.updateTime.map(RedshiftSchema.RedshiftDateFormat.format),
    )
  )
end RsProgress

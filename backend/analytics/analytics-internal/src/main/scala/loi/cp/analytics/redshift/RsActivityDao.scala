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

import argonaut.JsonIdentity.*
import argonaut.StringWrap.*
import argonaut.{EncodeJson, Json}
import cats.data.{Kleisli, NonEmptyList}
import clots.data.Cleisli
import com.learningobjects.cpxp.component.annotation.Service
import doobie.ConnectionIO
import doobie.syntax.string.*

import java.sql.Timestamp
import java.util.UUID

/** Each row of the activity table is one change to a grade, attempt, or progress for a section-user-edgepath. It is
  * actually quite scant on facts. I can think of dozens of more columns to put on it. But right now its sole purpose is
  * to enable a subscription to said changes. The subscriber is left to discover what the specific activity was once
  * they are told something happened.
  *
  * The `id` and `etlTime` columns are to assist the subscription implementation. They are not facts about the activity.
  * For example, `etlTime` is the start time of the ETL execution that inserted the activity row and thousands of other
  * rows will have the same value. It is not the time of the activity. The `id` is not the analyticeventfinder id. Many
  * analyticeventfinders may be present in one ETL execution for the same section-user-edgepath and only one activity
  * row is written.
  */
@Service
class RsActivityDao(
  insertService: RedshiftInsertService
):

  def insertAll(keys: Set[ContentKey]): Kleisli[ConnectionIO, EtlEnv, Unit] =

    // the Kleisli and the NonEmptyList add much boilerplate
    if keys.isEmpty then Kleisli.pure[ConnectionIO, EtlEnv, Unit](())
    else
      for
        keysNel <- Cleisli.fromFunction[ConnectionIO, EtlEnv](buildActivities(keys))
        // ought to check that the ids are not already present in the table but meh
        _       <- insertService.copyIntoTable("activity", keysNel).local[EtlEnv](_.domainDto)
      yield ()

  private def buildActivities(keys: Set[ContentKey])(env: EtlEnv): NonEmptyList[Json] =
    NonEmptyList.fromListUnsafe(
      keys.view
        .map(k => RsActivity(UUID.randomUUID.toString, env.timeSource.timestamp, k.sectionId, k.userId, k.edgePath))
        .map(_.asJson)
        .toList
    )
end RsActivityDao

case class RsActivity(id: String, etlTime: Timestamp, sectionId: Long, userId: Long, edgePath: String)

object RsActivity:
  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  // reminder: primary key has little functionality in Redshift, only hints to query planner, no constraints enforced
  val createTable: ConnectionIO[Int] =
    sql"""CREATE TABLE activity(
         |  id VARCHAR(36) PRIMARY KEY,
         |  etltime TIMESTAMP SORTKEY NOT NULL,
         |  sectionid INT8 NOT NULL REFERENCES section(id),
         |  userid INT8 NOT NULL REFERENCES usr(id),
         |  edgepath VARCHAR(32) NOT NULL
         |)""".stripMargin.update.run

  implicit val encodeJsonForRsActivity: EncodeJson[RsActivity] = EncodeJson(a =>
    Json(
      "id"        := a.id,
      "etltime"   := RedshiftSchema.RedshiftDateFormat.format(a.etlTime),
      "sectionid" := a.sectionId,
      "userid"    := a.userId,
      "edgepath"  := a.edgePath,
    )
  )
end RsActivity

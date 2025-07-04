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
class RsTimeSpentDiscreteDao(
  insertService: RedshiftInsertService
):

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  def insertAll(timeSpents: NonEmptyList[RsTimeSpentDiscrete]): Kleisli[ConnectionIO, DomainDTO, Unit] =
    insertService.copyIntoTable("timespentdiscrete", timeSpents.map(_.asJson)).void

  def refreshTimeSpentView(): ConnectionIO[Unit] = sql"refresh materialized view timespent".update.run.void
end RsTimeSpentDiscreteDao

/** The timespentdiscrete table in Redshift. This is a fact table that measures time spent for each discrete
  * interaction. This differs from the timespent table in that timespentdiscrete can have multiple rows per edgepath.
  * Every time a user interacts and generates a TimeSpentEvent, a row is also created here.
  */
case class RsTimeSpentDiscrete(
  userId: Long,
  edgePath: String,
  sectionId: Long,
  endTime: Timestamp,
  duration: Long, // milliseconds
  assetId: Long,
  eventId: String,
  originSectionId: Option[Long],
)

object RsTimeSpentDiscrete:

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""
         |CREATE TABLE timespentdiscrete(
         |  userid INT8 REFERENCES usr(id),
         |  edgepath VARCHAR(32),
         |  sectionid INT8 REFERENCES section(id),
         |  endtime TIMESTAMP SORTKEY NOT NULL,
         |  duration INT8 NOT NULL,
         |  assetid INT8 NOT NULL REFERENCES asset(id),
         |  eventid VARCHAR(36) NOT NULL,
         |  originsectionid INT8 REFERENCES section(id)
         |)""".stripMargin.update.run

  implicit val codecJsonForRsTimeSpentDiscrete: EncodeJson[RsTimeSpentDiscrete] = EncodeJson(a =>
    Json(
      "userid"          := a.userId,
      "edgepath"        := a.edgePath,
      "sectionid"       := a.sectionId,
      "endtime"         := RedshiftSchema.RedshiftDateFormat.format(a.endTime),
      "duration"        := a.duration,
      "assetid"         := a.assetId,
      "eventid"         := a.eventId,
      "originsectionid" := a.originSectionId,
    )
  )
end RsTimeSpentDiscrete

object RsTimeSpent:

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  val createView: ConnectionIO[Int] =
    sql"""CREATE MATERIALIZED VIEW timespent AUTO REFRESH NO AS (
         |  SELECT userid, edgepath, sectionid, SUM(duration) AS duration, MAX(assetid) AS assetid
         |  FROM timespentdiscrete
         |  GROUP BY userid, edgepath, sectionid
         |)""".stripMargin.update.run
end RsTimeSpent

object RsTimeSpentModule:

  // this is a slow query, we should store this instead of computing but.. lazy
  val createView: ConnectionIO[Int] =
    sql"""CREATE VIEW timespentmodule AS(
         |WITH timespent_module_without_moduleassetid AS (
         |  SELECT sectionid, userid, moduleedgepath, sum(duration) AS duration
         |  FROM timespentdiscrete
         |  JOIN sectioncontent sc USING(sectionid,edgepath)
         |  WHERE endtime > '2022-01-01'
         |  GROUP BY sectionid,userid,moduleedgepath
         |)
         |SELECT tsd.sectionid, tsd.userid, tsd.moduleedgepath, sc.assetid AS moduleassetid, tsd.duration
         |FROM timespent_module_without_moduleassetid tsd
         |JOIN sectioncontent sc ON sc.sectionid = tsd.sectionid AND sc.edgepath = tsd.moduleedgepath
       )""".stripMargin.update.run
end RsTimeSpentModule

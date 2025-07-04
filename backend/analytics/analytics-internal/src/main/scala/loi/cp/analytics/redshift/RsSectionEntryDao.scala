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

import java.sql.Timestamp

import argonaut.Argonaut.*
import argonaut.*
import cats.data.{Kleisli, NonEmptyList}
import cats.syntax.functor.*
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import doobie.*
import doobie.implicits.*

@Service
class RsSectionEntryDao(
  insertService: RedshiftInsertService
):

  def insertAll(sectionEntries: NonEmptyList[RsSectionEntry]): Kleisli[ConnectionIO, DomainDTO, Unit] =
    insertService.copyIntoTable("sectionentry", sectionEntries.map(_.asJson)).void

  final def insertAll(entryHead: RsSectionEntry, entryTail: RsSectionEntry*): Kleisli[ConnectionIO, DomainDTO, Unit] =
    insertAll(NonEmptyList.of(entryHead, entryTail*))
end RsSectionEntryDao

/** The sectionentry table in Redshift. This is a fact table that measures section entries.
  */
case class RsSectionEntry(
  userId: Long,
  sectionId: Long,
  time: Timestamp,
  sessionId: Option[Long],
  role: Option[String],
  originSectionId: Option[Long],
)

object RsSectionEntry:

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""
         |CREATE TABLE sectionentry(
         |  userid INT8 REFERENCES usr(id),
         |  sectionid INT8 REFERENCES section(id),
         |  time TIMESTAMP NOT NULL,
         |  sessionid INT8 REFERENCES session(id),
         |  role VARCHAR(64),
         |  originsectionid INT8 REFERENCES section(id)
         |)""".stripMargin.update.run

  implicit val encodeJsonForRsSectionEntry: EncodeJson[RsSectionEntry] = EncodeJson(a =>
    Json(
      "userid"          := a.userId,
      "sectionid"       := a.sectionId,
      "time"            := RedshiftSchema.RedshiftDateFormat.format(a.time),
      "sessionid"       := a.sessionId,
      "role"            := a.role,
      "originsectionid" := a.originSectionId,
    )
  )
end RsSectionEntry

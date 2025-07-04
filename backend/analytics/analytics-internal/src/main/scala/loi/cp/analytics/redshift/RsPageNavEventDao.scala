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
class RsPageNavEventDao(
  insertService: RedshiftInsertService
):

  def insertAll(pageNavEvents: NonEmptyList[RsPageNavEvent]): Kleisli[ConnectionIO, DomainDTO, Unit] =
    insertService.copyIntoTable("pagenavevent", pageNavEvents.map(_.asJson)).void

  def insertAll(eventHead: RsPageNavEvent, eventTail: RsPageNavEvent*): Kleisli[ConnectionIO, DomainDTO, Unit] =
    insertAll(NonEmptyList.of(eventHead, eventTail*))
end RsPageNavEventDao

/** The pagenavevent table in Redshift */
// assetId is lacking because I reused an existing event that lacked it, PageNavEvent
final case class RsPageNavEvent(
  id: String,
  time: Timestamp,
  source: Option[String],
  sessionId: Long,
  userId: Long,
  userExtId: Option[String],
  url: String,
  pageTitle: Option[String],
  sectionId: Option[Long],
  sectionExtId: Option[String],
  edgePath: Option[String],
  assetTypeId: Option[String],
  assetTitle: Option[String],
  er: Option[Boolean],
)

object RsPageNavEvent:

  implicit private val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""CREATE TABLE pagenavevent(
         |  id VARCHAR(36) NOT NULL,
         |  time TIMESTAMP NOT NULL,
         |  source VARCHAR(1024),
         |  sessionid INT8 NOT NULL,
         |  userid INT8 NOT NULL,
         |  userextid VARCHAR(256),
         |  url VARCHAR(1024) NOT NULL,
         |  pagetitle VARCHAR(1024),
         |  sectionid INT8,
         |  sectionextid VARCHAR(256),
         |  edgepath VARCHAR(32),
         |  assettypeid VARCHAR(255),
         |  assettitle VARCHAR(255),
         |  er BOOLEAN
         |)""".stripMargin.update.run

  implicit val encodeJsonForRsPageNavEvent: EncodeJson[RsPageNavEvent] = EncodeJson(a =>
    Json(
      "id"           := a.id,
      "time"         := RedshiftSchema.RedshiftDateFormat.format(a.time),
      "source"       := a.source,
      "sessionid"    := a.sessionId,
      "userid"       := a.userId,
      "userextid"    := a.userExtId,
      "url"          := a.url,
      "pagetitle"    := a.pageTitle,
      "sectionid"    := a.sectionId,
      "sectionextid" := a.sectionExtId,
      "edgepath"     := a.edgePath,
      "assettypeid"  := a.assetTypeId,
      "assettitle"   := a.assetTitle,
      "er"           := a.er,
    )
  )
end RsPageNavEvent

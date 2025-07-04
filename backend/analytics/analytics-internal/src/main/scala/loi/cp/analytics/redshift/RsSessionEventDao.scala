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
class RsSessionEventDao(
  insertService: RedshiftInsertService
):

  def insertAll(sessionEvents: NonEmptyList[RsSessionEvent]): Kleisli[ConnectionIO, DomainDTO, Unit] =
    insertService.copyIntoTable("sessionevent", sessionEvents.map(_.asJson)).void

/** The session event table in Redshift
  */
final case class RsSessionEvent(
  id: String,
  time: Timestamp,
  source: Option[String],
  sessionId: Option[Long],
  actionType: String,
  userId: Long,
  userExtId: Option[String],
  requestUrl: Option[String],
  ipAddress: Option[String],
  referrer: Option[String],
  acceptLanguage: Option[String],
  userAgent: Option[String],
  authMethod: Option[String],
  lastActive: Option[Timestamp],
  becameActive: Option[Timestamp]
)

object RsSessionEvent:

  implicit private val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""CREATE TABLE sessionevent(
         |  id VARCHAR(36) NOT NULL,
         |  time TIMESTAMP NOT NULL,
         |  source VARCHAR(1024),
         |  sessionid INT8,
         |  actiontype VARCHAR(256) NOT NULL,
         |  userid INT8 NOT NULL,
         |  userextid VARCHAR(256),
         |  requesturl VARCHAR(1024),
         |  ipaddress VARCHAR(256),
         |  referrer VARCHAR(1024),
         |  acceptlanguage VARCHAR(2048),
         |  useragent VARCHAR(1024),
         |  authmethod VARCHAR(256),
         |  lastactive TIMESTAMP,
         |  becameactive TIMESTAMP
         |)""".stripMargin.update.run

  implicit val encodeJsonForRsSessionEvent: EncodeJson[RsSessionEvent] = EncodeJson(a =>
    Json(
      "id"             := a.id,
      "time"           := RedshiftSchema.RedshiftDateFormat.format(a.time),
      "source"         := a.source,
      "sessionid"      := a.sessionId,
      "actiontype"     := a.actionType,
      "userid"         := a.userId,
      "userextid"      := a.userExtId,
      "requesturl"     := a.requestUrl,
      "ipaddress"      := a.ipAddress,
      "referrer"       := a.referrer,
      "acceptlanguage" := a.acceptLanguage,
      "useragent"      := a.userAgent,
      "authmethod"     := a.authMethod,
      "lastactive"     := a.lastActive.map(RedshiftSchema.RedshiftDateFormat.format),
      "becameactive"   := a.becameActive.map(RedshiftSchema.RedshiftDateFormat.format)
    )
  )
end RsSessionEvent

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
class RsSessionDao(
  insertService: RedshiftInsertService
):

  def insertNew(sessions: NonEmptyList[RsSession]): Kleisli[ConnectionIO, DomainDTO, Unit] =

    val jsons = sessions.distinctBy(_.id).map(_.asJson)

    for
      _ <- Kleisli.liftF(sql"CREATE TEMP TABLE session_stage (LIKE session)".update.run)
      _ <- insertService.copyIntoTable("session_stage", jsons)
      _ <- Kleisli.liftF(sql"DELETE FROM session_stage USING session WHERE session_stage.id = session.id".update.run)
      _ <- Kleisli.liftF(sql"INSERT INTO session SELECT * FROM session_stage".update.run)
    yield ()
  end insertNew
end RsSessionDao

/** The session table in Redshift. This is a dimension table.
  */
final case class RsSession(
  id: Long,
  start: Timestamp,
  userId: Long,
  userExtId: Option[String],
  requestUrl: Option[String],
  ipAddress: Option[String],
  referrer: Option[String],
  acceptLanguage: Option[String],
  userAgent: Option[String],
  authMethod: Option[String]
)

object RsSession:

  implicit private val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""CREATE TABLE session(
         |  id INT8 PRIMARY KEY,
         |  start TIMESTAMP NOT NULL,
         |  userid INT8 NOT NULL,
         |  userextid VARCHAR(256),
         |  requesturl VARCHAR(1024),
         |  ipaddress VARCHAR(256),
         |  referrer VARCHAR(1024),
         |  acceptlanguage VARCHAR(2048),
         |  useragent VARCHAR(1024),
         |  authmethod VARCHAR(256)
         |)""".stripMargin.update.run

  implicit val encodeJsonForRsSession: EncodeJson[RsSession] = EncodeJson(a =>
    Json(
      "id"             := a.id,
      "start"          := RedshiftSchema.RedshiftDateFormat.format(a.start),
      "userid"         := a.userId,
      "userextid"      := a.userExtId,
      "requesturl"     := a.requestUrl,
      "ipaddress"      := a.ipAddress,
      "referrer"       := a.referrer,
      "acceptlanguage" := a.acceptLanguage,
      "useragent"      := a.userAgent,
      "authmethod"     := a.authMethod,
    )
  )
end RsSession

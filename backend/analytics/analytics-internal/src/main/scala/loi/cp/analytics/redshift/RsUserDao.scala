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

@Service
class RsUserDao(
  insertService: RedshiftInsertService
):

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  def upsert(users: NonEmptyList[RsUser]): Kleisli[ConnectionIO, DomainDTO, Unit] =
    val rows = users.distinctBy(_.id)
    insertService.upsert("usr", rows, "id")

  def obfuscate(userIds: NonEmptyList[Long]): Kleisli[ConnectionIO, DomainDTO, Unit] =
    val jsons = userIds.map(id => Json("usrid" := id))
    for
      _ <- Kleisli.liftF(fr"CREATE TEMP TABLE usrobfuscate_stage(usrid INT8)".update.run)
      _ <- insertService.copyIntoTable("usrobfuscate_stage", jsons)
      _ <-
        Kleisli.liftF(
          fr"""UPDATE usr
              |SET externalid = NULL, email = NULL, username = NULL, emaildomain = NULL, givenname = NULL,
              |  familyname = NULL, fullname = NULL, subtenantid = NULL, subtenantname = NULL, integration = NULL,
              |  uniqueid = NULL
              |FROM usrobfuscate_stage
              |WHERE usr.id = usrobfuscate_stage.usrid""".stripMargin.update.run
        )
    yield ()
    end for
  end obfuscate

  final def upsert(userHead: RsUser, userTail: RsUser*): Kleisli[ConnectionIO, DomainDTO, Unit] =
    upsert(NonEmptyList.of(userHead, userTail*))
end RsUserDao

/** The usr table in Redshift
  */
case class RsUser(
  id: Long,
  externalId: Option[String],
  email: Option[String],
  userName: Option[String],
  emailDomain: Option[String],
  givenName: Option[String],
  familyName: Option[String],
  fullName: Option[String],
  subtenantId: Option[String],
  subtenantName: Option[String],
  integration: Option[String],
  uniqueId: Option[String],
)

object RsUser:

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""
         |CREATE TABLE usr(
         |  id INT8 PRIMARY KEY,
         |  externalid VARCHAR(255),
         |  email VARCHAR(255),
         |  username VARCHAR(255),
         |  emaildomain VARCHAR(255),
         |  givenname VARCHAR(255),
         |  familyname VARCHAR(255),
         |  fullname VARCHAR(255),
         |  subtenantid VARCHAR(255),
         |  subtenantname VARCHAR(255),
         |  integration VARCHAR(255),
         |  uniqueid VARCHAR(255)
         |)
         |""".stripMargin.update.run

  implicit val encodeJsonForRsUser: EncodeJson[RsUser] = EncodeJson(a =>
    Json(
      "id"            := a.id,
      "externalid"    := a.externalId,
      "email"         := a.email,
      "username"      := a.userName,
      "emaildomain"   := a.emailDomain,
      "givenname"     := a.givenName,
      "familyname"    := a.familyName,
      "fullname"      := a.fullName,
      "subtenantid"   := a.subtenantId,
      "subtenantname" := a.subtenantName,
      "integration"   := a.integration,
      "uniqueid"      := a.uniqueId
    )
  )
end RsUser

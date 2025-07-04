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
class RsSectionDao(
  insertService: RedshiftInsertService
):

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  /** Updates or inserts `sections` based on presence of the section id in the table.
    */
  def upsert(sections: NonEmptyList[RsSection]): Kleisli[ConnectionIO, DomainDTO, Unit] =
    val rows = sections.distinctBy(_.id)
    insertService.upsert("section", rows, "id")

  final def upsert(sectionHead: RsSection, sectionTail: RsSection*): Kleisli[ConnectionIO, DomainDTO, Unit] =
    upsert(NonEmptyList.of(sectionHead, sectionTail*))

  def delete(sectionIds: NonEmptyList[Long]): Kleisli[ConnectionIO, DomainDTO, Unit] =

    // doing the whole s3 dance feels very overkill for something that will never happen
    val jsons = sectionIds.map(id => Json("sectionid" := id))

    for
      _ <- Kleisli.liftF(fr"create temp table sectiondelete_stage(sectionid int8)".update.run)
      _ <- insertService.copyIntoTable("sectiondelete_stage", jsons)
      _ <-
        Kleisli.liftF(
          fr"delete from section using sectiondelete_stage where section.id = sectiondelete_stage.sectionid".update.run
        )
    yield {}
  end delete
end RsSectionDao

/** The section table in Redshift
  */
case class RsSection(
  id: Long,
  externalId: Option[String],
  name: Option[String],
  integrationUniqueId: Option[String],
  integrationSystemId: Option[Long],
  integrationSystemIdentifier: Option[String],
  integrationSystemName: Option[String],
  offeringId: Option[Long],
  offeringGroupId: Option[String],
  offeringName: Option[String],
  disabled: Option[Boolean],
  startTime: Option[Timestamp],
  endTime: Option[Timestamp],
  groupId: Option[String],
)

object RsSection:

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""
         |CREATE TABLE section(
         |  id INT8 PRIMARY KEY,
         |  externalid VARCHAR(255),
         |  name VARCHAR(255),
         |  integrationuniqueid VARCHAR(255),
         |  integrationsystemid INT8,
         |  integrationsystemidentifier VARCHAR(255),
         |  integrationsystemname VARCHAR(255),
         |  offeringid INT8,
         |  offeringgroupid VARCHAR(255),
         |  offeringname VARCHAR(255),
         |  disabled BOOLEAN,
         |  starttime TIMESTAMP,
         |  endtime TIMESTAMP,
         |  groupid VARCHAR(255)
         |)""".stripMargin.update.run

  implicit val encodeJsonForRsSection: EncodeJson[RsSection] = EncodeJson(a =>
    Json(
      "id"                          := a.id,
      "externalid"                  := a.externalId,
      "name"                        := a.name,
      "integrationuniqueid"         := a.integrationUniqueId,
      "integrationsystemid"         := a.integrationSystemId,
      "integrationsystemidentifier" := a.integrationSystemIdentifier,
      "integrationsystemname"       := a.integrationSystemName,
      "offeringid"                  := a.offeringId,
      "offeringgroupid"             := a.offeringGroupId,
      "offeringname"                := a.offeringName,
      "disabled"                    := a.disabled,
      "starttime"                   := a.startTime.map(RedshiftSchema.RedshiftDateFormat.format),
      "endtime"                     := a.endTime.map(RedshiftSchema.RedshiftDateFormat.format),
      "groupid"                     := a.groupId,
    )
  )
end RsSection

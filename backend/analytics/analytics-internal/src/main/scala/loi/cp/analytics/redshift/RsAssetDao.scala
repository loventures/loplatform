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
class RsAssetDao(
  insertService: RedshiftInsertService
):

  def insertNew(assets: NonEmptyList[RsAsset]): Kleisli[ConnectionIO, DomainDTO, Unit] =

    val jsons = assets.distinctBy(_.id).map(_.asJson)

    for
      _ <- Kleisli.liftF(sql"CREATE TEMP TABLE asset_stage (LIKE asset)".update.run)
      _ <- insertService.copyIntoTable("asset_stage", jsons)
      _ <- Kleisli.liftF(sql"DELETE FROM asset_stage USING asset WHERE asset_stage.id = asset.id".update.run)
      _ <- Kleisli.liftF(sql"INSERT INTO asset SELECT * FROM asset_stage".update.run)
    yield ()
  end insertNew
end RsAssetDao

/** The asset table in Redshift. This is a dimension table.
  */
case class RsAsset(
  id: Long,
  name: String,
  typeId: String,
  title: Option[String],
  keywords: Option[String],
  forCredit: Option[Boolean],
  pointsPossible: Option[BigDecimal]
)

object RsAsset:

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""CREATE TABLE asset(
         |  id INT8 PRIMARY KEY,
         |  name VARCHAR(36) NOT NULL,
         |  typeid VARCHAR(255) NOT NULL,
         |  title VARCHAR(255),
         |  keywords VARCHAR(255),
         |  forcredit BOOLEAN,
         |  pointspossible DECIMAL(18,2)
         |)""".stripMargin.update.run

  implicit val encodeJsonForRsAsset: EncodeJson[RsAsset] = EncodeJson(a =>
    Json(
      "id"             := a.id,
      "name"           := a.name,
      "typeid"         := a.typeId,
      "title"          := a.title,
      "keywords"       := a.keywords,
      "forcredit"      := a.forCredit,
      "pointspossible" := a.pointsPossible
    )
  )
end RsAsset

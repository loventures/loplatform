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

import argonaut.*
import Argonaut.*
import cats.data.{Kleisli, NonEmptyList}
import com.learningobjects.cpxp.service.domain.DomainDTO

import java.sql.Timestamp
import doobie.*
import doobie.implicits.*
import cats.syntax.functor.*
import com.learningobjects.cpxp.component.annotation.Service

@Service
class RsTutorialViewDao(
  insertService: RedshiftInsertService
):

  def insertAll(views: NonEmptyList[RsTutorialView]): Kleisli[ConnectionIO, DomainDTO, Unit] =
    val jsons = views.map(_.asJson)
    insertService.copyIntoTable("tutorialview", jsons).void

final case class RsTutorialView(
  userId: Long,
  time: Timestamp,
  tutorialName: String,
  autoPlay: Boolean,
  step: Int,
  complete: Boolean
)

object RsTutorialView:

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""CREATE TABLE tutorialview(
         |  userid INT8 NOT NULL REFERENCES usr(id),
         |  time TIMESTAMP NOT NULL,
         |  tutorialname VARCHAR(255) NOT NULL,
         |  autoplay BOOLEAN NOT NULL,
         |  step INT4 NOT NULL,
         |  complete BOOLEAN NOT NULL
         |)""".stripMargin.update.run

  implicit val encodeJsonForRsTutorialView: EncodeJson[RsTutorialView] = EncodeJson(a =>
    Json(
      "userid"       := a.userId,
      "time"         := RedshiftSchema.RedshiftDateFormat.format(a.time),
      "tutorialname" := a.tutorialName,
      "autoplay"     := a.autoPlay,
      "step"         := a.step,
      "complete"     := a.complete
    )
  )
end RsTutorialView

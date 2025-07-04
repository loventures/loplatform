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
import cats.Order
import cats.data.{Kleisli, NonEmptyList}
import cats.instances.long.*
import cats.instances.string.*
import cats.instances.tuple.*
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.DomainDTO
import doobie.*
import doobie.implicits.*

@Service
class RsSectionContentDao(
  insertService: RedshiftInsertService
):

  /** Upserts rows to be `contents` and deletes rows that are not in `contents`.
    */
  def set(contents: NonEmptyList[RsSectionContent]): Kleisli[ConnectionIO, DomainDTO, Unit] =

    val jsons = contents.distinctBy(a => (a.sectionId, a.edgePath)).map(_.asJson)

    val tmpTableName = "sectioncontent_stage"
    val tmpTable     = Fragment.const0(tmpTableName)

    for
      _ <- Kleisli.liftF(sql"CREATE TEMP TABLE $tmpTable (LIKE sectioncontent)".update.run)
      _ <- insertService.copyIntoTable(tmpTableName, jsons)

      _ <- Kleisli.liftF(
             sql"""delete from sectioncontent using $tmpTable
                  |where sectioncontent.sectionid = $tmpTable.sectionid""".stripMargin.update.run
           )

      _ <- Kleisli.liftF(sql"insert into sectioncontent select * from $tmpTable".update.run)
    yield ()
    end for
  end set
end RsSectionContentDao

/** The sectioncontent table in Redshift. This is like a fact table in that it references the asset and section
  * dimensions, but it doesn't measure anything. Perhaps it is a bridge table. It allows one to know the content of a
  * section. Is it measuring existence?
  */
// This design makes queries about what assessments are in a section much simpler.
// An alternative design with an immutable "commitcontent" table referencing asset.id and
// section.commitid cannot be made as explicit as I would like because a foreign key
// constraint on `commitid REFERENCES (section.commitid)` is not possible because
// section.commitid is not unique. Various stack overflow answers have me fearing that
// BI tools need simple schema, and relationships described therein, to work well out of
// the box, so here is that simple schema. Its not that hard to populate and modify as
// publishes are rare.
case class RsSectionContent(
  sectionId: Long,
  edgePath: String,
  assetId: Long,
  parentEdgePath: Option[String],
  parentAssetId: Option[Long],
  courseAssetId: Option[Long],
  moduleEdgePath: Option[String],
  moduleAssetId: Option[Long],
  lessonEdgePath: Option[String],
  lessonAssetId: Option[Long],
  forCreditPointsPossible: Option[BigDecimal],
  forCreditItemCount: Option[Int],
  learningPathIndex: Option[Int],
)

object RsSectionContent:

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""CREATE TABLE sectioncontent(
          |  sectionid INT8 REFERENCES section(id),
          |  edgepath VARCHAR(32),
          |  assetid INT8 NOT NULL REFERENCES asset(id),
          |  parentedgepath VARCHAR(32),
          |  parentassetid INT8 REFERENCES asset(id),
          |  courseassetid INT8 REFERENCES asset(id),
          |  moduleedgepath VARCHAR(32),
          |  moduleassetid INT8 REFERENCES asset(id),
          |  lessonedgepath VARCHAR(32),
          |  lessonassetid INT8 REFERENCES asset(id),
          |  forcreditpointspossible DECIMAL(18, 2),
          |  forcredititemcount INT8,
          |  learningpathindex INT4,
          |  PRIMARY KEY (sectionid, edgepath)
          |)""".stripMargin.update.run

  implicit val encodeJsonForRsSectionContent: EncodeJson[RsSectionContent] = EncodeJson(a =>
    Json(
      "sectionid"               := a.sectionId,
      "edgepath"                := a.edgePath,
      "assetid"                 := a.assetId,
      "parentedgepath"          := a.parentEdgePath,
      "parentassetid"           := a.parentAssetId,
      "courseassetid"           := a.courseAssetId,
      "moduleedgepath"          := a.moduleEdgePath,
      "moduleassetid"           := a.moduleAssetId,
      "lessonedgepath"          := a.lessonEdgePath,
      "lessonassetid"           := a.lessonAssetId,
      "forcreditpointspossible" := a.forCreditPointsPossible,
      "forcredititemcount"      := a.forCreditItemCount,
      "learningpathindex"       := a.learningPathIndex,
    )
  )

  implicit val orderForRsSectionContent: Order[RsSectionContent] = Order.by(a => (a.sectionId, a.edgePath))
end RsSectionContent

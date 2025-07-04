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
class RsDiscussionPostDao(
  insertService: RedshiftInsertService
):

  // insert or update, but the update is not a general update because we only want to record
  // data about the posts _first_ reply
  def upsert(discussionPosts: NonEmptyList[RsDiscussionPost]): Kleisli[ConnectionIO, DomainDTO, Unit] =
    val rows = discussionPosts.distinctBy(_.postId).map(_.asJson)

    val tmpTableName = "discussionpost_stage"
    val tmpTable     = Fragment.const0(tmpTableName)

    for
      _ <- Kleisli.liftF(sql"CREATE TEMP TABLE $tmpTable (LIKE discussionpost)".update.run)
      _ <- insertService.copyIntoTable(tmpTableName, rows)
      _ <- Kleisli.liftF(
             // the three values are an atomic unit
             sql"""update discussionpost
                  |set instructorreplyuserid = $tmpTable.instructorreplyuserid,
                  |  instructorreplytime = $tmpTable.instructorreplytime,
                  |  instructorreplyhourlag = $tmpTable.instructorreplyhourlag
                  |from $tmpTable
                  |where discussionpost.postid = $tmpTable.postid
                  |  and discussionpost.instructorreplyuserid is null
                  |  and discussionpost.instructorreplytime is null
                  |  and discussionpost.instructorreplyhourlag is null""".stripMargin.update.run
           )
      _ <- Kleisli.liftF(
             sql"""delete from $tmpTable using discussionpost
                  |where discussionpost.postid = $tmpTable.postid""".stripMargin.update.run
           )
      _ <- Kleisli.liftF(sql"insert into discussionpost select * from $tmpTable".update.run)
    yield ()
    end for
  end upsert

  final def upsert(postHead: RsDiscussionPost, postTail: RsDiscussionPost*): Kleisli[ConnectionIO, DomainDTO, Unit] =
    upsert(NonEmptyList.of(postHead, postTail*))
end RsDiscussionPostDao

/** The discussionpost table in Redshift */
case class RsDiscussionPost(
  postId: Long,
  userId: Long,
  sectionId: Long,
  edgePath: String,
  assetId: Long,
  role: String,
  depth: Int,
  createTime: Timestamp,
  instructorReplyUserId: Option[Long],
  instructorReplyTime: Option[Timestamp],
  instructorReplyHourLag: Option[Int]
)

object RsDiscussionPost:

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""CREATE TABLE discussionpost(
         postid INT8 PRIMARY KEY,
         userid INT8 NOT NULL REFERENCES usr(id),
         sectionid INT8 NOT NULL REFERENCES section(id),
         edgepath VARCHAR(32) NOT NULL,
         assetid INT8 NOT NULL REFERENCES asset(id),
         role VARCHAR(64) NOT NULL,
         depth INT NOT NULL,
         createtime TIMESTAMP NOT NULL,
         instructorreplyuserid INT8 REFERENCES usr(id),
         instructorreplytime TIMESTAMP,
         instructorreplyhourlag INT
       )""".stripMargin.update.run

  implicit val encodeJsonForRsDiscussionPost: EncodeJson[RsDiscussionPost] = EncodeJson(a =>
    Json(
      "postid"                 := a.postId,
      "userid"                 := a.userId,
      "sectionid"              := a.sectionId,
      "edgepath"               := a.edgePath,
      "assetid"                := a.assetId,
      "role"                   := a.role,
      "depth"                  := a.depth,
      "createtime"             := RedshiftSchema.RedshiftDateFormat.format(a.createTime),
      "instructorreplyuserid"  := a.instructorReplyUserId,
      "instructorreplytime"    := a.instructorReplyTime.map(RedshiftSchema.RedshiftDateFormat.format),
      "instructorreplyhourlag" := a.instructorReplyHourLag
    )
  )
end RsDiscussionPost

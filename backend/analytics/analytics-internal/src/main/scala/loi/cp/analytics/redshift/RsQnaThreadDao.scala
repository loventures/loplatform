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

// The Q&A Thread models a thread of messages in a question, starting when a student first
// posts a message along with any immediate followup messages, finishing when an instructor
// posts a response along with any immediate followup responses. If a student follows up
// with a new message after the instructor replies, that opens a new thread.
@Service
class RsQnaThreadDao(
  insertService: RedshiftInsertService
):

  def upsert(qnaThreads: NonEmptyList[RsQnaThread]): Kleisli[ConnectionIO, DomainDTO, Unit] =
    val rows = qnaThreads.distinctBy(_.threadId).map(_.asJson)

    val tmpTableName = "qnathread_stage"
    val tmpTable     = Fragment.const0(tmpTableName)

    for
      _ <- Kleisli.liftF(sql"CREATE TEMP TABLE $tmpTable (LIKE qnathread)".update.run)
      _ <- insertService.copyIntoTable(tmpTableName, rows)
      _ <- Kleisli.liftF(
             // the pecular upsert behaviour is to support recategorization without changing
             // the instructor reply/student close setting, and to allow an instructor to reply
             // many times but only have the first one recorded
             sql"""update qnathread
             |set instructorreplyuserid = COALESCE(qnathread.instructorreplyuserid, $tmpTable.instructorreplyuserid),
             |  instructorreplytime = COALESCE(qnathread.instructorreplytime, $tmpTable.instructorreplytime),
             |  instructorreplyhourlag = COALESCE(qnathread.instructorreplyhourlag, $tmpTable.instructorreplyhourlag),
             |  studentclosed = qnathread.studentclosed OR $tmpTable.studentclosed,
             |  category = $tmpTable.category,
             |  subcategory = $tmpTable.subcategory
             |from $tmpTable
             |where qnathread.threadid = $tmpTable.threadid""".stripMargin.update.run
           )
      _ <- Kleisli.liftF(
             sql"""delete from $tmpTable using qnathread
             |where qnathread.threadid = $tmpTable.threadid""".stripMargin.update.run
           )
      _ <- Kleisli.liftF(sql"insert into qnathread select * from $tmpTable".update.run)
    yield ()
    end for
  end upsert

  final def upsert(threadHead: RsQnaThread, threadTail: RsQnaThread*): Kleisli[ConnectionIO, DomainDTO, Unit] =
    upsert(NonEmptyList.of(threadHead, threadTail*))
end RsQnaThreadDao

/** The qnathread table in Redshift */
case class RsQnaThread(
  threadId: Long,
  questionId: Long,
  userId: Long,
  sectionId: Long,
  edgePath: String,
  // assetId: Long,
  createTime: Timestamp,
  instructorReplyUserId: Option[Long],    // This will not replace an existing value
  instructorReplyTime: Option[Timestamp], // This will not replace an existing value
  instructorReplyHourLag: Option[Int],    // This will not replace an existing value
  studentClosed: Boolean,                 // This will not replace an existing true
  category: Option[String],               // This always replaces or nulls an existing value
  subcategory: Option[String],            // This always replaces or nulls an existing value
)

object RsQnaThread:

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  val createTable: ConnectionIO[Int] =
    sql"""CREATE TABLE qnathread(
         threadid INT8 PRIMARY KEY,
         questionid INT8 NOT NULL,
         userid INT8 NOT NULL REFERENCES usr(id),
         sectionid INT8 NOT NULL REFERENCES section(id),
         edgepath VARCHAR(32) NOT NULL,
         createtime TIMESTAMP NOT NULL,
         instructorreplyuserid INT8 REFERENCES usr(id),
         instructorreplytime TIMESTAMP,
         instructorreplyhourlag INT,
         studentclosed BOOLEAN NOT NULL,
         category VARCHAR(256),
         subcategory VARCHAR(256)
       )""".stripMargin.update.run
  // assetid INT8 NOT NULL REFERENCES asset(id),

  implicit val encodeJsonForRsQnaThread: EncodeJson[RsQnaThread] = EncodeJson(a =>
    Json(
      "threadid"               := a.threadId,
      "questionid"             := a.questionId,
      "userid"                 := a.userId,
      "sectionid"              := a.sectionId,
      "edgepath"               := a.edgePath,
      // "assetid"                := a.assetId,
      "createtime"             := RedshiftSchema.RedshiftDateFormat.format(a.createTime),
      "instructorreplyuserid"  := a.instructorReplyUserId,
      "instructorreplytime"    := a.instructorReplyTime.map(RedshiftSchema.RedshiftDateFormat.format),
      "instructorreplyhourlag" := a.instructorReplyHourLag,
      "studentclosed"          := a.studentClosed,
      "category"               := a.category,
      "subcategory"            := a.subcategory
    )
  )
end RsQnaThread

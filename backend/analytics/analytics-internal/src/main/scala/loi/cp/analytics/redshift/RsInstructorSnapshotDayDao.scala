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
import argonaut.{EncodeJson, *}
import cats.data.NonEmptyList
import cats.syntax.flatMap.*
import cats.syntax.foldable.*
import cats.syntax.functor.*
import com.learningobjects.cpxp.component.annotation.Service
import doobie.*
import doobie.implicits.*

import java.sql.Date

// no dependencies... do I dare make it an object
@Service
class RsInstructorSnapshotDayDao():

  private implicit val log: org.log4s.Logger = org.log4s.getLogger

  def generate(days: NonEmptyList[java.util.Date]): ConnectionIO[Unit] =

    // temp tables are automatically dropped at the end of "the session"
    // but in the unlikely event that `days.size > 1` we need to drop it sooner,
    // before we try to create it again in the subsequent `generate(day)` invocation
    days.traverse(day => generate(day) >> sql"DROP TABLE instructorsnapshotday_stage".update.run).void

  /** Generates the instructor snapshots for `day` and inserts them into `instructorsnapshotday`
    */
  // inserts ~2000 rows per day. Would be ~1000 per day if I prevented snapshot generation
  // for users that get filtered out at query time, but the blacklist includes email
  // domains such as "asu.edu" and "broward.edu" which is surely a mistake no? Instructors
  // can have such emails. Such a mistake can be fixed at any query time, but not at _the_
  // ETL time.
  def generate(day: java.util.Date): ConnectionIO[Unit] =

    import RsInstructorSnapshotDay.{IdCols, UpdateCols}

    // for when the day needs to be its literal value, not a PreparedStatement parameter
    // which is what doobie will do if I just did fr"$day"
    val dayLiteral = Fragment.const0(RedshiftSchema.RedshiftDateOnlyFormat.format(day))

    // activeenrollment is mutable data, so an instructor may do things on a day, but if,
    // for example, their enrollment is disabled before that day's snapshot runs, then no
    // snapshot for that instructor-section is computed. To solve this, the underlying
    // dimensions: enrollment and section, need to become "Type 2 Slowly Changing Dimensions".
    // But that is punishment for future me. Hopefully the disabled/starttime/endtime of
    // enrollments/sections won't change once a course is in use.
    val selectSnapshots = sql"""WITH instructorenrollment(userid, sectionid) AS (
         |  SELECT userid, sectionid
         |  FROM activeenrollment
         |  WHERE role='instructor'
         |), entry(userid, sectionid, num) AS (
         |  SELECT e.userid, e.sectionid, COUNT(se.userid)
         |  FROM instructorenrollment e LEFT JOIN sectionentry se
         |    ON  se.userid = e.userid
         |    AND se.sectionid = e.sectionid
         |    AND se.role='instructor'
         |    AND trunc(se.time) = $day
         |  GROUP BY e.userid, e.sectionid
         |), post(userid, sectionid, num) AS (
         |  SELECT e.userid, e.sectionid, COUNT(p.postid)
         |  FROM instructorenrollment e
         |    LEFT JOIN discussionpost p
         |      ON  p.userid = e.userid
         |      AND p.sectionid = e.sectionid
         |      AND p.role='instructor'
         |      AND p.depth = 0
         |      AND trunc(p.createtime) = $day
         |  GROUP BY e.userid, e.sectionid
         |), reply(userid, sectionid, num) AS (
         |  SELECT e.userid, e.sectionid, COUNT(r.postid)
         |  FROM instructorenrollment e
         |    LEFT JOIN discussionpost r
         |      ON  r.userid = e.userid
         |      AND r.sectionid = e.sectionid
         |      AND r.role='instructor'
         |      AND r.depth > 0
         |      AND trunc(r.createtime) = $day
         |  GROUP BY e.userid, e.sectionid
         |), visit(userid, sectionid, num) AS (
         |  SELECT e.userid, e.sectionid, COUNT(v.userid)
         |  FROM instructorenrollment e
         |    LEFT JOIN pagenavevent v
         |      ON  v.userid = e.userid
         |      AND v.sectionid = e.sectionid
         |      AND v.assettypeid = 'discussion.1'
         |      AND trunc(v.time) = $day
         |  GROUP BY e.userid, e.sectionid
         |), score(userid, sectionid, num) AS (
         |  SELECT e.userid, e.sectionid, COUNT(a.id)
         |  FROM instructorenrollment e
         |    LEFT JOIN attempt a
         |      ON  a.scoreruserid = e.userid
         |      AND a.sectionid = e.sectionid
         |      AND a.manualscore
         |      AND trunc(a.scoretime) = $day
         |  GROUP BY e.userid, e.sectionid
         |)
         |SELECT
         |  e.userid,
         |  e.sectionid,
         |  CAST('$dayLiteral' AS DATE) AS "date",
         |  entry.num AS numsectionentry,
         |  score.num AS numattemptgrade,
         |  post.num AS numdiscussionpost,
         |  reply.num AS numdiscussionreply,
         |  visit.num AS numdiscussionvisit,
         |  (post.num + reply.num + visit.num) as numdiscussioninteraction
         |FROM instructorenrollment e
         |  JOIN entry USING (userid, sectionid)
         |  JOIN post USING (userid, sectionid)
         |  JOIN reply USING (userid, sectionid)
         |  JOIN visit USING (userid, sectionid)
         |  JOIN score USING (userid, sectionid)
         |""".stripMargin

    val tmpTable    = Fragment.const0("instructorsnapshotday_stage")
    val tgtTable    = Fragment.const0("instructorsnapshotday")
    val idEqual     = IdCols.map(col => fr"$tmpTable.$col = $tgtTable.$col").intercalate(fr" AND")
    val assignments = UpdateCols.map(col => fr"$col = $tmpTable.$col").intercalate(fr",")

    for
      _ <- sql"CREATE TEMP TABLE $tmpTable AS $selectSnapshots".update.run
      _ <- sql"UPDATE $tgtTable SET $assignments FROM $tmpTable WHERE $idEqual".update.run
      _ <- sql"DELETE FROM $tmpTable USING $tgtTable WHERE $idEqual".update.run
      _ <- sql"INSERT INTO $tgtTable SELECT * FROM $tmpTable".update.run
    yield ()
  end generate
end RsInstructorSnapshotDayDao

// the instructorsnapshotday table in Redshift
// is a periodic snapshot fact table
// warning java.sql.Date parses dates different from java.util.Date
// java.sql.Date does not read any time parts, in accordance with SQL DATE type.
// and I am stuck with a driver that chooses java.sql.Date.
case class RsInstructorSnapshotDay(
  userId: Long,
  sectionId: Long,
  date: Date,
  numSectionEntry: Int,
  numAttemptGrade: Int,
  numDiscussionPost: Int,
  numDiscussionReply: Int,
  numDiscussionVisit: Int,
  numDiscussionInteraction: Int,
)

object RsInstructorSnapshotDay:

  private implicit val logger: org.log4s.Logger = org.log4s.getLogger

  val IdCols: NonEmptyList[Fragment]     = NonEmptyList.of("userid", "sectionid", "date").map(Fragment.const0(_))
  val UpdateCols: NonEmptyList[Fragment] = NonEmptyList
    .of(
      "numsectionentry",
      "numattemptgrade",
      "numdiscussionpost",
      "numdiscussionreply",
      "numdiscussionvisit",
      "numdiscussioninteraction"
    )
    .map(Fragment.const0(_))

  val createTable: ConnectionIO[Int] =
    sql"""CREATE TABLE instructorsnapshotday(
         |  userid INT8 NOT NULL REFERENCES usr(id),
         |  sectionid INT8 NOT NULL REFERENCES section(id),
         |  date DATE SORTKEY NOT NULL,
         |  numsectionentry INT,
         |  numattemptgrade INT,
         |  numdiscussionpost INT,
         |  numdiscussionreply INT,
         |  numdiscussionvisit INT,
         |  numdiscussioninteraction INT,
         |  PRIMARY KEY(userid, sectionid, date)
         |)""".stripMargin.update.run

  implicit val encodeJsonForRsInstructorSnapshotDay: EncodeJson[RsInstructorSnapshotDay] = EncodeJson(a =>
    Json(
      "userid"                   := a.userId,
      "sectionid"                := a.sectionId,
      "date"                     := RedshiftSchema.RedshiftDateOnlyFormat.format(a.date),
      "numsectionentry"          := a.numSectionEntry,
      "numattemptgrade"          := a.numAttemptGrade,
      "numdiscussionpost"        := a.numDiscussionPost,
      "numdiscussionreply"       := a.numDiscussionReply,
      "numdiscussionvisit"       := a.numDiscussionVisit,
      "numdiscussioninteraction" := a.numDiscussionInteraction,
    )
  )
end RsInstructorSnapshotDay

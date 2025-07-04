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

import cats.instances.list.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*
import com.typesafe.config.ConfigFactory
import doobie.*
import doobie.implicits.*
import loi.doobie.io.*

import java.text.SimpleDateFormat

object RedshiftSchema:

  // caution: uses a local calendar to "display" the epoch seconds. This means that
  // timestamp values in redshift are in client time zone not UTC.
  final val RedshiftDateFormat     = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
  final val RedshiftDateOnlyFormat = new SimpleDateFormat("yyyy-MM-dd")

  final def config = ConfigFactory.load().getConfig("de.databases.redshift")

  def createSchema(schemaName: String, busUserName: Option[String]): ConnectionIO[Unit] =
    val frSchemaName = Fragment.const(schemaName)

    // the cloud formation stack setup ran this:
    // ALTER DEFAULT PRIVILEGES FOR USER "main user" GRANT ALL ON TABLES TO "bus user";
    // so "bus user" can do ALL on the tables created by "main user"
    // but it cannot USAGE any schema created by "main user", so the new schema's tables are inaccessible.
    // We have to manually grant USAGE (we grant broader ALL) to "bus user" to make them accessible.
    val grantStmt = busUserName.map(u => Fragment.const(u)) match
      case Some(frBusUserName) => fr"GRANT ALL ON SCHEMA $frSchemaName TO $frBusUserName".update.run.void
      case None                => FC.unit // there is no Monoid[ConnectionIO[Unit]] (this can't be right)

    fr"CREATE SCHEMA $frSchemaName".update.run >> grantStmt
  end createSchema

  def createAll(schemaName: String, busUserName: Option[String]): ConnectionIO[Unit] =
    List(
      createSchema(schemaName, busUserName),
      // pg_catalog and pg_temp are still in front of what we specify here
      // pg_temp holds the temp tables (temp tables destroyed on session end, aka connection end)
      setSearchPath(schemaName).void,
    ).sequence >> createTables

  val createTables: ConnectionIO[Unit] = List(
    // dimensions
    RsAsset.createTable,
    RsUser.createTable,
    RsSection.createTable,
    RsSession.createTable,
    RsSessionEvent.createTable,
    // facts
    RsActivity.createTable,
    RsAttempt.createTable,
    RsEnrollment.createTable,
    RsGrade.createTable,
    RsPageNavEvent.createTable,
    RsProgress.createTable,
    RsProgressOverTime.createTable,
    RsSectionEntry.createTable,
    RsSectionContent.createTable,
    RsInstructorSnapshotDay.createTable,
    RsSurveyQuestionResponse.createTable,
    RsTimeSpentDiscrete.createTable,
    RsTutorialView.createTable,
    RsDiscussionPost.createTable,
    RsQnaThread.createTable,
    // dimension views
    RsActiveSection.createView,
    RsActiveEnrollment.createView,
    // fact views
    RsMostRecentInstructorEntryTime.createView,
    RsOverdueAttempt.createView,
    RsInstructorDiscussionThread.createView,
    RsInstructorDiscussionReply.createView,
    RsInstructorDiscussionVisit.createView,
    RsStudentDiscussionThread.createView,
    RsTimeSpent.createView,
    RsTimeSpentModule.createView,
    RsUnscoredAttempt.createView,
  ).sequence.void

  object RsActiveSection:
    val createView: ConnectionIO[Int] =
      sql"""CREATE VIEW activesection AS
           |SELECT *
           |FROM section
           |WHERE disabled IS NOT TRUE
           |  AND (starttime IS NULL OR trunc(starttime) <= current_date)
           |  AND (endtime IS NULL OR trunc(endtime) >= current_date)
      """.stripMargin.update.run

  object RsActiveEnrollment:
    val createView: ConnectionIO[Int] =
      sql"""CREATE VIEW activeenrollment AS
           |SELECT e.*
           |FROM enrollment e
           |  join activesection s on e.sectionid = s.id
           |WHERE e.disabled IS NOT TRUE
           |  AND (e.starttime IS NULL OR trunc(e.starttime) <= current_date)
           |  AND (e.endtime IS NULL OR trunc(e.endtime) >= current_date)
      """.stripMargin.update.run
  end RsActiveEnrollment

  // caution if you want date_diff with lower granularity than 'day', then switch
  // current_date to getdate() because current_date is a date not a datetime
  object RsMostRecentInstructorEntryTime:
    val createView: ConnectionIO[Int] =
      sql"""CREATE VIEW mostrecentinstructorentry(userid, sectionid, mostrecententry, daylag) AS
           |SELECT e.userid, e.sectionid, max(time), date_diff('day', max(time), current_date)
           |FROM activeenrollment e
           |  LEFT JOIN sectionentry se ON se.userid = e.userid
           |   AND se.sectionid = e.sectionid
           |   AND se.role = 'instructor'
           |WHERE e.role = 'instructor'
           |GROUP BY e.userid, e.sectionid
           |""".stripMargin.update.run
  end RsMostRecentInstructorEntryTime

  object RsOverdueAttempt:

    // caution: multiple instructors for one section "increase" the number of attempts
    val createView: ConnectionIO[Int] =
      sql"""CREATE VIEW overdueattempt(
           |  id,
           |  instructoruserid,
           |  learneruserid,
           |  sectionid,
           |  edgepath,
           |  assetid,
           |  state,
           |  valid,
           |  createtime,
           |  submittime) AS
           |SELECT
           |  attempt.id,
           |  ae.userid,
           |  attempt.userid,
           |  attempt.sectionid,
           |  attempt.edgepath,
           |  attempt.assetid,
           |  attempt.state,
           |  attempt.valid,
           |  attempt.createtime,
           |  attempt.submittime
           |FROM attempt
           |  JOIN activeenrollment ae on attempt.sectionid = ae.sectionid and ae.role='instructor'
           |WHERE date_diff('day', attempt.submittime, current_date) > 2
           |  AND attempt.manualscore
           |  AND attempt.valid
           |  AND attempt.state = 'Submitted'""".stripMargin.update.run
  end RsOverdueAttempt

  object RsUnscoredAttempt:

    val createView: ConnectionIO[Int] =
      sql"""CREATE VIEW unscoredattempt AS
           |SELECT id, userid, sectionid, edgepath, assetid, createtime, submittime,
           |       date_diff('hour', submittime, getdate()) AS waithour,
           |       date_diff('day', submittime, getdate()) AS waitday
           |FROM attempt
           |WHERE state = 'Submitted' AND valid and manualscore
         """.stripMargin.update.run
  end RsUnscoredAttempt

  // How many times has the instructor created a new post in a discussion board?
  // How many times does the instructor post to a discussion board per day, on average?
  object RsInstructorDiscussionThread:

    val createView: ConnectionIO[Int] =
      sql"""CREATE VIEW instructordiscussionthread(postid, userid, sectionid, edgepath, assetid, createtime) AS
           |SELECT
           |  post.postid,
           |  post.userid,
           |  post.sectionid,
           |  post.edgepath,
           |  post.assetid,
           |  post.createtime
           |FROM discussionpost post
           |  JOIN activeenrollment ae
           |    ON post.sectionid = ae.sectionid
           |    AND ae.userid = post.userid
           |    AND ae.role='instructor'
           |WHERE post.role = 'instructor'
           |  AND post.depth = 0""".stripMargin.update.run
  end RsInstructorDiscussionThread

  // How many times has the instructor replied to a student in a discussion board?
  // How many times does the instructor respond to a student post per day, on average?
  object RsInstructorDiscussionReply:

    val createView: ConnectionIO[Int] =
      sql"""CREATE VIEW instructordiscussionreply(postid, userid, sectionid, edgepath, assetid, createtime, depth) AS
           |SELECT
           |  post.postid,
           |  post.userid,
           |  post.sectionid,
           |  post.edgepath,
           |  post.assetid,
           |  post.createtime,
           |  post.depth
           |FROM discussionpost post
           |  JOIN activeenrollment ae
           |    ON post.sectionid = ae.sectionid
           |    AND ae.userid = post.userid
           |    AND ae.role='instructor'
           |WHERE post.role = 'instructor'
           |  AND post.depth > 0""".stripMargin.update.run
  end RsInstructorDiscussionReply

  // How long does it take the instructor, on average, to respond to a student post?
  object RsStudentDiscussionThread:

    val createView: ConnectionIO[Int] =
      sql"""CREATE VIEW studentdiscussionthread(postid, userid, sectionid, edgepath, assetid, createtime, instructorreplyuserid, instructorreplytime, instructorreplyhourlag) AS
           |SELECT
           |  post.postid,
           |  post.userid,
           |  post.sectionid,
           |  post.edgepath,
           |  post.assetid,
           |  post.createtime,
           |  post.instructorreplyuserid,
           |  post.instructorreplytime,
           |  post.instructorreplyhourlag
           |FROM discussionpost post
           |  JOIN activeenrollment ae
           |    ON post.sectionid = ae.sectionid
           |    AND ae.userid = post.userid
           |    AND ae.role='student'
           |WHERE post.role = 'student'
           |  AND post.depth = 0""".stripMargin.update.run
  end RsStudentDiscussionThread

  // How many times has the instructor visited a discussion board?
  // How many times does the instructor visit a discussion board per day, on average?
  object RsInstructorDiscussionVisit:

    val createView: ConnectionIO[Int] =
      sql"""CREATE VIEW instructordiscussionvisit(id, userid, sectionid, edgepath, assettitle, time) AS
           |SELECT p.id, p.userid, p.sectionid, p.edgepath, p.assettitle, p.time
           |FROM pagenavevent p
           |  JOIN activeenrollment ae ON ae.userid = p.userid AND ae.sectionid = p.sectionid AND ae.role='instructor'
           |WHERE p.assettypeid = 'discussion.1'
           """.stripMargin.update.run
end RedshiftSchema

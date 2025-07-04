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

package loi.authoring.branch.store

import java.sql.Timestamp
import java.time.Instant
import argonaut.Argonaut.*
import argonaut.*
import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import jakarta.persistence.Query
import loi.authoring.branch.Branch
import org.hibernate.Session
import scaloi.json.ArgoExtras
import scaloi.json.ArgoExtras.*
import scaloi.syntax.any.*
import scaloi.syntax.collection.*

import scala.jdk.CollectionConverters.*

/** A debugging tool to show "activity" on a branch (commits, publishes, updates, section creations). Useful for
  * determining what the content was before various events.
  */
@Service
class ActivityLogDao(
  session: => Session
):

  /** Load pages of the activity log
    */
  def loadActivityLog(branch: Branch, offset: Option[Int], limit: Option[Int]): List[BranchAction] =
    loadActivityLog(
      branch,
      "SELECT * FROM activitylog ORDER BY time DESC",
      q =>
        offset.foreach(q.setFirstResult)
        limit.foreach(q.setMaxResults)
    )

  /** Load the activity log from now until `end`, exclusive
    */
  def loadActivityLog(branch: Branch, end: Timestamp): List[BranchAction] =
    loadActivityLog(
      branch,
      "SELECT * FROM activitylog WHERE time > :end ORDER BY time DESC",
      q => q.setParameter("end", end)
    )

  private def loadActivityLog(branch: Branch, selectFragment: String, qf: Query => Unit): List[BranchAction] =

    // :shame:
    val project = branch.requireProject

    val ctes =
      """WITH RECURSIVE commitlog AS (
        |  SELECT c.*
        |  FROM assetcommit c join assetbranch b ON b.head_id = c.id
        |  WHERE b.id = :branchId
        |  UNION ALL
        |  SELECT c.*
        |  FROM assetcommit c join commitlog ON c.id = commitlog.parent_id
        |), updatelog AS (
        |  SELECT *
        |  FROM updateofferinghistory
        |  WHERE branch_id = :branchId
        |), offeringlog AS (
        |  SELECT *
        |  FROM groupfinder
        |  WHERE xtype='CourseOffering'
        |    AND del IS NULL
        |    AND branch = :branchId
        |), sectionlog AS (
        |  SELECT s.*
        |  FROM groupfinder s
        |    JOIN offeringlog ON offeringlog.id = s.mastercourse_id
        |  WHERE s.xtype='CourseSection' AND s.del is null
        |), activitylog(time, action, key, data) AS (
        |  SELECT created, 'commit', id, json_build_object('userId', createdBy, 'parentId', parent_id) FROM commitlog
        |  UNION ALL
        |  SELECT createdate, 'update', id, json_build_object('userId', createdby_id) FROM updatelog
        |  UNION ALL
        |  SELECT createtime, 'publish', id, json_build_object('userId', creator_id) FROM offeringlog
        |  UNION ALL
        |  SELECT createtime, 'create section', id, json_build_object('groupId', groupid, 'offeringId', mastercourse_id, 'userId', creator_id) FROM sectionlog
        |)""".stripMargin

    val logItems = session
      .createNativeQuery(s"""$ctes
                            |$selectFragment""".stripMargin)
      .setParameter("branchId", branch.id)
      .tap(qf)
      .getResultList
      .asInstanceOf[java.util.List[Array[Object]]]
      .asScala
      .toList
      .map(row =>
        val time   = row(0).asInstanceOf[Timestamp].toInstant
        val action = row(1).asInstanceOf[String]
        // this is an id from various tables
        val id     = row(2).asInstanceOf[Number].longValue
        val data   = JacksonUtils.getMapper.readValue(row(3).asInstanceOf[String], classOf[JsonNode])
        (time, action, id, data)
      )

    val commitIds = logItems.foldLeft(Set.empty[Long])({
      case (acc, (_, "commit", commitId, data)) =>
        val parentCommitId = Option(data.get("parentId")).map(_.asLong)
        acc + commitId ++ parentCommitId
      case (acc, _)                             => acc
    })

    // next, load the course nodes for each commit
    val courseIds = session
      .createNativeQuery(s"""SELECT c.id as commitid, course.id, course.name
           |FROM assetcommit c,
           |    jsonb_each_text(nodes) AS node(name, id)
           |    JOIN assetedge pe ON pe.source_id = CAST(node.id AS bigint) AND pe."group"='courses'
           |    JOIN assetnode course ON pe.target_id = course.id and course.name = :courseName
           |WHERE c.id = ANY(CAST(:commitIds AS BIGINT[]))
           |  AND node.name = :programName
           |""".stripMargin)
      .setParameter("commitIds", commitIds.mkString("{", ",", "}"))
      .setParameter("courseName", project.homeNodeName.toString)
      .setParameter("programName", project.rootNodeName.toString)
      .getResultList
      .asInstanceOf[java.util.List[Array[Object]]]
      .asScala
      .groupMapUniq(row => row(0).asInstanceOf[Number].longValue)(row =>
        (row(1).asInstanceOf[Number].longValue, row(2).asInstanceOf[String])
      )

    logItems.collect({
      case (time, "commit", commitId, data)          =>
        val (courseId, courseName) = courseIds.getOrElse(commitId, (0L, "") /* this would be very unexpected */ )
        val parentCommitId         = Option(data.get("parentId")).map(_.asLong)
        // JsonNode.path will return MissingNode if the Bizarre happened, whose .asLong is 0
        // JsonNode.get will return null if the Bizarre happened, whose .asLong is a thrown NullPointerException
        // and since this is a debugging tool, I'd rather have zero's and incomplete data than NPEs and no data
        CommitAction(
          time,
          commitId,
          data.path("userId").asLong,
          courseId,
          courseName,
          parentCommitId,
          parentCommitId.flatMap(courseIds.get).map(_._1)
        )
      case (time, "update", historyId, data)         =>
        UpdateOfferingAction(time, historyId, data.path("userId").asLong)
      case (time, "publish", offeringId, data)       =>
        PublishOfferingAction(time, offeringId, data.path("userId").asLong)
      case (time, "create section", sectionId, data) =>
        CreateSectionAction(
          time,
          sectionId,
          data.path("groupId").asText,
          data.path("offeringId").asLong,
          data.path("userId").asLong
        )
    })
  end loadActivityLog
end ActivityLogDao

sealed trait BranchAction:
  def userId: Long

object BranchAction:

  implicit val encodeJsonForBranchAction: EncodeJson[BranchAction] = EncodeJson[BranchAction]({
    case aa: CommitAction          => ("action", "commit".asJson) ->: aa.asJson
    case aa: UpdateOfferingAction  => ("action", "update".asJson) ->: aa.asJson
    case aa: PublishOfferingAction => ("action", "publish".asJson) ->: aa.asJson
    case aa: CreateSectionAction   => ("action", "create section".asJson) ->: aa.asJson
  })

case class CommitAction(
  time: Instant,
  commitId: Long,
  userId: Long,
  courseId: Long,
  courseName: String,
  parentCommitId: Option[Long],
  parentCourseId: Option[Long],
) extends BranchAction

object CommitAction:
  implicit val codecJsonForCommitAction: CodecJson[CommitAction] =
    CodecJson.casecodec7(CommitAction.apply, ArgoExtras.unapply)(
      "time",
      "commitId",
      "userId",
      "courseId",
      "courseName",
      "parentCommitId",
      "parentCourseId",
    )
end CommitAction
case class UpdateOfferingAction(time: Instant, updateOfferingHistoryId: Long, userId: Long) extends BranchAction

object UpdateOfferingAction:
  implicit val codecJsonForUpdateOfferingAction: CodecJson[UpdateOfferingAction] =
    CodecJson.casecodec3(UpdateOfferingAction.apply, ArgoExtras.unapply)(
      "time",
      "updateOfferingHistoryId",
      "userId"
    )
case class PublishOfferingAction(time: Instant, offeringId: Long, userId: Long) extends BranchAction

object PublishOfferingAction:
  implicit val codecJsonForPublishOfferingAction: CodecJson[PublishOfferingAction] =
    CodecJson.casecodec3(PublishOfferingAction.apply, ArgoExtras.unapply)(
      "time",
      "offeringId",
      "userId"
    )

case class CreateSectionAction(time: Instant, sectionId: Long, groupId: String, offeringId: Long, userId: Long)
    extends BranchAction

object CreateSectionAction:
  implicit val codecJsonForCreateSectionAction: CodecJson[CreateSectionAction] =
    CodecJson.casecodec5(CreateSectionAction.apply, ArgoExtras.unapply)(
      "time",
      "sectionId",
      "groupId",
      "offeringId",
      "userId",
    )

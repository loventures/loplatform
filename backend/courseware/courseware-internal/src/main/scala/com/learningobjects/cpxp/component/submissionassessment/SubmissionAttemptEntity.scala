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

package com.learningobjects.cpxp.component.submissionassessment

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import com.learningobjects.cpxp.scala.util.JTypes.{JBoolean, JLong}
import com.learningobjects.cpxp.service.user.UserId
import jakarta.persistence.*
import loi.cp.assessment.attempt.{AbstractAttemptEntity, AttemptState, AttemptyWempty}
import loi.cp.submissionassessment.SubmissionAssessment
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.util.Date

/** An entity representing a user attempt against a submission based assessment. The {{UserInteractionEntity}} fields
  * point to the assessment in question with its context.
  */
@Entity
@Table(
  name = "SubmissionAttempt",
  indexes = Array(
    new Index(
      name = "submissionattempt_complex_idx",
      columnList = "contextid,edgepath,userid,valid,attemptstate,submittime,responsetime,createtime"
    )
  )
)
@HCache(usage = READ_WRITE)
class SubmissionAttemptEntity extends AbstractAttemptEntity:
  @Id
  var id: java.lang.Long = scala.compiletime.uninitialized

  /** the column containing the current state of the attempt */
  @Column(nullable = false)
  var attemptState: String = scala.compiletime.uninitialized

  /** the column containing whether the current attempt is valid */
  @Column(nullable = false)
  var valid: JBoolean = scala.compiletime.uninitialized

  /** the column containing when this attempt was created */
  @Column(nullable = false)
  var createTime: Date = scala.compiletime.uninitialized

  /** the column containing when this attempt was most recently responded to */
  @Column
  var responseTime: Date = scala.compiletime.uninitialized

  /** the column containing when this attempt was submitted, if it was submitted */
  @Column
  var submitTime: Date = scala.compiletime.uninitialized

  /** the column containing when this attempt's score was updated, if it was scored */
  @Column
  var scoreTime: Date = scala.compiletime.uninitialized

  /** the column containing a score, which may or may not be meaningful depending on {{state}} */
  @Column(columnDefinition = "JSONB")
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var score: JsonNode = scala.compiletime.uninitialized

  /** the column containing the user who set the score */
  @Column
  var scorer: JLong = scala.compiletime.uninitialized

  /** the column containing an essay response */
  @Column(columnDefinition = "TEXT")
  var essay: String = scala.compiletime.uninitialized

  /** a column containing an object holding the user response (attachments) for the attempt */
  @Column(nullable = false, columnDefinition = "JSONB")
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var attachments: JsonNode = scala.compiletime.uninitialized

  /** a column for instructor feedback on the attempt */
  @Column(nullable = false, columnDefinition = "JSONB")
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var feedback: JsonNode = scala.compiletime.uninitialized

  @Column(nullable = false)
  var feedbackReleased: JBoolean = scala.compiletime.uninitialized

  /** the folder the attempt is in */
  @Column(name = "attemptfolder_id", nullable = false)
  var attemptFolderId: java.lang.Long = scala.compiletime.uninitialized

  @Column(name = "root_id", nullable = false)
  var rootId: java.lang.Long = scala.compiletime.uninitialized
end SubmissionAttemptEntity

object SubmissionAttemptEntity:
  import loi.cp.assessment.persistence.AssessmentDaoUtils.jsonNode

  def newAttempt(
    id: Long,
    rootId: Long,
    assessment: SubmissionAssessment,
    user: UserId,
    createTime: Date,
    attemptFolder: SubmissionAttemptFolderEntity
  ): SubmissionAttemptEntity =
    val attemptEntity: SubmissionAttemptEntity = new SubmissionAttemptEntity

    attemptEntity.id = id
    attemptEntity.rootId = rootId

    attemptEntity.attemptState = AttemptState.Open.toString
    attemptEntity.valid = true
    attemptEntity.createTime = createTime

    attemptEntity.score = null

    attemptEntity.attachments = jsonNode(Seq())
    attemptEntity.essay = null

    attemptEntity.feedback = jsonNode(Seq())
    attemptEntity.feedbackReleased = false

    attemptEntity.userId = user.id
    attemptEntity.commitId = assessment.assetReference.commit
    attemptEntity.nodeName = assessment.assetReference.nodeName.toString
    attemptEntity.contextId = assessment.contentId.contextId.value
    attemptEntity.edgePath = assessment.contentId.edgePath.toString
    attemptEntity.attemptFolderId = attemptFolder.id()

    attemptEntity
  end newAttempt

  implicit val submissionAttemptyWempty: AttemptyWempty[SubmissionAttemptEntity] =
    new AttemptyWempty[SubmissionAttemptEntity]:
      import scalaz.syntax.std.option.*
      import scaloi.syntax.option.* // absolutely definitely worth it
      override def updateTimeSql: String                            = "COALESCE(submitTime, responseTime, createTime)"
      override def valid(t: SubmissionAttemptEntity): Boolean       = t.valid
      override def attemptState(t: SubmissionAttemptEntity): String = t.attemptState
      override def submitTime(t: SubmissionAttemptEntity): Date     = t.submitTime
      override def updateTime(t: SubmissionAttemptEntity): Date     =
        Option(t.submitTime) || Option(t.responseTime) | t.createTime
end SubmissionAttemptEntity

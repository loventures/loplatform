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

package com.learningobjects.cpxp.component.quiz

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import com.learningobjects.cpxp.scala.util.JTypes.JBoolean
import com.learningobjects.cpxp.service.user.UserDTO
import jakarta.persistence.*
import loi.cp.assessment.attempt.{AbstractAttemptEntity, AttemptState, AttemptyWempty}
import loi.cp.quiz.Quiz
import loi.cp.quiz.attempt.{QuizAttemptQuestionUsage, QuizQuestionResponse}
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.util.Date

/** An entity representing a user attempt against a quiz.
  *
  * The [[AbstractAttemptEntity]] fields point to the quiz in question with its context.
  */
@Entity
@Table(
  name = "quizattempt",
  indexes = Array(
    // The extra columns in this index allow us to run index-only aggregates against this table. This saves the
    // database from having to load full rows, including JSON, when filtering/aggregating just these columns.
    new Index(
      name = "quizattempt_complex_idx",
      columnList = "contextid,edgepath,userid,valid,attemptstate,submittime,updatetime"
    )
  )
)
@HCache(usage = READ_WRITE)
class QuizAttemptEntity extends AbstractAttemptEntity:

  @Id
  var id: java.lang.Long = scala.compiletime.uninitialized

  /** the column containing the current state of the assessment */
  @Column(nullable = false)
  var attemptState: String = scala.compiletime.uninitialized

  /** the column containing whether the current attempt is valid */
  @Column(nullable = false)
  var valid: JBoolean = scala.compiletime.uninitialized

  /** the column containing when this attempt was created */
  @Column(nullable = false)
  var createTime: Date = scala.compiletime.uninitialized

  /** the column containing when this attempt was last updated */
  @Column(nullable = false)
  var updateTime: Date = scala.compiletime.uninitialized

  /** the column containing when this attempt was submitted, if it was submitted */
  @Column
  var submitTime: Date = scala.compiletime.uninitialized

  /** Was this auto-submitted at time limit. */
  @Column
  var autoSubmitted: JBoolean = scala.compiletime.uninitialized

  /** the column containing when this attempt's score was updated, if it was scored */
  @Column
  var scoreTime: Date = scala.compiletime.uninitialized

  /** Maximum minutes allowed on this attempt. */
  @Column
  var maxMinutes: java.lang.Long = scala.compiletime.uninitialized

  /** the column containing a score, which may or may not be meaningful depending on {{state}} */
  @Column(columnDefinition = "JSONB")
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var score: JsonNode = scala.compiletime.uninitialized

  /** a column containing an object holding the questions selected for the attempt */
  @Column(nullable = false, columnDefinition = "JSONB")
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var questions: JsonNode = scala.compiletime.uninitialized

  /** a column containing an object holding the user responses for the attempt */
  @Column(nullable = false, columnDefinition = "JSONB")
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var responses: JsonNode = scala.compiletime.uninitialized

  /** the folder the attempt is in */
  @Column(name = "attemptfolder_id", nullable = false)
  var attemptFolderId: java.lang.Long = scala.compiletime.uninitialized

  @Column(name = "root_id", nullable = false)
  var rootId: java.lang.Long = scala.compiletime.uninitialized
end QuizAttemptEntity

object QuizAttemptEntity:
  import loi.cp.assessment.persistence.AssessmentDaoUtils.jsonNode

  def newAttempt(
    id: Long,
    rootId: Long,
    quiz: Quiz,
    user: UserDTO,
    createTime: Date,
    maxMinutes: Option[Long],
    questionsContainer: Seq[QuizAttemptQuestionUsage],
    responses: Seq[QuizQuestionResponse],
    attemptFolder: QuizAttemptFolderEntity
  ): QuizAttemptEntity =

    val attemptEntity: QuizAttemptEntity = new QuizAttemptEntity

    attemptEntity.id = id
    attemptEntity.rootId = rootId

    attemptEntity.attemptState = AttemptState.Open.entryName
    attemptEntity.valid = true
    attemptEntity.createTime = createTime
    attemptEntity.updateTime = createTime
    attemptEntity.maxMinutes = maxMinutes.map(Long.box).orNull

    attemptEntity.score = null
    attemptEntity.responses = jsonNode(responses)
    attemptEntity.questions = jsonNode(questionsContainer)

    attemptEntity.userId = user.id
    attemptEntity.commitId = quiz.assetReference.commit
    attemptEntity.nodeName = quiz.assetReference.nodeName.toString
    attemptEntity.contextId = quiz.contentId.contextId.value
    attemptEntity.edgePath = quiz.edgePath.toString
    attemptEntity.attemptFolderId = attemptFolder.id()

    attemptEntity
  end newAttempt

  implicit val quizAttemptyWempty: AttemptyWempty[QuizAttemptEntity] = new AttemptyWempty[QuizAttemptEntity]:
    override def updateTimeSql: String                      = "updateTime"
    override def valid(t: QuizAttemptEntity): Boolean       = t.valid
    override def attemptState(t: QuizAttemptEntity): String = t.attemptState
    override def submitTime(t: QuizAttemptEntity): Date     = t.submitTime
    override def updateTime(t: QuizAttemptEntity): Date     = t.updateTime
end QuizAttemptEntity

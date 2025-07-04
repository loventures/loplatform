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

package loi.cp.quiz.attempt

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.quiz.QuizAttemptEntity
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.assessment.AttemptId
import loi.cp.assessment.attempt.AttemptState
import loi.cp.quiz.Quiz
import scalaz.std.option.*
import scalaz.syntax.functor.*
import scaloi.syntax.option.*

import java.util.Date
import scala.jdk.CollectionConverters.*
import scala.util.Try

object QuizAttemptOps:
  import loi.cp.assessment.persistence.AssessmentDaoUtils.*

  implicit class AttemptOps(attempt: QuizAttempt):

    def toEntity: QuizAttemptEntity =
      val entity = new QuizAttemptEntity
      entity.id = attempt.id.value
      entity.attemptState = attempt.state.entryName
      entity.valid = attempt.valid
      entity.createTime = Date.from(attempt.createTime)
      entity.updateTime = Date.from(attempt.updateTime)
      entity.submitTime = attempt.submitTime.map(Date.from).orNull
      entity.autoSubmitted = attempt.maxMinutes.as(attempt.autoSubmitted).map(Boolean.box).orNull
      entity.scoreTime = attempt.scoreTime.map(Date.from).orNull
      entity.maxMinutes = attempt.maxMinutes.map(Long.box).orNull
      entity.score = attempt.score.map(jsonNode).orNull
      entity.questions = jsonNode(attempt.questions)
      entity.responses = jsonNode(attempt.responses)
      entity.attemptFolderId = attempt.folderId
      entity.rootId = attempt.rootId

      entity.contextId = attempt.contentId.contextId.value
      entity.edgePath = attempt.contentId.edgePath.toString
      entity.userId = attempt.user.id

      entity.nodeName = attempt.assessment.assetReference.nodeName.toString
      entity.commitId = attempt.assessment.assetReference.commit

      entity
    end toEntity
  end AttemptOps

  implicit class QuizAttemptEntityOps(entity: QuizAttemptEntity):
    def toAttempt(quiz: Quiz, user: UserDTO): QuizAttempt =
      Try({
        val mapper: ObjectMapper     = JacksonUtils.getMapper
        val score: Option[QuizScore] = Option(entity.score).map(mapper.treeToValue(_, classOf[QuizScore]))

        val questions: Seq[QuizAttemptQuestionUsage] =
          entity.questions.elements().asScala.toSeq.map(mapper.treeToValue(_, classOf[QuizAttemptQuestionUsage]))
        val responses: Seq[QuizQuestionResponse]     =
          entity.responses.elements().asScala.toSeq.map(mapper.treeToValue(_, classOf[QuizQuestionResponse]))

        val state: AttemptState = AttemptState.withName(entity.attemptState)

        QuizAttempt(
          id = AttemptId(entity.id),
          state = state,
          valid = entity.valid,
          createTime = entity.createTime.toInstant,
          updateTime = entity.updateTime.toInstant,
          submitTime = Option(entity.submitTime).map(_.toInstant),
          autoSubmitted = Option(entity.autoSubmitted).isTrue,
          maxMinutes = Option(entity.maxMinutes).map(_.longValue),
          score = score,
          questions = questions,
          responses = responses,
          scoreTime = Option(entity.scoreTime).map(_.toInstant),
          user = user,
          assessment = quiz,
          folderId = entity.attemptFolderId,
          rootId = entity.rootId
        )
      }).recover({ case th: Throwable =>
        throw new RuntimeException(s"toAttempt failure on quiz attempt entity ${entity.id}", th)
      }).get
  end QuizAttemptEntityOps
end QuizAttemptOps

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

package loi.cp.submissionassessment.attempt

import java.util
import java.util.Date
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.`type`.CollectionType
import com.learningobjects.cpxp.component.submissionassessment.SubmissionAttemptEntity
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.scala.util.JTypes.JLong
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.assessment.attempt.AttemptState
import loi.cp.assessment.{AttemptId, Feedback, ResponseScore}
import loi.cp.attachment.AttachmentId
import loi.cp.submissionassessment.SubmissionAssessment
import scaloi.syntax.CollectionBoxOps.*

import scala.jdk.CollectionConverters.*
import scala.util.Try

object SubmissionAssessmentAttemptOps:
  import loi.cp.assessment.persistence.AssessmentDaoUtils.*

  implicit class AttemptOps(attempt: SubmissionAttempt):
    def toEntity: SubmissionAttemptEntity =
      val entity = new SubmissionAttemptEntity

      entity.id = attempt.id.value
      entity.attemptFolderId = attempt.folderId
      entity.rootId = attempt.rootId

      entity.attemptState = attempt.state.entryName
      entity.valid = attempt.valid
      entity.createTime = Date.from(attempt.createTime)
      entity.responseTime = attempt.responseTime.map(Date.from).orNull
      entity.submitTime = attempt.submitTime.map(Date.from).orNull
      entity.scoreTime = attempt.scoreTime.map(Date.from).orNull
      entity.score = attempt.score.map(jsonNode).orNull
      entity.scorer = attempt.scorer.map(Long.box).orNull

      entity.essay = attempt.essay.orNull
      entity.attachments = jsonNode(attempt.attachments)

      entity.feedback = jsonNode(attempt.feedback)
      entity.feedbackReleased = attempt.feedbackReleased

      entity.userId = Long.box(attempt.user.id)
      entity.edgePath = attempt.contentId.edgePath.toString
      entity.contextId = attempt.contentId.contextId.value
      entity.nodeName = attempt.assessment.assetReference.nodeName.toString
      entity.commitId = attempt.assessment.assetReference.commit

      entity
    end toEntity
  end AttemptOps

  implicit class SubmissionAttemptEntityOps(entity: SubmissionAttemptEntity):
    def toAttempt(assessment: SubmissionAssessment, user: UserDTO): SubmissionAttempt =
      Try({
        val mapper: ObjectMapper         = JacksonUtils.getMapper
        val score: Option[ResponseScore] =
          Option(entity.score).map(mapper.treeToValue(_, classOf[ResponseScore]))

        val essay: Option[String]        = Option(entity.essay)
        val longListType: CollectionType =
          mapper.getTypeFactory.constructCollectionType(classOf[util.ArrayList[?]], classOf[JLong])

        val attachments: Seq[AttachmentId] =
          mapper
            .convertValue[util.List[JLong]](entity.attachments, longListType)
            .unboxInsideTo[Seq]()
            .map(AttachmentId(_))

        val state: AttemptState = AttemptState.withName(entity.attemptState)

        val feedbackListType: CollectionType =
          mapper.getTypeFactory.constructCollectionType(classOf[util.ArrayList[?]], classOf[Feedback])
        val feedback: Seq[Feedback]          =
          mapper.convertValue[util.List[Feedback]](entity.feedback, feedbackListType).asScala.toSeq

        SubmissionAttempt(
          id = AttemptId(entity.id),
          state = state,
          valid = entity.valid,
          createTime = entity.createTime.toInstant,
          responseTime = Option(entity.responseTime).map(_.toInstant),
          submitTime = Option(entity.submitTime).map(_.toInstant),
          scoreTime = Option(entity.scoreTime).map(_.toInstant),
          score = score,
          scorer = Option(entity.scorer).map(Long.unbox),
          essay = essay,
          attachments = attachments,
          feedback = feedback,
          feedbackReleased = entity.feedbackReleased,
          user = user,
          assessment = assessment,
          folderId = entity.attemptFolderId,
          rootId = entity.rootId
        )
      }).recover({ case th: Throwable =>
        throw new RuntimeException(s"toAttempt failure on submission attempt entity ${entity.id}", th)
      }).get
  end SubmissionAttemptEntityOps
end SubmissionAssessmentAttemptOps

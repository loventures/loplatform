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

import java.util.UUID

import argonaut.Argonaut.*
import argonaut.*
import cats.data.NonEmptyList
import com.learningobjects.cpxp.component.annotation.Service
import doobie.*

@Service
trait RsSurveyService:

  def loadResponseStats(
    sectionIds: NonEmptyList[Long],
  ): ConnectionIO[List[ResponseStats]]

  def loadQuestionStats(
    sectionIds: NonEmptyList[Long],
    contentEdgePath: NonEmptyList[String],
    essayResponsesLimit: Option[Int]
  ): ConnectionIO[List[QuestionStats]]

  def loadEssayResponses(
    questionAssetName: UUID,
    sectionIds: NonEmptyList[Long],
    contentEdgePaths: NonEmptyList[String],
    offset: Int = 0,
    limit: Option[Int] = None
  ): ConnectionIO[List[String]]
end RsSurveyService

final case class ResponseStats(
  edgePath: String,
  responseCounts: Long,
)

sealed trait QuestionStats:
  def questionName: UUID

final case class ChoiceQuestionStats(
  questionName: UUID,
  choiceCounts: List[ChoiceCount],
) extends QuestionStats

final case class EssayQuestionStats(
  questionName: UUID,
  responseCount: Long,
  responses: List[String],
) extends QuestionStats

object QuestionStats:

  implicit val encodeJsonForQuestionStats: EncodeJson[QuestionStats] = EncodeJson({
    case a: ChoiceQuestionStats =>
      Json(
        "questionName" := a.questionName,
        "choiceCounts" := a.choiceCounts,
      )
    case a: EssayQuestionStats  =>
      Json(
        "questionName"  := a.questionName,
        "responseCount" := a.responseCount,
        "responses"     := a.responses,
      )
  })
end QuestionStats

final case class ChoiceCount(
  choice: String,
  count: Long,
)

object ChoiceCount:
  implicit val encodeJsonForChoiceContent: EncodeJson[ChoiceCount] = EncodeJson(a =>
    Json(
      "choice" := a.choice,
      "count"  := a.count,
    )
  )

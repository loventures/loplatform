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

package loi.cp.analytics.event

import java.util.{Date, UUID}

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonInclude, JsonProperty}
import loi.cp.analytics.AnalyticsConstants
import loi.cp.analytics.AnalyticsConstants.EventActionType
import loi.cp.analytics.event.SurveySubmissionEvent.QuestionResponse1

object SurveySubmissionEvent:

  case class QuestionResponse1(
    questionAssetId: Long,
    response: String,
  )

@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class SurveySubmissionEvent2(
  id: UUID,
  time: Date,
  @JsonProperty(AnalyticsConstants.SESSION_KEY) session: Long,
  source: String,
  userId: Long,
  sectionId: Long,
  attemptId: UUID,
  contentAssetId: Long,
  contentEdgePath: String,
  surveyAssetId: Long,
  surveyEdgePath: String,
  responses: List[QuestionResponse1]
) extends Event:

  override val eventType: String                   = "SurveySubmissionEvent2"
  @JsonIgnore override val sessionId: Option[Long] = Some(session)
  override val actionType: EventActionType         = EventActionType.SUBMIT
end SurveySubmissionEvent2

/** Do not emit anymore. lacks the survey assetid and edgepath
  */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class SurveySubmissionEvent1(
  id: UUID,
  time: Date,
  @JsonProperty(AnalyticsConstants.SESSION_KEY) session: Long,
  source: String,
  userId: Long,
  sectionId: Long,
  attemptId: UUID,
  contentAssetId: Long,
  contentEdgePath: String,
  responses: List[QuestionResponse1]
) extends Event:

  override val eventType: String                   = "SurveySubmissionEvent1"
  @JsonIgnore override val sessionId: Option[Long] = Some(session)
  override val actionType: EventActionType         = EventActionType.SUBMIT
end SurveySubmissionEvent1

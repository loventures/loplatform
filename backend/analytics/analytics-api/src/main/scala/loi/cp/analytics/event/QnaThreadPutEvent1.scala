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

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonInclude}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import loi.cp.analytics.AnalyticsConstants.EventActionType

import java.time.Instant
import java.util.{Date, UUID}

// There should be four events, one for student create, one for instructor reply,
// one for student close and one for instructor recategorize. However, so much
// complexity is involved and some of these features are unsupported by the ui,
// so instead we have one event and rely on the use of options and coalesce to
// achieve the behaviour we want.
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class QnaThreadPutEvent1(
  id: UUID,
  @JsonDeserialize(contentAs = classOf[java.lang.Long]) session: Option[Long],
  source: String,
  time: Date,
  threadId: Long,
  questionId: Long,
  userId: Long,
  actualUserId: Option[Long],
  sectionId: Long,
  edgePath: String,
  // assetId: Long,
  createTime: Instant,
  @JsonDeserialize(contentAs = classOf[java.lang.Long]) instructorReplyUserId: Option[Long],
  instructorReplyTime: Option[Instant],
  instructorClosed: Boolean,
  studentClosed: Boolean,
  category: Option[String],
  subcategory: Option[String],
) extends Event:

  override val eventType: String           = "QnaThreadPutEvent1"
  override val actionType: EventActionType = EventActionType.PUT

  @JsonIgnore override val sessionId: Option[Long] = session
end QnaThreadPutEvent1

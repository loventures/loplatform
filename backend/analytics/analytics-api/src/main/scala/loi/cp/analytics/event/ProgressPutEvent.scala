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

import com.fasterxml.jackson.annotation.JsonIgnore
import loi.cp.analytics.AnalyticsConstants.EventActionType
import loi.cp.analytics.event.ProgressPutEvent.{PValue1, PValue2}

import java.util.{Date, UUID}
import scala.math.BigDecimal.RoundingMode

object ProgressPutEvent:

  final case class PValue2(
    edgePath: String,
    assetId: Long,
    completions: Int,
    total: Int,
    visited: Int,
    testedOut: Int,
    skipped: Int,
    forCreditGrades: Option[Int],
    forCreditGradesPossible: Option[Int],
  ):
    @JsonIgnore lazy val percentage: BigDecimal =
      if total == 0 then BigDecimal(0)
      else ((completions / BigDecimal(total)) * 100).setScale(2, RoundingMode.HALF_UP)
  end PValue2

  // legacy class, did not have skipped progress values
  final case class PValue1(
    edgePath: String,
    assetId: Long,
    completions: Int,
    total: Int,
    visited: Int,
    testedOut: Int
  ):
    @JsonIgnore lazy val percentage: BigDecimal =
      if total == 0 then BigDecimal(0)
      else ((completions / BigDecimal(total)) * 100).setScale(2, RoundingMode.HALF_UP)
  end PValue1
end ProgressPutEvent

/** @param maintenance
  *   true if this event is for Redshift maintenance, null otherwise
  */
final case class ProgressPutEvent2(
  id: UUID,
  time: Date,
  source: String,
  session: Long,
  sectionId: Long,
  userId: Long,
  values: List[PValue2],
  maintenance: Option[Boolean]
) extends Event:

  override val eventType: String                   = "ProgressPutEvent2"
  override val actionType: EventActionType         = EventActionType.PUT
  @JsonIgnore override val sessionId: Option[Long] = Some(session)
end ProgressPutEvent2

// legacy event, did not have skipped progress values
final case class ProgressPutEvent1(
  id: UUID,
  time: Date,
  source: String,
  session: Long,
  sectionId: Long,
  userId: Long,
  values: List[PValue1]
) extends Event:

  override val eventType: String                   = "ProgressPutEvent1"
  override val actionType: EventActionType         = EventActionType.PUT
  @JsonIgnore override val sessionId: Option[Long] = Some(session)
end ProgressPutEvent1

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

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonInclude, JsonProperty}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import loi.cp.analytics.AnalyticsConstants
import loi.cp.analytics.AnalyticsConstants.EventActionType
import loi.cp.analytics.event.PublishContentEvent.{Asset1, Content1}

import java.util.{Date, UUID}

object PublishContentEvent:

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  case class Asset1(
    id: Long,
    name: UUID,
    typeId: String,
    title: Option[String],
    keywords: Option[String],
    forCredit: Option[Boolean],
    pointsPossible: Option[java.math.BigDecimal]
  )

  /** @param learningPathIndex
    *   the index of this content in the list of the learning path. None for content outside the learning path, such as
    *   surveys. 0-based. The course is always 0.
    */
  // forCreditPointsPossible is not always asset pointspossible:
  //  - a container asset's fcpp is the sum of its children fcpp, whereas asset pp is null in such cases
  //  - a non-credit asset's fcpp is null, wheras asset pp may be Some
  //  - if/when analytics cares about Instructor Customisation, fcpp will may be a different value from asset pp
  case class Content1(
    assetId: Long,
    edgePath: String,
    learningPathIndex: Option[Int],
    forCreditPointsPossible: Option[BigDecimal],
    forCreditItemCount: Option[Int],
    // and now the ancestors of (assetid, edgepath):
    // `parent`, if Some, will always be one of `course`/`module`/`lesson`.
    // content outside of elements-elements-elements always have None for all four.
    parent: Option[SuperAssetId],
    course: Option[SuperAssetId],
    module: Option[SuperAssetId],
    lesson: Option[SuperAssetId],
  )

  case class SuperAssetId(assetId: Long, edgePath: String)
end PublishContentEvent

/** Event that occurs when a new content is pushed to an offering and sections (branch publish or branch
  * publish-udpate).
  *
  * @param sectionIds
  *   on first publish always empty, on publish-update likely to have some
  */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class PublishContentEvent1(
  id: UUID,
  time: Date,
  @JsonProperty(AnalyticsConstants.SESSION_KEY) session: Long,
  source: String,
  assets: List[Asset1],
  contents: List[Content1],
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  sectionIds: List[Long],
) extends Event:

  override val eventType: String                   = "PublishContentEvent1"
  @JsonIgnore override val sessionId: Option[Long] = Some(session)
  override val actionType: EventActionType         = EventActionType.UPDATE
end PublishContentEvent1

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

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

import java.util.Date

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME, // MINIMAL_CLASS has a leading period
  include = JsonTypeInfo.As.PROPERTY,
  property = "eventType",
  visible = true
)
@JsonSubTypes(
  Array(
    new Type(name = "PageNavFEEvent", value = classOf[PageNavFEEvent]),
    new Type(name = "CourseEntryFEEvent", value = classOf[CourseEntryFEEvent]),
    new Type(name = "QuestionViewedFEEvent", value = classOf[QuestionViewedFEEvent]),
    new Type(name = "TutorialViewFEEvent", value = classOf[TutorialViewFEEvent]),
  )
)
trait FrontEndEvent:
  val eventType: String
  val clientTime: Date
end FrontEndEvent

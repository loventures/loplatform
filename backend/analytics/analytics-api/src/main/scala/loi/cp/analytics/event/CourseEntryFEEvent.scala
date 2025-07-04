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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import loi.cp.analytics.entity.CourseId

import java.util.Date

// when you change this file, you need to change:
// - /cpv/course-lw/app/scripts/lof/src/js/analytics/dean.js
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class CourseEntryFEEvent(
  eventType: String,
  clientTime: Date,
  course: CourseId,
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  impersonatedId: Option[Long],
  userAgent: Option[String],
  framed: Option[Boolean],
) extends FrontEndEvent

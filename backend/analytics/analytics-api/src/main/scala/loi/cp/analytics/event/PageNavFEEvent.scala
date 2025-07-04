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
import loi.cp.analytics.entity.{ContentId, CourseId}

import java.util.Date

// when you change this file, you need to change:
// - /cpv/lof/src/js/analytics/AnalyticsProvider.js
// - /cpv/course-lw/lof/src/js/analytics/AnalyticsProvider.js
@JsonInclude(JsonInclude.Include.NON_EMPTY)
case class PageNavFEEvent(
  eventType: String,
  clientTime: Date,
  url: String,
  pageTitle: Option[String],
  course: Option[CourseId],
  content: Option[ContentId],
  impersonatedUserId: Option[Long],
  er: Boolean,
) extends FrontEndEvent

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

package loi.cp.lwgrade

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.annotation.Schema
import loi.cp.notification.Notification

/** A notification to a user that their grade on an item has been set by an instructor.
  */
@Schema(GradeNotification.Schema)
trait GradeNotification extends Notification:
  type Init = GradeNotification.Init

  @JsonProperty
  def title: String

object GradeNotification:

  // stored in DB
  final val Schema = "gradeNotification2"

  final case class Data(title: String, learnerId: Long)
  final case class Init(column: GradeColumn, learnerId: Long, contextId: Long)

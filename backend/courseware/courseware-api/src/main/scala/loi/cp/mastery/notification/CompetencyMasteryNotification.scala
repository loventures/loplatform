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

package loi.cp.mastery.notification

import com.fasterxml.jackson.annotation.JsonProperty
import com.learningobjects.cpxp.component.annotation.Schema
import com.learningobjects.cpxp.service.user.UserDTO
import loi.authoring.asset.Asset
import loi.cp.competency.Competency
import loi.cp.course.CourseSection
import loi.cp.notification.Notification

import java.time.Instant

/** A notification that a user has mastered a competency. */
@Schema("competencyMasteryNotification")
trait CompetencyMasteryNotification extends Notification:
  type Init = CompetencyMasteryNotification.Init

  /** The competency which the user has mastered. */
  @JsonProperty
  def getCompetency: Asset[?]

object CompetencyMasteryNotification:
  final case class Init(
    when: Instant,
    user: UserDTO,
    course: CourseSection,
    competency: Competency,
  )

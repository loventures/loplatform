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

package loi.cp.gatedate

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.course.lightweight.Lwc

@Service
trait GateDateSchedulingService:

  /** Schedule an appevent for the next gating date in the course. Does nothing for self-study.
    */
  def scheduleGateDateEvents(course: Lwc): Unit

  /** Schedule an appevent for the next gating date in the course for this user. Does nothing for instructor-led.
    */
  def scheduleGateDateEvents(course: Lwc, user: UserId): Unit
end GateDateSchedulingService

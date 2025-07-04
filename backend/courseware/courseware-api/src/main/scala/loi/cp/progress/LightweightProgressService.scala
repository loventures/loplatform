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

package loi.cp.progress

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.course.CourseSection
import loi.cp.lwgrade.StudentGradebook
import scalaz.\/

@Service
trait LightweightProgressService:

  def loadProgress(section: CourseSection, user: UserId, gradebook: StudentGradebook): ProgressMap

  def loadProgress(
    section: CourseSection,
    users: List[UserId],
    gradebooks: Map[UserId, StudentGradebook]
  ): Map[UserId, ProgressMap]

  /** @return
    *   `ProgressMap`s where `forCreditGrades` is always 0, but other fields are accurate
    */
  def loadVisitationBasedProgress(section: CourseSection, users: List[UserId]): Map[UserId, ProgressMap]

  /** @param changes
    *   changes to visitation-based progress
    */
  def updateProgress(
    section: CourseSection,
    user: UserId,
    gradebook: StudentGradebook,
    changes: List[ProgressChange]
  ): NonLeafProgressRequest \/ ProgressMap

  /** Schedules a background process to write refreshed progress documents for each learner in `section`. This should be
    * done whenever a content update occurs that affects progress, such as hiding an assessment in Instructor Controls.
    * Users never observe stale progress because the platform computes progress upon read when stale. This background
    * task is so that the reporting database is eventually consistent with the platform. Note that data in the reporting
    * database is used for other business transactions, such as awarding completion certificates.
    */
  // RIP "lightweight"
  def scheduleProgressUpdate(sectionId: Long): Unit

  def deleteProgress(section: CourseSection, user: UserId): Unit

  /** Creates a copy of a UserProgressEntity from srcCourse with dstCourse as the new contextId then `del`s the
    * srcCourse entry
    */
  def transferProgress(
    srcSection: CourseSection,
    dstSection: CourseSection,
    user: UserId,
    srcGradebook: StudentGradebook
  ): Unit
end LightweightProgressService

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

package loi.cp.learnertransfer

import com.learningobjects.cpxp.component.annotation.Service
import loi.cp.course.CourseSection
import loi.cp.enrollment.EnrollmentComponent
import loi.cp.user.UserComponent
import scalaz.{NonEmptyList, \/}

/** A service for transferring learners between sections.
  */
@Service
trait LearnerTransferService:

  /** Transfers a student's enrollment from sourceSectionId to destinationSectionId, along with their relevant work. Be
    * warned, a failed transfer leaves the transaction with uncommitted garbage so roll back! Roll back!
    *
    * @param userId
    *   the id of the learner.
    * @param sourceSectionId
    *   the pk of the section where the student is currently enrolled in.
    * @param destinationSectionId
    *   the pk of the section where the student has transferred to.
    */
  def transferLearner(
    userId: Long,
    sourceSectionId: Long,
    destinationSectionId: Long
  ): NonEmptyList[String] \/ CompletedLearnerTransfer
end LearnerTransferService

/** A completed learner transfer
  */
case class CompletedLearnerTransfer(
  student: UserComponent,
  source: CourseSection,
  destination: CourseSection,
  sourceEnrollment: EnrollmentComponent,
  destinationEnrollment: EnrollmentComponent,
)

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
import scalaz.ValidationNel

/** Service to validate a request to transfer a learner
  */
@Service
trait LearnerTransferValidator:

  /** Validates a learner transfer request, returning relevant entities if successful
    * @param studentId
    *   the id of the student to be transferred
    * @param sourceSectionId
    *   the id of the section that the student is currently enrolled in
    * @param destinationSectionId
    *   the id of the course that the student will be transferring into
    * @return
    *   An object containing the validated entities to be used to actually transfer the learner.
    */
  def validateTransfer(
    studentId: Long,
    sourceSectionId: Long,
    destinationSectionId: Long
  ): ValidationNel[LrnrXferValErr, ValidatedLearnerTransfer]
end LearnerTransferValidator

sealed trait LrnrXferValErr:
  val msg: String

object LrnrXferValErr:
  case class NoSuchUsr(id: Long)                                 extends LrnrXferValErr:
    val msg: String = s"User with id: $id doesn't exist."
  case class NoSuchSection(id: Long)                             extends LrnrXferValErr:
    val msg: String = s"Section with id: $id doesn't exist."
  case class SectionCommitsDiffer(srcId: Long, dstId: Long)      extends LrnrXferValErr:
    val msg: String = s"Sections $srcId and $dstId are from different commits."
  case class NoSuchStdntInSrcSection(stdntId: Long, srcId: Long) extends LrnrXferValErr:
    val msg: String = s"User with id: $stdntId is not a student in course with id $srcId, or is disabled."
  case class StdntAlreadyInDstSection(userId: Long, dstId: Long) extends LrnrXferValErr:
    val msg: String = s"User with id: $userId already in destination course $dstId"
end LrnrXferValErr

/** A validated representation of a learner transfer
  */
final case class ValidatedLearnerTransfer(
  student: UserComponent,
  source: CourseSection,
  destination: CourseSection,
  sourceEnrollment: EnrollmentComponent
)

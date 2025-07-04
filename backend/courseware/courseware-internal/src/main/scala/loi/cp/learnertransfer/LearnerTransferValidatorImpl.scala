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
import com.learningobjects.cpxp.component.{ComponentEnvironment, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.*
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import loi.cp.course.{CourseSection, CourseSectionService}
import loi.cp.enrollment.EnrollmentComponent
import loi.cp.learnertransfer.LrnrXferValErr.*
import loi.cp.role.RoleService
import loi.cp.user.UserComponent
import scalaz.syntax.applicative.*
import scalaz.syntax.std.list.*
import scalaz.syntax.std.option.*
import scalaz.syntax.validation.*
import scalaz.{Validation, ValidationNel}
import scaloi.syntax.`option`.*

import scala.jdk.CollectionConverters.*

/** The only implementation of [[LearnerTransferValidator]].
  */
@Service
class LearnerTransferValidatorImpl(
  ews: EnrollmentWebService,
  courseSectionService: CourseSectionService,
  roleService: RoleService
)(implicit cs: ComponentService, env: ComponentEnvironment)
    extends LearnerTransferValidator:
  private def studentRole = roleService.getRoleByItemId(ROLE_STUDENT_NAME)

  /** Validates a learner transfer request, returning relevant entities if successful
    *
    * @param studentId
    *   the id of the student to be transferred
    * @param sourceSectionId
    *   the id of the section that the student is currently enrolled in
    * @param destinationSectionId
    *   the id of the course that teh student will be transferring into
    * @return
    *   An object containing the validated entities to be used to actually transfer the learner.
    */
  override def validateTransfer(
    studentId: Long,
    sourceSectionId: Long,
    destinationSectionId: Long
  ): ValidationNel[LrnrXferValErr, ValidatedLearnerTransfer] =
    val vUsr = validateUser(studentId)
    val vSrc = validateSection(sourceSectionId)
    val vDst = validateSection(destinationSectionId)

    import scalaz.Validation.FlatMap.*
    val vUsrInSrc: ValidationNel[LrnrXferValErr, EnrollmentComponent]               = vUsr tuple vSrc flatMap { case (usr, src) =>
      validateUsrHasRoleInSrcCourse(usr, src)
    }
    val vUsrInDst: ValidationNel[LrnrXferValErr, Unit]                              = vUsr tuple vDst flatMap { case (usr, dst) =>
      validateUsrNotAlreadyInDstCourse(usr, dst)
    }
    val vSectnsMatch: ValidationNel[LrnrXferValErr, (CourseSection, CourseSection)] =
      vSrc tuple vDst flatMap { case (src, dst) =>
        validateLwCoursesAreFromSameCommit(src, dst)
      }

    /** Compose above validations with applicative |@|. Failures on left are accumulated. If no failures, construct a
      * VLT with the resulting tuple.
      */
    (vUsr |@| vSrc |@| vDst |@| vUsrInSrc |@| vUsrInDst |@| vSectnsMatch) { case (usr, src, dst, enrollment, _, _) =>
      ValidatedLearnerTransfer(usr, src, dst, enrollment)
    } leftMap (errNel =>
      // :( can we make a scalaz.Order or hashtypes for LrnrXfrErr
      errNel.list.toList.distinct.toNel.toOption.get
    )
  end validateTransfer

  def validateUser(userId: Long): ValidationNel[LrnrXferValErr, UserComponent] =
    userId.component_?[UserComponent] toSuccessNel NoSuchUsr(userId)

  def validateSection(section: Long): ValidationNel[LrnrXferValErr, CourseSection] =
    courseSectionService.getCourseSection(section, None) toSuccessNel NoSuchSection(section)

  def validateLwCoursesAreFromSameCommit(
    section1: CourseSection,
    section2: CourseSection
  ): ValidationNel[LrnrXferValErr, (CourseSection, CourseSection)] =
    if section1.commitId == section2.commitId then (section1, section2).successNel[LrnrXferValErr]
    else
      Validation.failureNel[LrnrXferValErr, (CourseSection, CourseSection)](
        SectionCommitsDiffer(section1.id, section2.id)
      )

  def validateUsrHasRoleInSrcCourse(
    user: UserComponent,
    section: CourseSection
  ): ValidationNel[LrnrXferValErr, EnrollmentComponent] =
    val userEnrollments = ews.getUserEnrollments(
      user.getId,
      section.getId,
      EnrollmentType.ACTIVE_ONLY
    ) // Only active students should be transferable.
    val studentEnrollment = userEnrollments.asScala.find(_.getRoleId == studentRole.getId)

    studentEnrollment
      .map(
        _.componentService[EnrollmentComponent]
      )
      .toSuccessNel[LrnrXferValErr](
        NoSuchStdntInSrcSection(
          user.getId,
          section.getId
        )
      )
  end validateUsrHasRoleInSrcCourse

  def validateUsrNotAlreadyInDstCourse(
    user: UserComponent,
    section: CourseSection
  ): ValidationNel[LrnrXferValErr, Unit] =
    val userEnrollments = ews.getUserEnrollments(user.getId, section.getId, EnrollmentType.ALL)
    userEnrollments.asScala
      .find(_.getRoleId == studentRole.getId)
      .thenInvalidNel(studentEnrollment => StdntAlreadyInDstSection(user.getId, section.getId))
end LearnerTransferValidatorImpl

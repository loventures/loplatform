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

package loi.cp.course

import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.user.UserId
import loi.cp.admin.right.{AdminRight, CourseAdminRight, IntegrationAdminRight}
import loi.cp.context.ContextId
import loi.cp.course.right.*
import loi.cp.right.Right
import loi.cp.user.UserComponent

/** A collection of methods allowing for querying of various user access levels in a course.
  */
// TODO: all of these methods use the `Current` user, since they go through the
// TODO: SRS `CollectedAuthorities machinery; refactor them to take a user param
@Service
trait CourseAccessService:
  import CourseAccessService.*

  def getUserHasAccess(course: ContextId): Boolean

  def getUserRights(course: ContextId)(implicit cs: ComponentService): Set[Class[? <: Right]] =
    getUserRights(course, Current.getUser.component[UserComponent])

  def getUserRights(
    course: ContextId,
    user: UserComponent,
  ): Set[Class[? <: Right]]

  /** Returns whether the current user has instructor access for this course (i.e., they can assign grades to users).
    *
    * @note
    *   that this includes admin-level access.
    *
    * @return
    *   whether the current user is an instructor
    */
  def hasInstructorAccess(course: ContextId): Boolean

  /** Returns whether the current user is an academic advisor.
    *
    * This check does not necessarily return true if the user is an instructor.
    *
    * @return
    *   whether the current user is an advisor
    */
  def hasAdvisorAccess(course: ContextId): Boolean

  /** Returns whether the current user has administrator access in this course.
    *
    * @return
    *   whether the current user is an administrator
    */
  def hasAdminAccess(course: ContextId): Boolean

  /** Returns whether the current user has unconditional access to all content in this course.
    *
    * @note
    *   this is controlled by [[loi.cp.course.right.ContentCourseRight]], which is automatically granted to instructors
    *   and administrators
    */
  def hasAllContentAccess(course: ContextId): Boolean

  /** Returns whether the user is any type of learner in the course.
    *
    * @return
    *   whether the current user is a learner in the course
    */
  def hasLearnerAccess(course: ContextId): Boolean

  /** Returns whether the user can read content in the course.
    *
    * @return
    *   whether the current user can read content in the course.
    */
  def hasReadAccess(course: ContextId): Boolean

  /** Returns whether the user can read interact/submit within in the course.
    *
    * @return
    *   whether the current user can interact/submit within in the course.
    */
  def hasInteractAccess(course: ContextId): Boolean

  /** Get the user's rights in this course, if any.
    *
    * @note
    *   This looks only at actual rights, it does not consider SRS collected authority
    *
    * @return
    *   the user rights, or [[None]] if they have none
    */
  def actualRights(course: ContextId, user: UserId): Option[CourseRights]

  /** Returns whether this user is a restricted learner in the specified context. Restricted learners have additional
    * policy restrictions such as no discussion board access.
    */
  def isRestrictedLearner(course: ContextId, user: UserId): Boolean
end CourseAccessService

object CourseAccessService:
  type AnyRight = Class[? <: Right]

  /** Container of course rights. The restricted flags mandates no access to communications like discussion boards.
    */
  final case class CourseRights(rights: Set[AnyRight], restricted: Boolean):

    /** Returns whether the current user has any form of instructor-like access for this course.
      */
    def likeInstructor: Boolean =
      hasAnyRight(InstructorLikeRights*)

    /** Messed up temporary LOV-162 support for RO admins viewing stuff. */
    def viewAllContent: Boolean =
      hasAnyRight(AlwaysViewRights*)

    /** Returns whether the current user has admin rights. */
    def isAdministrator: Boolean =
      hasAnyRight(AdministratorLikeRights*)

    /** Returns whether the current user is a trial learner. */
    def isTrialLearner: Boolean = // wtf is TrialContentRight
      !viewAllContent && !hasAnyRight(classOf[FullContentRight])

    // Unfortunately administrators don't magically have course rights...
    def hasRight(right: AnyRight): Boolean = rights.contains(right) ||
      classOf[CourseRight].isAssignableFrom(right) && isAdministrator

    private def hasAnyRight(rs: AnyRight*): Boolean =
      rs.exists(rights.contains)
  end CourseRights

  /** Users with these rights are administrator-like. */
  val AdministratorLikeRights: List[AnyRight] = List(
    classOf[AdminRight],
    classOf[CourseAdminRight],
    classOf[IntegrationAdminRight]
  )

  /** Users with these rights are instructor-like. */
  val InstructorLikeRights: List[AnyRight] =
    classOf[ViewCourseGradeRight] :: AdministratorLikeRights

  /** Users with these rights in a course may always view it. */
  val AlwaysViewRights: List[AnyRight] =
    classOf[ContentCourseRight] :: classOf[ManageCoursesReadRight] :: InstructorLikeRights
end CourseAccessService

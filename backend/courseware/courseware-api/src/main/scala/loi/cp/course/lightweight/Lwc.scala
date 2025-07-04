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
package lightweight

import com.learningobjects.cpxp.scala.cpxp.PK
import com.learningobjects.cpxp.service.enrollment.EnrollmentFacade
import loi.asset.course.model.Course
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.cp.context.{ContextId, WithContextId}
import scaloi.syntax.boolean.*

import java.lang
import java.time.Instant

/** An object that has data needed to load content for a lightweight course.
  */
trait Lwc extends WithContextId:

  /** the primary key of the course */
  def id: Long

  /** a unique string identifier for the group in its domain */
  def groupId: String

  /** an identifier provided for this course given by another LMS */
  def externalId: Option[String]

  /** whether multiple students or cohorts of students can enroll in this section in a rolling (consecutive and/or
    * concurrent) fashion
    */
  def rollingEnrollment: Boolean

  /** when this course is scheduled to start, if explicitly specified */
  def explicitStartDate: Option[Instant]

  /** when this course was created */
  def createDate: Instant

  /** the root asset of the course */
  def course: Asset[Course]

  /** the branch of the coruse */
  def branch: Branch

  /** the commit the course is on */
  def commitId: Long

  /** the generation, for caching purposes. If none, don't cache. */
  def generation: Option[Long]

  override def getId: lang.Long = id

  /** Returns when the course is listed as available. This may differ for users based on their enrollemnts.
    *
    * @return
    *   when the course is listed as available
    */
  def startDate: Option[Instant] =
    CourseStartDateUtils.effectiveSectionStartDate(rollingEnrollment, explicitStartDate, createDate)

  /** Returns when a user with the given enrollments first has access to the course.
    *
    * @param enrollments
    *   the users enrollments
    * @return
    *   when the course is first available to the user
    */
  def startDateForUser(enrollments: List[EnrollmentFacade]): Option[Instant] =
    CourseStartDateUtils.effectiveLearnerStartDate(rollingEnrollment, explicitStartDate, createDate, enrollments)
end Lwc

object Lwc:

  /** A [[Lwc]] has a database PK. */
  implicit val pk: PK[Lwc] = _.id

  /** Fabricate a [[Lwc]] from whole cloth. */
  def apply(
    id: Long,
    groupId: String,
    externalId: Option[String],
    selfStudy: Boolean,
    startDate: Option[Instant],
    createDate: Instant,
    course: Asset[Course],
    branch: Branch,
    commitId: Long,
    generation: Long
  ): Lwc =
    LwcImpl(id, groupId, externalId, selfStudy, startDate, createDate, course, branch, commitId, Some(generation))

  private final case class LwcImpl(
    id: Long,
    groupId: String,
    externalId: Option[String],
    rollingEnrollment: Boolean,
    explicitStartDate: Option[Instant],
    createDate: Instant,
    course: Asset[Course],
    branch: Branch,
    commitId: Long,
    generation: Option[Long]
  ) extends Lwc

  import language.implicitConversions
  implicit def toContextId(lwc: Lwc): ContextId = ContextId(lwc.id)
end Lwc

/** Utilities for determining when a course section is first generally available or first available to specific
  * learners. A learner may have first access for a section after the course becomes generally available due to
  * specified dates on their enrollment. Two learners may have differing start dates for the same section.
  */
private object CourseStartDateUtils:

  /** Returns the general availability date for the course. Absent any more specific specific information (such as
    * enrollments), this is when the course is first available. If enrollments exist, then use
    * {{effectiveLearnerStartDate}}.
    *
    * @param selfStudy
    *   whether or not the course is self study
    * @param explicitStartDate
    *   the fiated start date specified on the group for the section
    * @param createDate
    *   the date the group was created
    * @return
    *   the start date for a section with the given parameters
    */
  def effectiveSectionStartDate(
    selfStudy: Boolean,
    explicitStartDate: Option[Instant],
    createDate: Instant
  ): Option[Instant] =
    selfStudy.noption(courseStartDate(explicitStartDate, createDate))

  /** Returns when, for a specific learner, a course is first available. This may differ from the general availability
    * date for the course.
    *
    * @param selfStudy
    *   whether or not the course is self study
    * @param explicitStartDate
    *   the fiated start date specified on the group for the section
    * @param createDate
    *   the date the group was created
    * @param enrollments
    *   the enrollments the learner has in the specified class
    * @return
    */
  def effectiveLearnerStartDate(
    selfStudy: Boolean,
    explicitStartDate: Option[Instant],
    createDate: Instant,
    enrollments: List[EnrollmentFacade]
  ): Option[Instant] =
    if selfStudy then enrollmentStart(enrollments)
    else Some(courseStartDate(explicitStartDate, createDate))

  private def enrollmentStart(userEnrollments: List[EnrollmentFacade]): Option[Instant] =
    val enabledEnrollments: List[EnrollmentFacade] = userEnrollments.filter(!_.getDisabled)

    val maxStartTime: Option[Instant]  =
      enabledEnrollments.map(enrollment => Option(enrollment.getStartTime).map(_.toInstant)).max
    val maxCreateTime: Option[Instant] =
      enabledEnrollments.map(enrollment => Option(enrollment.getCreatedOn)).max

    maxStartTime.orElse(maxCreateTime)

  private def courseStartDate(
    explicitStartDate: Option[Instant],
    createDate: Instant
  ): Instant =
    explicitStartDate.getOrElse(createDate)
end CourseStartDateUtils

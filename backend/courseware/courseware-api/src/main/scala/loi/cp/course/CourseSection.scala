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

import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.service.enrollment.EnrollmentFacade
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType
import loi.asset.course.model.Course
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.cp.content.CourseContents
import loi.cp.content.DueDate
import loi.cp.context.ContextId
import loi.cp.course.lightweight.Lwc
import loi.cp.group.SectionGroup
import loi.cp.reference.EdgePath

import java.time.Instant
import java.lang as jl

/** The course section service object. The course section is the product of the course content and the customization of
  * the content of the course. The course section encompasses all the content, customization and generalized access
  * control information for the content. This does not contain any user specific views on the course, such as gating
  * status or due date exemptions.
  */
case class CourseSection(
  id: Long,
  groupId: String,
  externalId: Option[String],
  selfStudy: Boolean,
  explicitStartDate: Option[Instant],
  createDate: Instant,
  asset: Asset[Course],
  branch: Branch,
  commitId: Long,
  offeringId: Long,
  generation: Option[Long],
  contents: CourseContents,
  courseAvailabilityDates: Map[EdgePath, Instant],
  courseDueDates: Map[EdgePath, DueDate],
  groupType: GroupType,
) extends Id:
  self =>
  lazy val lwc: Lwc = new Lwc:
    override val id: Long                           = self.id
    override val groupId: String                    = self.groupId
    override val externalId: Option[String]         = self.externalId
    override def rollingEnrollment: Boolean         = self.selfStudy
    override def explicitStartDate: Option[Instant] = self.explicitStartDate
    override def createDate: Instant                = self.createDate
    override val course: Asset[Course]              = self.asset
    override def branch: Branch                     = self.branch
    override val commitId: Long                     = self.commitId
    override val generation: Option[Long]           = self.generation

  override def getId: jl.Long = self.id

  lazy val startDate: Option[Instant] = lwc.startDate

  def startDateForUser(enrollments: List[EnrollmentFacade]): Option[Instant] = lwc.startDateForUser(enrollments)

  /** Returns when a particular piece of content will be 'available', otherwise [[None]] if the content is not
    * temporally blocked. The availability date for content for a particular user may differ depending on enrollments
    * and overrides. Non-existent paths will return [[None]].
    *
    * @param edgePath
    *   the location of the content
    * @return
    *   the date the content at the given location will be available
    */
  def courseAvailabilityDate(edgePath: EdgePath): Option[Instant] =
    courseAvailabilityDates.get(edgePath)

  /** Returns when a particular piece of content will be due, otherwise [[None]] if the content has no due date. The
    * availability date for content for a particular user may differ depending on enrollments and overrides.
    * Non-existent paths will also return [[None]].
    *
    * @param edgePath
    *   the location of the content
    * @return
    *   the date the content at the given location will be available
    */
  def courseDueDate(edgePath: EdgePath): Option[Instant] =
    courseDueDates.get(edgePath)

  // you wish
  // coursewareApi cannot depend on gradeApi because gradeApi depends on coursewareApi
  // lazy val gradeStructure: GradeStructure = GradeStructure(contents)
end CourseSection

object CourseSection:

  def fromOldCode(
    lwc: SectionGroup,
    contents: CourseContents,
    courseAvailabilityDates: Map[EdgePath, Instant],
    courseDueDates: Map[EdgePath, DueDate]
  ): CourseSection = CourseSection(
    lwc.id,
    lwc.groupId,
    lwc.externalId,
    lwc.rollingEnrollment,
    lwc.explicitStartDate,
    lwc.createDate,
    lwc.course,
    lwc.branch,
    lwc.commitId,
    // TestSections have no offering. We should not be putting TestSections into
    // instances of CourseSection but I have other things to do.
    lwc.offeringId.getOrElse(0),
    lwc.generation,
    contents,
    courseAvailabilityDates,
    courseDueDates,
    lwc.groupType,
  )

  import language.implicitConversions

  implicit def toContextId(section: CourseSection): ContextId = ContextId(section.id)
end CourseSection

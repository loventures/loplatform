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

package loi.cp.group

import java.time.Instant

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserDTO
import enumeratum.{Enum, EnumEntry}
import loi.asset.course.model.Course
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.cp.course.lightweight.LightweightCourse
import scalaz.\/

/** A service for creating and retrieving groups (courses without content).
  */
@Service
trait LightweightGroupService:

  /** Returns the section group with the given id. [[Offering]] s will not be found by this service even if a valid
    * offering id is provided.
    *
    * @param id
    *   the pk of the section
    * @return
    *   the section group with the given id
    */
  def fetchSectionGroup(id: Long): Option[SectionGroup]

  /** Create a new section from the given offering and initialization parameters.
    *
    * @param offering
    *   the offering to provision from
    * @param implementation
    *   the course implementation to use
    * @param groupId
    *   the unique id for the section
    * @param name
    *   the user facing name of the section
    * @param selfStudy
    *   whether the course is considered self study
    * @param subtenantId
    *   the subtenant for this group, if any
    * @param creator
    *   the user creating the group
    * @param startDate
    *   the general start date for the course (enrollments can affect individual availability)
    * @param endDate
    *   the general date when users can no longer interact with the course
    * @param shutdownDate
    *   when everyone is locked out of the course
    * @return
    *   the newly created section
    */
  def createSectionGroup(
    offering: Offering,
    implementation: SectionGroupImplementation,
    groupId: String,
    name: String,
    selfStudy: Boolean,
    subtenantId: Option[Long],
    creator: UserDTO,
    startDate: Option[Instant],
    endDate: Option[Instant],
    shutdownDate: Option[Instant]
  ): NonUniqueGroupId \/ SectionGroup

  /** Returns the offering with the given id. [[SectionGroup]] s will not be found by this service even if a valid
    * section id is provided.
    *
    * @param id
    *   the pk of the offering
    * @return
    *   the offering for the given id
    */
  def fetchOffering(id: Long): Option[Offering]

  /** Returns the unique [[Offering]] for the given branch and content. If an offer for the given pair doesn't exist,
    * one is created. The offering will start at the most recent commit of the given branch.
    *
    * @param branch
    *   the branch to get the offering for
    * @param root
    *   the content to get the offering for
    * @param creator
    *   the creator if this creates an offering
    * @return
    *   the offering for the given pair
    */
  def getOrCreateOffering(branch: Branch, root: Asset[Course], creator: UserDTO): Offering
end LightweightGroupService

sealed abstract class SectionGroupImplementation(val componentId: String) extends EnumEntry

object SectionGroupImplementation extends Enum[SectionGroupImplementation]:
  val values = findValues

  def forComponentId(componentId: String): Option[SectionGroupImplementation] =
    values.find(_.componentId == componentId)

  def forComponentIdOrThrow(componentId: String): SectionGroupImplementation =
    forComponentId(componentId).getOrElse(
      throw new IllegalArgumentException(s"No known course implementation found for $componentId")
    )

  case object LightweightCourseImplementation extends SectionGroupImplementation(LightweightCourse.Identifier)
end SectionGroupImplementation

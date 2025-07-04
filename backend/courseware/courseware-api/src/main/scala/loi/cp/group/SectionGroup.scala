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

import com.learningobjects.cpxp.service.group.GroupConstants.GroupType
import loi.asset.course.model.Course
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.cp.context.ContextId
import loi.cp.course.lightweight.Lwc

import java.time.Instant

/** A service object representing the course section without content and other layerings.
  *
  * @param id
  *   the pk of the group
  * @param groupId
  *   the unique identifier of the group
  * @param externalId
  *   the identifier for the course from an external LMS
  * @param rollingEnrollment
  *   whether multiple students or cohorts of students can enroll in this section in a rolling (consecutive and/or
  *   concurrent) fashion
  * @param explicitStartDate
  *   the explicit date, if any, content is generally available
  * @param createDate
  *   the date the course was created
  * @param endDate
  *   the general end date for the course
  * @param shutdownDate
  *   the general shutdown date for the course
  * @param course
  *   the authored content of the course
  * @param branch
  *   the branch the course was provisioned from
  * @param commit
  *   the particular point in time the course is at
  * @param generation
  *   the iteration of content the course is on
  */
case class SectionGroup(
  id: Long,
  groupId: String,
  externalId: Option[String],
  rollingEnrollment: Boolean,
  explicitStartDate: Option[Instant],
  createDate: Instant,
  endDate: Option[Instant],
  shutdownDate: Option[Instant],
  course: Asset[Course],
  branch: Branch,
  commitId: Long,
  offeringId: Option[Long],
  generation: Option[Long],
  groupType: GroupType
) extends Lwc

object SectionGroup:
  import language.implicitConversions
  implicit def toContextId(sg: SectionGroup): ContextId = ContextId(sg.id)

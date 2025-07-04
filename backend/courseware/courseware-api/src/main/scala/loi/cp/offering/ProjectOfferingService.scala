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

package loi.cp.offering

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.query.QueryBuilder
import loi.asset.course.model.Course
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.authoring.project.Project
import loi.cp.course.CourseComponent
import loi.cp.course.lightweight.LightweightCourse

import java.time.Instant
import scala.util.Try

@Service
trait ProjectOfferingService:

  /** Push reindex messages for all non-suspended offerings. */
  def reindexAllOfferings(): Unit

  def analyzePublish(project: Project): PublishAnalysis

  def publishProject(project: Project): Unit

  def updateProject(project: Project, message: Option[String]): Int

  /** Counts the number of course sections that are associated with this branch. This does not include test or preview
    * sections.
    */
  def countCourseSections(branch: Branch): Try[Int]

  /** Returns the course offering for a branch. For a program this could be any course, under the assumption that the
    * data of interest is the same for all.
    */
  def getOfferingForBranch(branch: Branch): Option[CourseOfferingDto]

  /** Returns the course offering for a branch.
    *
    * @param branch
    *   - the branch to get the offering for
    * @return
    *   the component for that offering
    */
  def getOfferingComponentForBranch(branch: Branch): Option[LightweightCourse]

  def getCourseSections(offering: CourseComponent): List[LightweightCourse]

  /** Returns a course offering that matches an asset and branch.
    */
  def getOffering(course: Asset[Course], branch: Branch): Option[LightweightCourse]

  /** Return only those branch ids for which there is an active offering.
    */
  def offeredBranches(branches: Seq[Branch]): Set[Long]

  /** Cleanup after a project is deleted.
    */
  def deleteProject(project: Project): Unit

  private[offering] def queryOfferings: QueryBuilder

  private[offering] def invalidateOfferings(): Unit
end ProjectOfferingService

case class CourseOfferingDto(
  createdBy: Option[Long],
  created: Instant
)

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

import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.de.authorization.Secured
import kantan.csv.HeaderEncoder
import loi.authoring.security.right.CreateProjectRight
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.course.right.ManageLibrariesAdminRight
import scalaz.\/
import scaloi.json.ArgoExtras

import java.time.Instant
import java.util.UUID
import scala.compat.java8.OptionConverters.*

@Controller(value = "lwc-offerings", root = true)
@RequestMapping(path = "lwc/courseOfferings")
@Secured(Array(classOf[ManageLibrariesAdminRight]))
trait CourseOfferingWebController extends ApiRootComponent:
  import CourseOfferingWebController.*

  @RequestMapping(method = Method.GET, path = "")
  def getCourseOfferings(query: ApiQuery): ApiQueryResults[CourseOffering]

  @RequestMapping(method = Method.GET, path = "{id}")
  def getCourseOffering(@PathVariable("id") id: Long): Option[CourseOffering]

  @RequestMapping(method = Method.DELETE, path = "{id}")
  def deleteCourseOffering(@PathVariable("id") id: Long): ErrorResponse \/ Unit

  @RequestMapping(method = Method.GET, path = "{id}/ltiLaunchInfo")
  def getLtiLaunchInfo(@PathVariable("id") id: Long): ErrorResponse \/ Seq[LtiLaunchInfo]

  @RequestMapping(method = Method.GET, path = "{id}/links")
  def getActivityLinks(@PathVariable("id") id: Long, request: WebRequest): ErrorResponse \/ WebResponse

  @RequestMapping(method = Method.GET, path = "ltiLaunchInfo")
  def downloadLtiInfo(
    @QueryParam(value = "id", required = true, decodeAs = classOf[Long]) ids: List[Long],
    request: WebRequest
  ): ErrorResponse \/ WebResponse

  @RequestMapping(method = Method.PUT, path = "{id}/status")
  def updateStatus(@PathVariable("id") id: Long, @RequestBody status: OfferingStatus): ErrorResponse \/ OfferingStatus

  @RequestMapping(method = Method.GET, path = "branch/{branch}/sections/count")
  @Secured(
    value = Array(classOf[CreateProjectRight]),
    overrides = true
  ) // perhaps PublishOfferingRight but our roles and tests
  def countSections(@PathVariable("branch") branch: Long): ErrorResponse \/ Int
end CourseOfferingWebController

object CourseOfferingWebController:
  final case class CourseOffering(
    id: Long,
    name: String,
    createTime: Instant,
    disabled: Boolean,
    groupId: String,
    commit_id: Option[Long],
    project_id: Option[Long],
    project_name: Option[String] = None,
    projectCode: Option[String] = None,
    projectRevision: Option[Int] = None,
    projectProductType: Option[String] = None,
    projectCategory: Option[String] = None,
    projectSubCategory: Option[String] = None,
    version_id: Option[Long] = None,
    version_name: Option[String] = None,
    asset_guid: Option[UUID] = None,
  )

  object CourseOffering:
    def apply(lwc: LightweightCourse): CourseOffering =
      val branch = lwc.loadBranch()
      CourseOffering(
        id = Long.unbox(lwc.getId),
        name = lwc.getName,
        createTime = lwc.getCreateTime,
        disabled = lwc.getDisabled,
        groupId = lwc.getGroupId,
        commit_id = Some(lwc.commitId),
        project_id = lwc.getProjectId.asScala.map(Long.unbox),
        project_name = branch.project.map(_.name),
        projectCode = branch.project.flatMap(_.code),
        projectRevision = branch.project.flatMap(_.revision),
        projectProductType = branch.project.flatMap(_.productType),
        projectCategory = branch.project.flatMap(_.category),
        projectSubCategory = branch.project.flatMap(_.subCategory),
        version_id = Some(branch.id),
        version_name = Some(branch.name),
        asset_guid = Some(lwc.loadCourseName),
      )
    end apply
  end CourseOffering

  final case class LtiLaunchInfo(
    id: String,
    name: String,
    depth: Int,
    gradable: Option[Boolean]
  )

  final case class LtiLaunchCsvRow(
    offeringId: String,
    projectName: Option[String],
    // versionName: Option[String], Serena says "no"
    courseName: String,
    launchUrl: String
  )

  object LtiLaunchCsvRow:
    given HeaderEncoder[LtiLaunchCsvRow] =
      HeaderEncoder.caseEncoder("Offering Id", "Project Name", "Course Name", "Launch URL")(ArgoExtras.unapply)

  final case class ActivityCsvRow(
    name: String,
    url: String,
    graded: Boolean
  )

  object ActivityCsvRow:
    given HeaderEncoder[ActivityCsvRow] = HeaderEncoder.caseEncoder("Name", "LTI URL", "Graded")(ArgoExtras.unapply)

  final case class OfferingStatus(disabled: Boolean)
end CourseOfferingWebController

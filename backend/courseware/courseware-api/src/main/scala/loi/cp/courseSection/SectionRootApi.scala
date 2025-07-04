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

package loi.cp.courseSection

import java.lang as jl
import java.time.Instant
import java.util.UUID
import com.learningobjects.cpxp.component.annotation.{PathVariable, QueryParam, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.CourseAdminRight
import loi.cp.context.ContextId
import loi.cp.course.CourseComponent
import loi.cp.course.right.ManageCoursesReadRight
import scalaz.\/

/** Base web API for interacting with course sections.
  */
trait SectionRootApi extends ApiRootComponent:
  import SectionRootApi.*

  /** Add a course section. */
  @RequestMapping(method = Method.POST)
  @Secured(Array(classOf[CourseAdminRight]))
  def add(@RequestBody init: SectionInit): ErrorResponse \/ SectionDTO

  /** Query all the course sections. */
  @RequestMapping(method = Method.GET)
  @Secured(Array(classOf[CourseAdminRight], classOf[ManageCoursesReadRight]))
  def get(apiQuery: ApiQuery): ApiQueryResults[SectionDTO]

  /** Get a particular course section. */
  @RequestMapping(path = "{id}", method = Method.GET)
  @Secured(Array(classOf[CourseAdminRight], classOf[ManageCoursesReadRight]))
  def get(@PathVariable("id") id: Long): Option[SectionDTO]

  /** Delete a course section. */
  @RequestMapping(path = "{id}", method = Method.DELETE)
  @Secured(Array(classOf[CourseAdminRight]))
  def delete(@PathVariable("id") id: Long): ErrorResponse \/ Unit

  /** Delete a batch of course sections. */
  @RequestMapping(method = Method.DELETE)
  @Secured(Array(classOf[CourseAdminRight]))
  def deleteBatch(
    @QueryParam(value = "id", required = true, decodeAs = classOf[Long]) ids: List[Long]
  ): ErrorResponse \/ Unit

  /** Update a course section. */
  @RequestMapping(path = "{id}", method = Method.PUT)
  @Secured(Array(classOf[CourseAdminRight]))
  def update(
    @PathVariable("id") id: Long,
    @RequestBody update: SectionInit,
  ): ErrorResponse \/ SectionDTO

  /** Update a course section status. */
  @RequestMapping(path = "{id}/status", method = Method.PUT)
  @Secured(Array(classOf[CourseAdminRight]))
  def updateStatus(
    @PathVariable("id") id: Long,
    @RequestBody status: SectionStatus,
  ): ErrorResponse \/ SectionStatus
end SectionRootApi

object SectionRootApi:
  case class SectionStatus(disabled: Boolean)

  /* there should be one of these, and it should be parameterized over the
   * optionality of the fields, but Jackson dislikes that.
   */

  final case class SectionDTO(
    id: Long,
    createTime: Instant,
    disabled: Boolean,
    name: String,
    groupId: String,
    url: String,
    startDate: Option[Instant] = None,
    endDate: Option[Instant] = None,
    shutdownDate: Option[Instant] = None,
    configuredShutdownDate: Option[Instant] = None,
    externalId: Option[String] = None,
    subtenant_id: Option[Long] = None,
    course_id: Option[Long] = None,
    course_name: Option[String] = None,
    program_id: Option[Long] = None,
    project_id: Option[Long] = None,
    project_name: Option[String] = None,
    project_homeNodeName: Option[UUID] = None,
    projectCode: Option[String] = None,
    projectRevision: Option[Int] = None,
    projectProductType: Option[String] = None,
    projectCategory: Option[String] = None,
    projectSubCategory: Option[String] = None,
    version_id: Option[Long] = None,
    version: Option[String] = None,
    commit_id: Option[Long] = None,
    version_archived: Boolean = false,
    masterCourse_id: Option[Long] = None,
    integrations: Seq[IntegrationDTO] = Nil,
    fjœr: Boolean = false,
    updatable: Boolean = false,
    course_assetName: Option[UUID] = None,
    selfStudy: Boolean = false
  )

  import scala.language.implicitConversions

  extension (self: SectionDTO) implicit def contextId: ContextId = ContextId(self.id)

  final case class IntegrationDTO(
    id: Long,
    connector_id: Long,
    uniqueId: String
  )

  final case class SectionInit(
    name: String,
    groupId: String,
    startDate: Option[Instant] = None,
    endDate: Option[Instant] = None,
    shutdownDate: Option[Instant] = None,
    externalId: Option[String] = None,
    subtenant_id: Option[jl.Long] = None,
    // meh, can't update (only create)
    fjœr: Option[jl.Boolean] = None,
    masterCourse_id: Option[jl.Long] = None,
    uniqueIds: List[CourseComponent.UniqueId] = Nil,
    project_id: Option[jl.Long] = None,
    course_id: Option[jl.Long] = None,
    version_id: Option[jl.Long] = None,
    useOffering: Boolean = false,
    selfStudy: Boolean = false
  )
end SectionRootApi

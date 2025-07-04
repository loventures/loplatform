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

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonView}
import com.learningobjects.cpxp.component.ComponentInterface
import com.learningobjects.cpxp.component.annotation.{Controller, ItemMapping, Schema}
import com.learningobjects.cpxp.service.attachment.ImageFacade
import com.learningobjects.cpxp.service.group.GroupConstants
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.{Id, Url}
import com.learningobjects.de.group.GroupComponent
import com.learningobjects.de.web.{DeletableEntity, Queryable, QueryableProperties}
import loi.asset.course.model.Course
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.context.WithContextId
import loi.cp.course.lightweight.{LightweightCourse, Lwc}
import loi.cp.integration.IntegrationRootOwner
import loi.cp.right.Right
import loi.cp.user.{UniqueIdHandler, UserHandler}
import scalaz.\/

import java.time.Instant
import java.util.UUID
import javax.validation.groups.Default

@ItemMapping(GroupConstants.ITEM_TYPE_GROUP)
@Controller(value = "deCourse", category = Controller.Category.CONTEXTS)
@Schema("deCourse")
@QueryableProperties(
  Array(
    new Queryable(
      name = "uniqueId",
      handler = classOf[UniqueIdHandler],
      traits = Array(Queryable.Trait.NOT_SORTABLE, Queryable.Trait.CASE_INSENSITIVE)
    ),
    new Queryable(
      name = "user_id",
      handler = classOf[UserHandler],
      traits = Array(Queryable.Trait.NOT_SORTABLE)
    ),
    new Queryable(
      name = "project",
      dataType = GroupConstants.DATA_TYPE_GROUP_PROJECT,
      joinComponent = classOf[ProjectComponent]
    )
  )
)
trait CourseComponent
    extends GroupComponent
    with Id
    with WithContextId
    with Url
    with DeletableEntity
    with IntegrationRootOwner
    with Lwc:

  def loadCourse(): Asset[Course]

  def setCourseId(id: Long): Unit

  @JsonProperty("asset_guid")
  final def loadCourseName: UUID = loadCourse().info.name

  /** This course's generation.
    *
    * Increments any time a change is made to the course structure (by offering update or instructor customisation).
    */
  def getGeneration: Option[Long]

  def setGeneration(generation: Long): Unit

  def getOffering: LightweightCourse

  @JsonProperty("offering_id")
  def getOfferingId: Option[Long]

  def loadBranch(): Branch

  def setCommitId(id: Long): Unit

  @JsonIgnore
  def getWorkspace: AttachedReadWorkspace

  @JsonIgnore
  def getCreatedBy: Option[UserDTO]

  def isSelfStudy: Boolean

  def setSelfStudy(selfStudy: Boolean): Unit

  // is the current learner restricted from accessing communication tools in this course
  @JsonProperty
  def isRestricted: Boolean

  def getLogo: ImageFacade

  @JsonProperty
  def getStartDate: Option[Instant]

  def setStartDate(startDate: Option[Instant]): Unit

  @JsonProperty
  def getEndDate: Option[Instant]

  def setEndDate(endDate: Option[Instant]): Unit

  /** The shutdown date is the course end date, plus the amount of time allotted for the review period. This returns
    * either the course-specific shutdown or else the end date plus default review period.
    */
  @JsonProperty
  def getShutdownDate: Option[Instant]

  def setShutdownDate(shutdownDate: Option[Instant]): Unit

  /** The overridden shutdown date for this section, if configured.
    */
  @JsonView(Array(classOf[Default]))
  def getConfiguredShutdownDate: Option[Instant]

  @JsonProperty("subtenant_id")
  @Queryable(dataType = GroupConstants.DATA_TYPE_GROUP_SUBTENANT)
  def getSubtenantId: java.lang.Long

  def setSubtenant(subtenantId: java.lang.Long): Unit

  def getPreferences: CoursePreferences

  @JsonView(Array(classOf[Default]))
  @JsonProperty("hasStarted")
  def hasCourseStarted: Boolean

  @JsonView(Array(classOf[Default]))
  @JsonProperty("hasEnded")
  def hasCourseEnded: Boolean

  @JsonView(Array(classOf[Default]))
  @JsonProperty("hasShutdown")
  def hasCourseShutdown: Boolean

  @JsonProperty
  @Queryable(dataType = GroupConstants.DATA_TYPE_GROUP_ARCHIVED)
  def isArchived: java.lang.Boolean

  def setArchived(archived: java.lang.Boolean): Unit

  @SuppressWarnings(Array("unused")) // lohtml ffs
  def getUserRights: java.util.Set[Class[? <: Right]]

  // LWC implementations. I am not a fan, makes database loads look like properties
  override final def course: Asset[Course] = loadCourse()

  override final def generation: Option[Long] = getGeneration

  override final def branch: Branch = loadBranch()

  override final def rollingEnrollment: Boolean = isSelfStudy

  override final def explicitStartDate: Option[Instant] = getStartDate

  override final def createDate: Instant = getCreateTime
end CourseComponent

object CourseComponent:

  class Init(
    name: String,
    groupId: String,
    groupType: GroupConstants.GroupType,
    val createdBy: UserDTO,
    val source: LightweightCourse \/ (Asset[Course], Branch),
    disabled: Boolean = false,
    externalId: java.util.Optional[String] = java.util.Optional.empty(),
    val startDate: java.util.Optional[Instant] = java.util.Optional.empty(),
    val endDate: java.util.Optional[Instant] = java.util.Optional.empty(),
    val shutdownDate: java.util.Optional[Instant] = java.util.Optional.empty(),
  ) extends GroupComponent.Init(name, groupId, groupType, disabled, externalId)

  class UniqueId(
    val integrationId: java.lang.Long,
    val systemId: java.lang.Long,
    val uniqueId: String
  )
end CourseComponent

@ItemMapping("Project")
@QueryableProperties(
  Array(
    new Queryable(name = "code", dataType = "Project.code", traits = Array(Queryable.Trait.CASE_INSENSITIVE))
  )
)
trait ProjectComponent extends ComponentInterface

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

import com.learningobjects.cpxp.component.query.*
import com.learningobjects.cpxp.component.web.ErrorResponse
import com.learningobjects.cpxp.component.web.ErrorResponse.*
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.group.GroupConstants
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType
import com.learningobjects.cpxp.service.integration.{IntegrationFacade, IntegrationWebService}
import com.learningobjects.cpxp.service.user.UserDTO
import loi.asset.course.model.Course
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.authoring.node.AssetNodeService
import loi.authoring.project.{AccessRestriction, ProjectService}
import loi.authoring.workspace.service.ReadWorkspaceService
import loi.cp.analytics.CoursewareAnalyticsService
import loi.cp.course.CourseComponent.UniqueId
import loi.cp.course.*
import loi.cp.course.lightweight.{LightweightCourse, LightweightCourseService}
import loi.cp.gatedate.GateDateSchedulingService
import loi.cp.integration.IntegrationComponent
import loi.cp.offering.ProjectOfferingService
import scalaz.std.list.*
import scalaz.std.option.*
import scalaz.syntax.bitraverse.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scalaz.syntax.validation.*
import scalaz.{NonEmptyList, ValidationNel, \/}
import scaloi.syntax.any.*
import scaloi.syntax.boolean.*
import scaloi.syntax.option.*

import java.util as ju
import scala.PartialFunction.cond
import scala.compat.java8.OptionConverters.*
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

/** Base section root API implementation.
  */
abstract class SectionRootApiImpl(folderId: String)(implicit
  coursewareAnalyticsService: CoursewareAnalyticsService,
  fs: FacadeService,
  iws: IntegrationWebService,
  lwcs: LightweightCourseService,
  projectService: ProjectService,
  nodeService: AssetNodeService,
  workspaceService: ReadWorkspaceService,
  projectOfferingService: ProjectOfferingService,
  gateDateService: GateDateSchedulingService,
  user: UserDTO,
) extends SectionRootApi:

  import SectionRootApi.*
  import SectionRootApiImpl.*

  /** Add a section. */
  override def add(ini: SectionInit): ErrorResponse \/ SectionDTO =
    for
      init  <- toLwInit(ini)
      // TODO: guid/externalid/... clashes better
      folder = sectionFolder <| { _.lock(true) }
      _     <- folder.findCourseByGroupId(init.groupId).isPresent \/>! {
                 duplicateSectionError(init.groupId)
               }
      course = folder.addCourse(LightweightCourse.Identifier, init)
      // TODO: Make this less awful :(
      _      = // sundry side effects
        course.setExternalId(ini.externalId.asJava)
        (user.subtenantId orElse ini.subtenant_id).foreach { subtenantId =>
          course.setSubtenant(subtenantId)
        }
        if Option(ini.groupId).isEmpty && ini.externalId.isEmpty then
          course.setExternalId(init.groupId.jome) // if we give it an id give it an xid
      _     <- updateUniqueIds(course, Option(ini.uniqueIds).orZero)
    yield
      val lwc = course.component[LightweightCourse]
      lwc.setSelfStudy(ini.selfStudy)
      lwcs.initializeSection(lwc, Option(lwc.getOffering))

      toDto(course)

  private def toLwInit(ini: SectionInit): ErrorResponse \/ CourseComponent.Init =
    for
      projectId <- ini.project_id \/> badRequest("no project specified")
      branch     = projectService.loadProjectAsMasterBranch(projectId, AccessRestriction.none).get
      ws         = workspaceService.requireReadWorkspace(branch.id, AccessRestriction.none)
      course     = nodeService.loadA[Course](ws).byName(ws.homeName).get
      source    <- getSource(course, branch, !ini.useOffering) \/> badRequest("offering not found")
    yield
      val groupId = Option(ini.groupId) getOrElse ju.UUID.randomUUID.toString
      new CourseComponent.Init(
        name = ini.name,
        groupId = groupId,
        groupType = GroupType.forFolder(folderId),
        createdBy = user,
        source = source,
        startDate = ini.startDate.asJava,
        endDate = ini.endDate.asJava,
        shutdownDate = ini.shutdownDate.asJava,
      )

  private def getSource(
    course: Asset[Course],
    branch: Branch,
    nonOffering: Boolean
  ): Option[LightweightCourse \/ (Asset[Course], Branch)] =
    nonOffering
      .either((course -> branch).some)
      .or(projectOfferingService.getOffering(course, branch).map(_.component[LightweightCourse]))
      .bisequence[Option, LightweightCourse, (Asset[Course], Branch)]

  private def duplicateSectionError(gid: String): ErrorResponse =
    validationError("groupId", gid)("Duplicate section id")

  override def get(id: Long): Option[SectionDTO] =
    get(ApiQuery.byId(id, classOf[CourseComponent])).asOption

  // this oughta preload the integrations
  override def get(apiQuery: ApiQuery): ApiQueryResults[SectionDTO] =
    get0(apiQuery).flatMap(cc =>
      Try(toDto(cc)) match
        case Success(dto) => java.util.stream.Stream.of(dto)
        case Failure(ex)  =>
          logger.error(ex)(s"groupfinder ${cc.id} is broken")
          java.util.stream.Stream.empty();
    )

  private def get0(id: Long): Option[CourseComponent] =
    get0(ApiQuery.byId(id, classOf[CourseComponent])).asOption

  private def get0(apiQuery: ApiQuery): ApiQueryResults[CourseComponent] =
    ApiQuerySupport.query(sectionFolder.queryGroups, apiQuery, classOf[CourseComponent])

  /** Delete a section */
  override def delete(id: Long): ErrorResponse \/ Unit =
    for course <- get0(id) \/> ErrorResponse.notFound
    yield course.delete()

  /** Delete a batch of sections. */
  override def deleteBatch(ids: List[Long]): ErrorResponse \/ Unit =
    for courses <- ids.traverseU(id => get0(id) \/> ErrorResponse.notFound)
    yield courses.foreach(course => course.delete())

  /** Update a section. */
  override def update(id: Long, update: SectionInit): ErrorResponse \/ SectionDTO =
    for course <- get0(id) \/> ErrorResponse.notFound
    // TODO: guid/externalid/... clashes
    // TODO: subtenant id match
    yield
      val currentStartDate = course.getStartDate
      sectionFolder.invalidate()
      course.setGroupId(update.groupId)
      course.setName(update.name)
      course.setStartDate(update.startDate)
      course.setEndDate(update.endDate)
      course.setShutdownDate(update.shutdownDate)
      course.setExternalId(update.externalId.asJava)
      if user.subtenantId.isEmpty then
        // ???
        course.setSubtenant(update.subtenant_id.orNull)
      Option(update.uniqueIds) foreach { x =>
        updateUniqueIds(course, x)
      }

      course match
        case LightweightCourse(lwc) =>
          lwc.setSelfStudy(update.selfStudy)
          lwcs.updateSection(lwc)
          if currentStartDate != update.startDate then gateDateService.scheduleGateDateEvents(lwc)

        case _ =>

      if course.getGroupType == GroupType.CourseSection then coursewareAnalyticsService.emitSectionUpdateEvent(course)

      toDto(course)

  private def updateUniqueIds(
    course: CourseComponent,
    uids: List[UniqueId]
  ): ErrorResponse \/ Unit =
    val courseId = course.getId

    def checkNonDuplicate(uid: UniqueId): ValidationNel[UniqueId, Unit] =
      val existing = Option {
        iws.findByUniqueId(uid.systemId, uid.uniqueId, GroupConstants.ITEM_TYPE_GROUP)
      }
      existing match
        case None | Some(`courseId`) => ().successNel
        case _                       => uid.failureNel

    def toDuplicateError(duplicates: NonEmptyList[String]): ErrorResponse =
      validationError("uniqueId", duplicates)("Duplicate integration unique IDs")

    for _ <- uids
               .traverse(checkNonDuplicate)
               .toDisjunction
               .leftMap(_.map(_.uniqueId))
               .leftMap(toDuplicateError)
    yield
      val retain = uids.flatMap(uid => Option(uid.integrationId))
      iws
        .getIntegrationFacades(course.getId)
        .asScala
        .filter(facade => !retain.contains(facade.getId))
        .foreach(_.delete())
      uids foreach { uid =>
        val facade = Option(uid.integrationId).fold(iws.addIntegration(course.getId)) { id =>
          id.facade[IntegrationFacade]
        }
        facade.setExternalSystem(uid.systemId)
        facade.setUniqueId(uid.uniqueId)
      }
    end for
  end updateUniqueIds

  /** Update a section state. */
  override def updateStatus(id: Long, status: SectionStatus): ErrorResponse \/ SectionStatus =
    for course <- get0(id) \/> ErrorResponse.notFound
    yield
      sectionFolder.invalidate()
      course.setDisabled(status.disabled)
      if course.getGroupType == GroupType.CourseSection then coursewareAnalyticsService.emitSectionUpdateEvent(course)
      status

  private def sectionFolder = folderId.facade[CourseFolderFacade]
end SectionRootApiImpl

object SectionRootApiImpl:
  import SectionRootApi.*

  def toDto(cc: CourseComponent): SectionDTO =
    val branch  = cc.branch
    val project = branch.project
    val dto     = SectionDTO(
      id = cc.getId,
      createTime = cc.getCreateTime,
      disabled = Boxtion(cc.getDisabled).getOrElse(false),
      name = cc.getName,
      groupId = cc.getGroupId,
      url = cc.getUrl,
      startDate = cc.getStartDate,
      endDate = cc.getEndDate,
      shutdownDate = cc.getShutdownDate,
      configuredShutdownDate = cc.getConfiguredShutdownDate,
      externalId = cc.getExternalId.asScala,
      subtenant_id = Option(cc.getSubtenantId).map(Long.unbox),
      integrations = cc.getIntegrationRoot.getIntegrations(ApiQuery.ALL).asScala.toSeq.map(toIntegrationDto),
      fjœr = isFjøer(cc),
      version = Option(branch.name),
      version_id = Option(branch.id),
      version_archived = !branch.active,
      project_id = project.map(_.id),
      project_name = project.map(_.name),
      project_homeNodeName = project.map(_.homeNodeName),
      projectCode = project.flatMap(_.code),
      projectRevision = project.flatMap(_.revision),
      projectProductType = project.flatMap(_.productType),
      projectCategory = project.flatMap(_.category),
      projectSubCategory = project.flatMap(_.subCategory),
      commit_id = cc.getCommitId.asScala.map(Long.unbox)
    )
    cc match
      case LightweightCourse(lwc) =>
        val course = lwc.loadCourse()
        dto.copy(
          course_id = Some(course.info.id),
          course_name = Some(course.data.title),
          updatable = lwc.commitId != branch.head.id,
          course_assetName = Some(course.info.name),
          selfStudy = lwc.isSelfStudy
        )
      case _                      => dto
    end match
  end toDto

  private def isFjøer(cc: CourseComponent): Boolean = cond(cc) { case LightweightCourse(_) =>
    true
  }

  private def toIntegrationDto(i: IntegrationComponent): IntegrationDTO =
    IntegrationDTO(
      id = i.getId,
      connector_id = i.getSystemId,
      uniqueId = i.getUniqueId
    )

  private val logger: org.log4s.Logger = org.log4s.getLogger
end SectionRootApiImpl

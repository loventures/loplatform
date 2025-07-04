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

package loi.authoring.project.web

import argonaut.Argonaut.*
import argonaut.*
import com.google.common.annotations.VisibleForTesting
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.ApiQueries.ApiQueryOps
import com.learningobjects.cpxp.component.query.{
  ApiQuery,
  ApiQueryResults,
  ApiQuerySupport,
  BaseApiFilter,
  PredicateOperator
}
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainWebService}
import com.learningobjects.cpxp.service.exception.HttpApiException.*
import com.learningobjects.cpxp.service.user.{UserDTO, UserWebService}
import com.learningobjects.de.authorization.Secured
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.authoring.copy.CopyService
import loi.authoring.edge.Group
import loi.authoring.node.AssetNodeService
import loi.authoring.project.*
import loi.authoring.project.exception.*
import loi.authoring.security.right.*
import loi.authoring.user.exception.{NoSuchUserIdException, NoSuchUsernameException}
import loi.authoring.web.AuthoringWebUtils
import loi.authoring.workspace.WorkspaceService
import loi.authoring.write.*
import loi.cp.i18n.syntax.bundleMessage.*
import loi.cp.i18n.{AuthoringBundle, BundleMessage}
import loi.cp.right.RightService
import loi.cp.role.SupportedRoleService
import loi.cp.user.{UserComponent, UserService}
import scalaz.NonEmptyList
import scalaz.std.list.*
import scalaz.syntax.foldable.*
import scaloi.misc.TryInstances.*
import scaloi.syntax.option.*
import scaloi.syntax.validation.*

import java.util.UUID
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

/** Controller that can serve Project DTOs
  */
@Component
@Controller(root = true, value = "authoring/projects")
private[web] class ProjectWebController(
  val componentInstance: ComponentInstance,
  domainWebService: DomainWebService,
  userWebService: UserWebService,
  projectService: ProjectService,
  userDto: => UserDTO,
  webUtils: AuthoringWebUtils,
  workspaceService: WorkspaceService,
  writeService: WriteService,
  copyService: CopyService,
  currentDomain: => DomainDTO,
  projectDao2: ProjectDao2,
  commitDao2: CommitDao2,
  layeredWriteService: LayeredWriteService,
  rightService: RightService,
  supportedRoleService: SupportedRoleService,
  assetNodeService: AssetNodeService
)(implicit
  userService: UserService
) extends ApiRootComponent
    with ComponentImplementation:

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/authors", method = Method.GET)
  def getAuthors(query: ApiQuery): ApiQueryResults[UserComponent] =
    val roleIds      = for
      domainRole <- supportedRoleService.getRoles(currentDomain).asScala
      rights      = rightService.expandRightIds(domainRole.rightIdsJava)
      if rights.contains(classOf[AccessAuthoringAppRight])
    yield domainRole.roleType.id
    val authorFilter = new BaseApiFilter("domainRole", PredicateOperator.IN, roleIds.mkString(","))
    ApiQuerySupport.query(userService.queryActiveUsers, query.withPrefilter(authorFilter), classOf[UserComponent])
  end getAuthors

  /** Creates a project with initial version/branch--set as master branch.
    *
    * @param webDto
    *   new project data
    * @return
    *   created project
    */
  @Secured(Array(classOf[CreateProjectRight]))
  @RequestMapping(path = "authoring/projects", method = Method.POST)
  def createProject(
    @RequestBody webDto: CreateProjectRequest
  ): Try[ProjectsResponse] =
    val dto = CreateProjectDto(
      webDto.projectName,
      ProjectType.Course,
      userDto.id,
      webDto.code,
      webDto.productType,
      webDto.category,
      webDto.subCategory,
      webDto.revision,
      webDto.launchDate,
      webDto.liveVersion,
      webDto.s3,
      !webDto.layered.isFalse,
      webDto.projectStatus,
      webDto.courseStatus,
    )

    projectService
      .createProject(dto)
      .map(_._1)
      .fold(
        failMsgs => Failure(unprocessableEntity(nel2JList(failMsgs))),
        branch => Success(ProjectsResponse(Seq(branch)))
      )
  end createProject

  /** Gets the master branch of a single project.
    *
    * @param id
    *   project ID
    * @return
    *   project DTO, or exception
    */
  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects/{id}", method = Method.GET)
  def getProject(@PathVariable("id") id: Long): ProjectsResponse =
    val masterBranch = webUtils.masterOrFakeBranchOrThrow404(id, AccessRestriction.projectMember)
    ProjectsResponse(Seq(masterBranch))

  /** Gets all domain projects.
    *
    * @param archived
    *   whether to include archived projcets
    * @return
    *   collection of domain project DTOs
    */
  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects", method = Method.GET)
  def getProjects(
    @QueryParam(value = "archived", decodeAs = classOf[Boolean]) archived: Option[Boolean]
  ): ProjectsResponse =
    if archived.isTrue then throw badRequest(BundleMessage("project.archivedOnlyInvalid")(using AuthoringBundle.bundle))

    val branches = projectService.loadProjectsAsMasterBranches(notArchived = archived.isDefined)
    ProjectsResponse(branches)

  /** Updates project settings--currently, name and image.
    *
    * @param id
    *   project ID
    * @param webDto
    *   updated project data
    * @return
    *   project DTO
    */
  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects/{id}", method = Method.PUT)
  def putProjectSettings(
    @PathVariable("id") id: Long,
    @RequestBody webDto: PutProjectSettingsRequest
  ): Try[ProjectsResponse] =
    val project = webUtils.projectOrThrow404(id, AccessRestriction.projectMemberOr[EditSettingsAnyProjectRight])
    putProject(
      project,
      PutProjectSettingsDto(
        webDto.projectName,
        webDto.code,
        webDto.productType,
        webDto.category,
        webDto.subCategory,
        webDto.revision,
        webDto.launchDate,
        webDto.liveVersion,
        webDto.s3,
      )
    )
  end putProjectSettings

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects/{id}/status", method = Method.PUT)
  def putProjectStatus(
    @PathVariable("id") id: Long,
    @RequestBody liveVersion: Option[String]
  ): Try[ProjectsResponse] =
    val project = webUtils.projectOrThrow404(id, AccessRestriction.projectMemberOr[EditSettingsAnyProjectRight])
    val dto     = PutProjectSettingsDto(
      project.name,
      project.code,
      project.productType,
      project.category,
      project.subCategory,
      project.revision,
      project.launchDate,
      liveVersion,
      project.s3,
    )
    putProject(project, dto)
  end putProjectStatus

  private def putProject(project: Project, dto: PutProjectSettingsDto) =
    projectService
      .putProjectSettings(project, dto)
      .toTry(failures => unprocessableEntity(nel2JList(failures)))
      .map(prj1 =>
        val masterBranch = projectService.loadMasterBranch(prj1)
        ProjectsResponse(Seq(masterBranch))
      )

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects/{id}/archive", method = Method.POST)
  def archiveProject(@PathVariable("id") id: Long): ProjectsResponse = setProjectArchived(id, true)

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects/{id}/unarchive", method = Method.POST)
  def unarchiveProject(@PathVariable("id") id: Long): ProjectsResponse = setProjectArchived(id, false)

  private def setProjectArchived(id: Long, archived: Boolean): ProjectsResponse =
    val project      = webUtils.projectOrThrow404(id, AccessRestriction.projectMemberOr[EditSettingsAnyProjectRight])
    val projectᛌ     = projectService.setProjectArchived(project, archived)
    val masterBranch = projectService.loadMasterBranch(projectᛌ)
    ProjectsResponse(Seq(masterBranch))

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects/{id}/contributors", method = Method.PUT)
  def setContributors(
    @PathVariable("id") id: Long,
    @RequestBody contributors: ArgoBody[SetProjectContributorsRequest]
  ): ProjectsResponse =
    val project = webUtils.projectOrThrow404(id, AccessRestriction.projectMemberOr[EditContributorsAnyProjectRight])
    // This should be done with State but so awful
    val attempt = for
      request     <- contributors.decode_!
      project0    <- if request.owner == project.ownedBy then Success(project) else updateOwner(project, request.owner)
      project1    <-
        (project0.contributedBy.keySet &~ request.contributors.keySet).toList.foldLeftM(project0)(removeContributor)
      project2    <-
        (project1.contributedBy.keySet & request.contributors.keySet)
          .filter(u => project.contributedBy(u) != request.contributors(u))
          .toList
          .foldLeftM(project1)((p, u) => updateContributor(p, u, request.contributors(u)))
      project3    <-
        (request.contributors.keySet &~ project2.contributedBy.keySet).toList.foldLeftM(project2)((p, u) =>
          addContributor(p, u, request.contributors(u))
        )
      masterBranch = projectService.loadMasterBranch(project3)
    yield ProjectsResponse(Seq(masterBranch))

    attempt
      .recover({
        case ex: NotAValidAuthorException      => throw unprocessableEntity(ex)
        case ex: DuplicateContributorException => throw unprocessableEntity(ex)
        case ex: ContributorIsOwnerException   => throw unprocessableEntity(ex)
      })
      .get
  end setContributors

  private def updateOwner(project: Project, user: Long): Try[Project] =
    for
      owner   <- getUser(user)
      updated <- projectService.setOwnerForProject(project, owner)
    yield updated

  private def removeContributor(project: Project, user: Long): Try[Project] =
    for
      contributor <- getUser(user)
      updated     <- projectService.removeContributor(project, contributor)
    yield updated

  private def updateContributor(project: Project, user: Long, role: Option[String]): Try[Project] =
    for
      contributor <- getUser(user)
      updated     <- projectService.updateContributor(project, contributor, role)
    yield updated

  private def addContributor(project: Project, user: Long, role: Option[String]): Try[Project] =
    for
      contributor <- getUser(user)
      updated     <- projectService.addContributor(project, contributor, role)
    yield updated

  private def getUser(id: Long): Try[UserDTO] =
    Option(userWebService.getUserDTO(id)).toTry(NotAValidAuthorException(id))

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects/{id}/contributors", method = Method.POST)
  def addContributor(
    @PathVariable("id") id: Long,
    @RequestBody contributorInfo: AddProjectContributorRequest
  ): ProjectsResponse =
    val project = webUtils.projectOrThrow404(id, AccessRestriction.projectMemberOr[EditContributorsAnyProjectRight])
    val attempt = for
      user           <- userService.getUser(contributorInfo.user).toTry(NoSuchUsernameException(contributorInfo.user))
      updatedProject <- projectService.addContributor(project, user, contributorInfo.role)
      masterBranch    = projectService.loadMasterBranch(updatedProject)
    yield ProjectsResponse(Seq(masterBranch))

    attempt
      .recover({
        case ex: NoSuchUsernameException       => throw notFound(ex)
        case ex: DuplicateContributorException => throw unprocessableEntity(ex)
        case ex: ContributorIsOwnerException   => throw unprocessableEntity(ex)
      })
      .get
  end addContributor

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects/{id}/rehome", method = Method.POST)
  def setProjectHomeNode(
    @PathVariable("id") id: Long,
    @RequestBody homeNodeName: UUID
  ): ProjectsResponse =
    val project      = webUtils.projectOrThrow404(id, AccessRestriction.projectMemberOr[EditContentAnyProjectRight])
    val ws           = workspaceService.loadWriteWorkspace(project.id, AccessRestriction.none).get
    val delEdges     = ws.outEdgeInfos(project.rootName, Group.Courses).map(e => DeleteEdge(e.name)).toList
    val addEdge      = AddEdge(project.rootName, homeNodeName, Group.Courses)
    val setHome      = SetHomeName(homeNodeName)
    val seo          = SetEdgeOrder(project.rootName, Group.Courses, Seq(addEdge.name))
    val commitResult = writeService.commit(ws, setHome :: addEdge :: seo :: delEdges)

    val attempt = commitResult.map(result => ProjectsResponse(Seq(result.ws.branch)))

    attempt
      .recover({
        case ex: NoSuchUsernameException       => throw notFound(ex)
        case ex: DuplicateContributorException => throw unprocessableEntity(ex)
        case ex: ContributorIsOwnerException   => throw unprocessableEntity(ex)
      })
      .get
  end setProjectHomeNode

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects/{id}/contributors/{contributorId}", method = Method.DELETE)
  def removeContributor(
    @PathVariable("id") id: Long,
    @PathVariable("contributorId") contributorId: Long,
  ): Unit =
    val project = webUtils.projectOrThrow404(id, AccessRestriction.projectMemberOr[EditContributorsAnyProjectRight])
    val attempt = for
      user <- userService.getUser(contributorId).toTry(NoSuchUserIdException(contributorId))
      _    <- projectService.removeContributor(project, user)
    yield ()

    attempt
      .recover({
        case ex: NoSuchUserIdException    => throw notFound(ex)
        case ex: NotAContributorException => throw unprocessableEntity(ex)
      })
      .get
  end removeContributor

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects/{id}/contributors/{contributorId}", method = Method.PUT)
  def updateContributor(
    @PathVariable("id") id: Long,
    @PathVariable("contributorId") contributorId: Long,
    @RequestBody contributorInfo: ReroleProjectContributorRequest
  ): Unit =
    val project = webUtils.projectOrThrow404(id, AccessRestriction.projectMemberOr[EditContributorsAnyProjectRight])
    val attempt = for
      user <- userService.getUser(contributorId).toTry(NoSuchUserIdException(contributorId))
      _    <- projectService.updateContributor(project, user, contributorInfo.role)
    yield ()

    attempt
      .recover({
        case ex: NoSuchUserIdException    => throw notFound(ex)
        case ex: NotAContributorException => throw unprocessableEntity(ex)
      })
      .get
  end updateContributor

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects/{id}/owner", method = Method.PUT)
  def updateOwner(@PathVariable("id") id: Long, @RequestBody dto: TransferProjectOwnerRequest): ProjectsResponse =
    val project = webUtils.projectOrThrow404(id, AccessRestriction.projectMemberOr[ChangeOwnerAnyProjectRight])

    val attempt = for
      user           <- userService.getUser(dto.user).toTry(NoSuchUsernameException(dto.user))
      updatedProject <- projectService.setOwnerForProject(project, user)
      masterBranch    = projectService.loadMasterBranch(updatedProject)
    yield ProjectsResponse(Seq(masterBranch))

    attempt
      .recover({
        case ex: NoSuchUsernameException => throw notFound(ex)
        case ex: NotAValidOwnerException => throw unprocessableEntity(ex)
      })
      .get
  end updateOwner

  /** Gets all the branches of a project.
    *
    * @param id
    *   project ID
    * @return
    *   collection of project DTOs
    */
  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects/{id}/branches", method = Method.GET)
  def getProjectBranches(
    @PathVariable("id") id: Long
  ): ProjectsResponse =
    val project = webUtils.projectOrThrow404(id, AccessRestriction.projectMember)
    project match
      case p: Project2 => ProjectsResponse(Seq(p.asBranch))

  private def nel2JList[A](nel: NonEmptyList[A]): java.util.List[A] =
    nel.list.toList.asJava

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projectProps", method = Method.POST)
  def loadProjectProps(@RequestBody propsRequest0: ArgoBody[PropsRequest]): ArgoBody[PropsResponse] =

    val propsRequest = propsRequest0.decode_!.get // throws 422

    val values = projectService
      .loadProjectProps(propsRequest.property, propsRequest.startsWith)
      .leftMap(_ => List.empty[String])
      .toDisjunction
      .merge

    ArgoBody(PropsResponse(propsRequest.property, values))
  end loadProjectProps

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/admin/project", method = Method.POST)
  def copyProject(@RequestBody requestDto: ArgoBody[ProjectCopyRequest]): WebResponse =

    val webDto = requestDto.decode_!.get // ArgoParseException extends the 422 exception class

    val srcBranch = projectService
      .loadBronch(webDto.branchId)
      .getOrElse(AuthoringBundle.noSuchBranch(webDto.branchId).throw422)

    val newProjectName = webDto.projectName.trim
    if newProjectName.isEmpty then AuthoringBundle.projectCopyFailure("New name must not be empty").throw422
    else if newProjectName.length > 255 then
      AuthoringBundle.projectCopyFailure("New name must be less than 255 characters").throw422
    val createDto      = CreateProjectDto(
      newProjectName,
      ProjectType.Course,
      userDto.id,
      webDto.code,
      webDto.productType,
      webDto.category,
      webDto.subCategory,
      webDto.revision,
      webDto.launchDate,
      webDto.liveVersion,
      webDto.s3,
      srcBranch.layered,
      webDto.projectStatus,
      webDto.courseStatus,
    )
    webDto.targetDomain match
      case Some(tgtId) =>
        val tgtDomain = Option(domainWebService.getDomainDTO(tgtId))
          .getOrElse(AuthoringBundle.projectCopyFailure("Invalid domain").throw422)
        if !rightService.getUserHasRight(classOf[CopyAnyProjectVersionRight]) then
          AuthoringBundle.projectCopyFailure("User cannot cross domain copy").throw422
        EntityResponse(copyService.deepCopyProject(srcBranch, tgtDomain, newProjectName))

      case None =>
        val tgtBranch = copyService.shallowCopyProject(srcBranch, createDto)
        ArgoResponse(ShallowCopyResponse(tgtBranch))
    end match
  end copyProject

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects/{id}/dependencies", method = Method.GET)
  def getDependencies(
    @PathVariable("id") id: Long
  ): ProjectsResponse =
    val project = webUtils.projectOrThrow404(id, AccessRestriction.projectMemberOr[ViewAllProjectsRight])
    project match
      case project2: Project2 =>
        val head        = commitDao2.loadWithInitializedDocs(project2.head)
        val depProjects = projectDao2.load(head.comboDoc.deps.keys).map(_.asBranch)
        ProjectsResponse(depProjects)
      case _                  =>
        throw unprocessableEntity(AuthoringBundle.notLayered)
  end getDependencies

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects/{id}/dependencies/add", method = Method.POST)
  def addDependencies(
    @PathVariable("id") id: Long,
    @RequestBody webDto: ArgoBody[ProjectDependencyRequest]
  ): ProjectsResponse =
    processDependencies(
      id,
      webDto.decode_!.get.ids,
      (ws, project2) =>
        layeredWriteService
          .addDependency(ws, project2)
          .getOrElse(throw unprocessableEntity(AuthoringBundle.dependencyCycle))
    )

  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects/{id}/dependencies/sync", method = Method.POST)
  def syncDependencies(
    @PathVariable("id") id: Long,
    @RequestBody webDto: ArgoBody[ProjectDependencyRequest]
  ): ProjectsResponse =
    processDependencies(
      id,
      webDto.decode_!.get.ids,
      (ws, project2) => layeredWriteService.updateDependency(ws, project2.id).ws
    )

  @VisibleForTesting // one layered IT suite uses this
  @Secured(Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/projects/{id}/nodes", method = Method.GET)
  def getNodes(
    @PathVariable("id") id: Long
  ): List[Asset[?]] =
    val ws = webUtils.workspaceOrThrow404(id)
    assetNodeService.load(ws).all()

  private def processDependencies(
    projectId: Long,
    dependencyIds: List[Long],
    f: (LayeredWriteWorkspace, Project2) => LayeredWriteWorkspace
  ): ProjectsResponse =
    val project       = webUtils.projectOrThrow404(projectId, AccessRestriction.projectMemberOr[EditContentAnyProjectRight])
    val project2      = project match
      case project2: Project2 => project2
      case _                  => throw unprocessableEntity(AuthoringBundle.notLayered)
    val dependencies  =
      dependencyIds.map(id => webUtils.projectOrThrow404(id, AccessRestriction.projectMemberOr[ViewAllProjectsRight]))
    val dependencies2 = dependencies map {
      case project2: Project2 => project2
      case _                  => throw unprocessableEntity(AuthoringBundle.notLayered)
    }
    workspaceService.loadWriteWorkspace(project2.id) match
      case Some(ws: LayeredWriteWorkspace) =>
        dependencies2.foldLeft(ws)(f)
        ProjectsResponse(dependencies2.map(_.asBranch))
      case _                               =>
        throw unprocessableEntity(AuthoringBundle.notLayered)
  end processDependencies

  @Secured(value = Array(classOf[AccessAuthoringAppRight]))
  @RequestMapping(path = "authoring/project2s/bulk", method = Method.GET)
  def getProject2s(
    @QueryParam(value = "id", decodeAs = classOf[Long]) ids: List[Long],
  ): ProjectsResponse =
    val projects = projectDao2.load(ids)
    ProjectsResponse(projects.map(_.asBranch))
end ProjectWebController

case class ShallowCopyResponse(branchId: Long, homeNodeName: UUID)

object ShallowCopyResponse:
  implicit val codec: EncodeJson[ShallowCopyResponse] = CodecJson.derive[ShallowCopyResponse]

  def apply(branch: Branch): ShallowCopyResponse = ShallowCopyResponse(branch.id, branch.requireProject.homeName)

case class PropsRequest(property: String, startsWith: Option[String])

object PropsRequest:
  implicit val decodeJson: DecodeJson[PropsRequest] = DecodeJson.jdecode2L(PropsRequest.apply)("property", "startsWith")

case class PropsResponse(property: String, values: List[String])

object PropsResponse:
  implicit val encodeJson: EncodeJson[PropsResponse] = EncodeJson(a => Json("values" := a.values))

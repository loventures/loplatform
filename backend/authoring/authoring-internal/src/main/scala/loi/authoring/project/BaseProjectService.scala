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

package loi.authoring.project

import cats.syntax.option.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainFinder}
import com.learningobjects.cpxp.service.user.{UserDTO, UserFinder}
import com.learningobjects.cpxp.util.GuidUtil
import com.learningobjects.cpxp.util.HibernateSessionOps.*
import loi.asset.course.model.Course
import loi.asset.root.model.Root
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.authoring.edge.Group
import loi.authoring.index.{ProjectMetadata, ReindexService}
import loi.authoring.node.AssetNodeService
import loi.authoring.project.exception.*
import loi.authoring.security.right.{AccessAuthoringAppRight, ViewAllProjectsRight}
import loi.authoring.workspace.WorkspaceService
import loi.authoring.write.*
import loi.cp.config.ConfigurationKey
import loi.cp.i18n.BundleMessage
import loi.cp.user.UserService
import loi.jackson.syntax.jsonNode.*
import org.apache.commons.lang3.StringUtils
import org.hibernate.Session
import scalaz.ValidationNel
import scalaz.syntax.validation.*
import scaloi.syntax.BooleanOps.*
import scaloi.syntax.OptionOps.*

import java.time.LocalDateTime
import java.util.{Date, UUID}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

@Service
class BaseProjectService(
  workspaceService: WorkspaceService,
  projectDao2: ProjectDao2,
  commitDao2: CommitDao2,
  domainDto: => DomainDTO,
  userDto: => UserDTO,
  userService: UserService,
  writeService: WriteService,
  assetNodeService: AssetNodeService,
  reindexService: ReindexService,
  session: => Session,
  localDateTime: => LocalDateTime,
) extends ProjectService:

  override def createProject(
    dto: CreateProjectDto
  ): ValidationNel[BundleMessage, (Branch, Asset[Root])] =

    import scalaz.Validation.FlatMap.*

    for
      validDto <- validateForCreate(dto)
      branch   <- insertProject(validDto, domainDto.id, None, None)
    yield
      val ws        = workspaceService.loadWriteWorkspace(branch.id, AccessRestriction.none).get
      val addRoot   = AddNode(Root(ws.projectInfo.name, projectStatus = dto.projectStatus))
      val addCourse = AddNode(Course(ws.projectInfo.name, contentStatus = dto.courseStatus))
      val addEdge   = AddEdge(addRoot.name, addCourse.name, Group.Courses, position = Position.End.some)

      val ops    = List(addRoot, addCourse, addEdge, SetRootName(addRoot.name), SetHomeName(addCourse.name))
      val result = writeService.commit(ws, ops).get
      val root   = assetNodeService.loadA[Root](result.ws).byName(result.ws.rootName).get

      (result.ws.branch, root)
    end for
  end createProject

  // visible for deep project copy
  // visible for testing, many of which don't make root.1s, a few not even course.1s, lawless there
  def insertProject(
    dto: CreateProjectDto,
    rootId: Long,
    rootName: Option[UUID],
    homeName: Option[UUID]
  ): ValidationNel[BundleMessage, Branch] =
    validateForCreate(dto).map(validDto => insertLayeryProject(dto, rootId, rootName, homeName))

  private def insertLayeryProject(
    dto: CreateProjectDto,
    rootId: Long,
    rootName: Option[UUID],
    homeName: Option[UUID]
  ): Branch =
    val root      = session.ref[DomainFinder](rootId)
    val createdBy = session.ref[UserFinder](userDto.id)
    val ownedBy   = session.ref[UserFinder](dto.createdBy)

    val kfDoc = new CommitDocEntity(Commit2.Doc.empty, root)
    session.persist(kfDoc)
    val head  = new CommitEntity2(rootName, homeName, kfDoc, localDateTime, createdBy, root)
    session.persist(head)

    val project = new ProjectEntity2(
      dto.projectName,
      head,
      localDateTime,
      createdBy,
      ownedBy,
      dto.code.orNull,
      dto.productType.orNull,
      dto.category.orNull,
      dto.subCategory.orNull,
      dto.revision.map(int2Integer).orNull,
      dto.launchDate.orNull,
      dto.liveVersion.orNull,
      dto.s3.orNull,
      null,
      root
    )
    session.persist(project)

    project.toProject2.asBranch
  end insertLayeryProject

  override def loadProject(id: Long, accessRestriction: AccessRestriction): Option[Project] =

    val project = projectDao2.load(id)

    project.filter(accessRestriction.pass(userDto, _, userService))
  end loadProject

  override def loadProjects(excludeArchived: Boolean, checkMembership: Boolean): Seq[Project] =
    val userFilter = checkMembership && limitToContributorsAndOwners()

    val layeryProjects = projectDao2.loadAll(excludeArchived, userFilter)

    layeryProjects.sortBy(_.name)

  private def nameNotEmpty(name: String): ValidationNel[BundleMessage, String] =
    name.isEmpty.thenInvalidNel(ProjectError.NameRequired, name)

  /** Valid if
    *   - name is nonEmpty &&
    *   - matches current project name || project name does not match other project names Also trims name
    */
  private def validateForUpdate(
    existingProject: Project,
    dto: PutProjectSettingsDto
  ): ValidationNel[BundleMessage, PutProjectSettingsDto] =
    val trimmedNewName = StringUtils.truncate(StringUtils.trimToEmpty(dto.projectName), 255)
    if StringUtils.trimToEmpty(existingProject.name) == trimmedNewName then
      dto.copy(projectName = trimmedNewName).successNel[BundleMessage]
    else nameNotEmpty(trimmedNewName).map(_ => dto.copy(projectName = trimmedNewName))

  /** Valid if
    *   - name is nonEmpty &&
    *   - project name does not match other project names Also trims name
    */
  override def validateForCreate(
    dto: CreateProjectDto
  ): ValidationNel[BundleMessage, CreateProjectDto] =
    val trimmedNewName = StringUtils.trimToEmpty(dto.projectName)
    nameNotEmpty(trimmedNewName).map(_ =>
      val truncatedName = StringUtils.truncate(trimmedNewName, 255)
      dto.copy(projectName = truncatedName)
    )

  override def loadProjectProps(
    prop: String,
    startsWith: Option[String]
  ): ValidationNel[BundleMessage, List[String]] = for validProp <- ProjectDao2.ProjectProp.validate(prop)
  yield projectDao2.loadProjectProps(validProp, startsWith.map(_.trim))

  override def putProjectSettings(
    project: Project,
    dto: PutProjectSettingsDto
  ): ValidationNel[BundleMessage, Project] =
    validateForUpdate(project, dto).map { validDto =>
      project match
        case p: Project2 =>
          val entity  = projectDao2.loadReference(p)
          entity.name = validDto.projectName
          entity.code = validDto.code.orNull
          entity.productType = validDto.productType.orNull
          entity.category = validDto.category.orNull
          entity.subCategory = validDto.subCategory.orNull
          entity.revision = validDto.revision.map(int2Integer).orNull
          entity.launchDate = validDto.launchDate.orNull
          entity.liveVersion = validDto.liveVersion.orNull
          entity.s3 = validDto.s3.orNull
          val updated = entity.toProject2
          reindexService.projectMetadata(project.id, ProjectMetadata(updated))
          updated
    }

  override def setProjectArchived(project: Project, archived: Boolean): Project =
    reindexService.projectRetired(project.id, archived)
    project match
      case p: Project2 =>
        val entity = projectDao2.loadReference(p)
        entity.archived = archived
        entity.toProject2
    end match
  end setProjectArchived

  override def loadMasterBranch(project: Project): Branch =
    project match
      case p: Project2 => p.asBranch

  override def loadBronch(bronchId: Long, accessRestriction: AccessRestriction): Option[Branch] =
    val bronch = projectDao2.load(bronchId).map(_.asBranch)

    bronch.filter(b => accessRestriction.pass(userDto, b.requireProject, userService))
  end loadBronch

  override def loadProjectsAsMasterBranches(notArchived: Boolean): Seq[Branch] =
    val userFilter = limitToContributorsAndOwners()

    val fakeBranches = projectDao2.loadAll(notArchived, userFilter).map(_.asBranch)

    fakeBranches.sortBy(_.name)

  override def loadProjectAsMasterBranch(id: Long, accessRestriction: AccessRestriction): Option[Branch] =
    val branch = projectDao2.load(id).map(_.asBranch)

    branch.filter(b => accessRestriction.pass(userDto, b.requireProject, userService))
  end loadProjectAsMasterBranch

  override def addContributor(project: Project, contributor: UserDTO, role: Option[String]): Try[Project] =
    project match
      case p: Project2 =>
        val pEntity = projectDao2.loadReference(p)
        if pEntity.contributors.asScala.exists(_.user.getId == contributor.id) then
          Failure(DuplicateContributorException(contributor.userName))
        else if pEntity.ownedBy.getId == contributor.id then Failure(ContributorIsOwnerException(contributor.userName))
        else
          pEntity.addContributor(session.ref[UserFinder](contributor.id), role)
          Success(pEntity.toProject2)

  /** Sets a new owner on the project
    *
    * @param project
    *   the project to set the owner for
    * @param newOwner
    *   the user to set as the owner
    */
  override def setOwnerForProject(
    project: Project,
    newOwner: UserDTO
  ): Try[Project] =
    val validNewOwner = userService.userHasDomainRight[AccessAuthoringAppRight](newOwner.id)
    if validNewOwner then

      project match
        case p: Project2 =>
          val entity             = projectDao2.loadReference(p)
          val newOwnerUserFinder = session.ref[UserFinder](newOwner.id)
          entity.ownedBy = newOwnerUserFinder
          entity.removeContributor(newOwnerUserFinder)
          Success(entity.toProject2)
    else Failure(NotAValidOwnerException(newOwner.userName))
    end if
  end setOwnerForProject

  /** Removes a contributor from a project
    *
    * @param contributor
    *   - the contributor to remove
    * @param project
    *   - the project to remove the contributor from
    */
  override def removeContributor(
    project: Project,
    contributor: UserDTO
  ): Try[Project] =

    project match
      case p: Project2 =>
        val entity = projectDao2.loadReference(p)
        if entity.removeContributor(session.ref[UserFinder](contributor.id)) then Success(entity.toProject2)
        else Failure(NotAContributorException(contributor.userName))

  override def updateContributor(
    project: Project,
    contributor: UserDTO,
    role: Option[String],
  ): Try[Project] =
    project match
      case p: Project2 =>
        val entity = projectDao2.loadReference(p)
        entity.contributors.asScala.find(_.user.getId == contributor.id) match
          case Some(contributorEntity) =>
            contributorEntity.role = role.orNull
            Success(entity.toProject2)
          case None                    =>
            Failure(NotAContributorException(contributor.userName))

  /** Soft deletes a project. The project is recoverable
    *
    * @param project
    *   the project to soft delete
    */
  override def deleteProject(project: Project): Try[String] =
    reindexService.deleteProject(project.id)
    val delGuid = generateDeleteGuid()

    project match
      case p =>
        projectDao2.delete(p.id, delGuid)

    Success(delGuid)
  end deleteProject

  private def generateDeleteGuid() = s"${GuidUtil.temporalGuid(new Date())}/${userDto.id}"

  private def limitToContributorsAndOwners(): Boolean =
    // do not limit if user has view-all right
    !userService.userHasDomainRight[ViewAllProjectsRight](userDto.id)

  override def loadProjectByName(name: String, filterByCurrentUser: Boolean): Try[Project] =
    projectDao2
      .loadAll(activeFilter = true, userFilter = filterByCurrentUser)
      .find(p => p.name == name)
      .toTry(NoSuchProjectNameException(name))

  override def markPublished(project: Project): Unit =
    project match
      case p: Project2 =>
        val entity = projectDao2.loadReference(p)
        entity.published = true

  override def rewindHead(branch: Branch, commit: Long): Option[Long] =
    for ancestor <- commitDao2.loadAncestor(branch.head.id, commit)
    yield
      projectDao2.loadEntity(branch.id).get.head = ancestor
      ancestor.id

  override def getRawConfigJson[A](key: ConfigurationKey[A], projectId: Long): JsonNode =
    loadProject(projectId, AccessRestriction.none)
      .map(_.getRawConfigJson(key))
      .getOrElse(JsonNodeFactory.instance.objectNode())

  override def setRawConfigJson[A](key: ConfigurationKey[A], projectId: Long, config: JsonNode): Unit =
    updateRawConfigJson(projectId)(prevConfig =>
      Option(prevConfig)
        .flatMap(_.toObjNode)
        .getOrElse(JsonNodeFactory.instance.objectNode())
        .set[JsonNode](ConfigurationKey.configBinding(key.getClass).value(), config)
    )

  override def removeRawConfigJson[A](key: ConfigurationKey[A], projectId: Long): Unit =
    updateRawConfigJson(projectId)(prevConfig =>
      Option(prevConfig)
        .flatMap(_.toObjNode)
        .map(_.without[JsonNode](ConfigurationKey.configBinding(key.getClass).value()))
        .orNull
    )

  private def updateRawConfigJson(projectId: Long)(f: JsonNode => JsonNode): Unit =
    loadProject(projectId, AccessRestriction.none) foreach { case p2: Project2 =>
      val project2 = projectDao2.loadReference(p2)
      project2.configuration = f(project2.configuration)
    }
end BaseProjectService

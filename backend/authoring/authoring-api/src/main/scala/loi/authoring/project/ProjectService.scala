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

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserDTO
import loi.asset.root.model.Root
import loi.authoring.asset.Asset
import loi.authoring.branch.Branch
import loi.cp.config.ConfigurationKey
import loi.cp.i18n.BundleMessage
import scalaz.ValidationNel

import scala.util.Try

@Service
trait ProjectService:

  /** Sets a new owner on the rpject
    *
    * @param project
    *   the project to set the owner for
    * @param user
    *   the user to set as the owner
    */
  def setOwnerForProject(project: Project, user: UserDTO): Try[Project]

  /** Soft deletes a project. The project is recoverable
    *
    * @param project
    *   the project to soft delete
    */
  def deleteProject(project: Project): Try[String]

  /** Removes a contributor from a project
    *
    * @param contributor
    *   - the contributor to remove
    * @param project
    *   - the project to remove the contributor from
    */
  def removeContributor(project: Project, contributor: UserDTO): Try[Project]

  def updateContributor(project: Project, contributor: UserDTO, role: Option[String]): Try[Project]

  /** Adds a contributor to a project.
    *
    * @param project
    *   the project
    * @param contributor
    *   the contributor's user id
    * @return
    *   Success(Project) if user exists and is not already a contributor of the project
    */
  def addContributor(project: Project, contributor: UserDTO, role: Option[String]): Try[Project]

  /** Creates a new project with a master branch.
    *
    * @param dto
    *   webDto
    * @return
    *   the master branch of the new project
    */
  def createProject(dto: CreateProjectDto): ValidationNel[BundleMessage, (Branch, Asset[Root])]

  /** Loads a project
    *
    * @return
    *   the project
    */
  def loadProject(id: Long, accessRestriction: AccessRestriction = AccessRestriction.none): Option[Project]

  /** Bulk-load all projects
    *
    * @Param
    *   notArchived do not return archived projects
    * @return
    *   the projects
    */
  def loadProjects(excludeArchived: Boolean, checkMembership: Boolean = true): Seq[Project]

  /** Updates the project.
    *
    * @return
    *   the updated project
    */
  def putProjectSettings(project: Project, dto: PutProjectSettingsDto): ValidationNel[BundleMessage, Project]

  /** Archives or unarchives the project.
    *
    * @return
    *   the updated project
    */
  def setProjectArchived(project: Project, archived: Boolean): Project

  /** Loads the master branch of the project
    *
    * @return
    *   the master branch of the project
    */
  def loadMasterBranch(project: Project): Branch

  def loadBronch(bronchId: Long, accessRestriction: AccessRestriction = AccessRestriction.projectMember): Option[Branch]

  /** Loads the master branches of all projects. Since each project has exactly one master branch, you can get the
    * projects with `loadProjectsAsMasterBranches.map(_.project)`
    *
    * @param notArchived
    *   ignore archived projects
    * @return
    *   the master branches in this domain
    */
  def loadProjectsAsMasterBranches(notArchived: Boolean): Seq[Branch]

  def loadProjectAsMasterBranch(
    id: Long,
    accessRestriction: AccessRestriction = AccessRestriction.projectMember
  ): Option[Branch]

  /** Loads a project by name
    *
    * @return
    *   the project
    */
  def loadProjectByName(name: String, filterByCurrentUser: Boolean): Try[Project]

  def validateForCreate(dto: CreateProjectDto): ValidationNel[BundleMessage, CreateProjectDto]

  def loadProjectProps(prop: String, startsWith: Option[String]): ValidationNel[BundleMessage, List[String]]

  def markPublished(project: Project): Unit

  /** Set the branch head to the specified prior commit
    * @return
    *   the new head commit id (equal to branch.head.parentId). If None, then no rewind occurred because branch.head had
    *   no parent
    */
  def rewindHead(branch: Branch, commit: Long): Option[Long]

  // see CourseConfigurationService for non-raw and non-JsonNode config access
  def getRawConfigJson[A](key: ConfigurationKey[A], projectId: Long): JsonNode

  def setRawConfigJson[A](key: ConfigurationKey[A], projectId: Long, config: JsonNode): Unit

  def removeRawConfigJson[A](key: ConfigurationKey[A], projectId: Long): Unit
end ProjectService

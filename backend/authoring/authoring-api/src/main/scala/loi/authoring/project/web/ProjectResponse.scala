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

import com.learningobjects.cpxp.service.user.UserDTO
import loi.authoring.branch.{Branch, BranchType}
import loi.authoring.project.Project
import loi.cp.user.UserService

import java.util.Date

case class ProjectResponse(
  project: Project,
  branchId: Long,
  branchName: String,
  branchType: BranchType,
  branchCreated: Date,
  branchActive: Boolean,
  branchProvisionable: Boolean,
  ownerId: Long,
  contributors: Map[Long, Option[String]],
  headId: Long,
  headCreated: Date,
  headCreatedBy: Long,
  layered: Boolean,
)

object ProjectResponse:
  def apply(branch: Branch, project: Project): ProjectResponse =
    ProjectResponse(
      project = project,
      branchId = branch.id,
      branchName = branch.name,
      branchType = branch.branchType,
      branchCreated = branch.created,
      branchActive = branch.active,
      branchProvisionable = branch.provisionable,
      ownerId = project.ownedBy,
      contributors = project.contributedBy,
      headId = branch.head.id,
      headCreated = branch.head.createTime,
      headCreatedBy = branch.head.createUser,
      layered = branch.layered,
    )
end ProjectResponse

case class ProjectsResponse(
  projects: Seq[ProjectResponse],
  users: Map[Long, UserDTO]
)

object ProjectsResponse:

  def apply(branches: Seq[Branch])(implicit
    userService: UserService
  ): ProjectsResponse =

    val userIds = branches.view.flatMap(_.userIds).toSet
    val users   = userService.getUsers(userIds)

    val branchedProjects = branches.flatMap { branch =>
      branch.project.map { project => ProjectResponse(branch, project) }
    }
    ProjectsResponse(branchedProjects, users)
  end apply
end ProjectsResponse

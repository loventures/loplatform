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

package loi.authoring.project.exception

import com.learningobjects.de.web.UncheckedMessageException
import loi.cp.i18n.{AuthoringBundle, BundleMessage}

// todo: rename package to `project.error` because this PR is getting too big right now

case class NoSuchProjectIdException(id: Long)
    extends UncheckedMessageException(AuthoringBundle.message("project.noSuchProjectId", long2Long(id)))

case class MissingMasterBranchException(projectId: Long)
    extends UncheckedMessageException(
      AuthoringBundle.message("project.missingMasterBranch", long2Long(projectId))
    )

case object ProjectTypeRequiredException
    extends UncheckedMessageException(
      AuthoringBundle.message("project.required.projectType")
    )

case object ProjectBranchNameRequiredException
    extends UncheckedMessageException(
      AuthoringBundle.message("project.required.branchName")
    )

case class NoSuchBranchOnProjectException(branchId: Long, projectId: Long)
    extends UncheckedMessageException(
      AuthoringBundle.message("project.noSuchBranchIdOnProjectId", long2Long(branchId), long2Long(projectId))
    )

case class NoSuchBranchException(branchId: Long)
    extends UncheckedMessageException(
      AuthoringBundle.message("project.noSuchBranchId", long2Long(branchId))
    )

case class ProjectHasNoImage(projectId: Long)
    extends UncheckedMessageException(
      AuthoringBundle.message("project.hasNoImage", long2Long(projectId))
    )

case class NotAProjectBranchException(branchId: Long)
    extends UncheckedMessageException(
      AuthoringBundle.message("project.branchMissingProject", long2Long(branchId))
    )

case class NotACourseProjectException(projectId: Long)
    extends UncheckedMessageException(
      AuthoringBundle.message("project.notACourseProject", long2Long(projectId))
    )

case class NoSuchProjectNameException(name: String)
    extends UncheckedMessageException(AuthoringBundle.message("project.noSuchProjectName", name))

case class DuplicateContributorException(contributorUserName: String)
    extends UncheckedMessageException(AuthoringBundle.message("project.duplicateContributor", contributorUserName))

case class NotAContributorException(contributorUserName: String)
    extends UncheckedMessageException(AuthoringBundle.message("project.notAContributor", contributorUserName))

case class ContributorIsOwnerException(contributorUserName: String)
    extends UncheckedMessageException(AuthoringBundle.message("project.contributorIsOwner", contributorUserName))

case class NotAValidOwnerException(newOwnerName: String)
    extends UncheckedMessageException(AuthoringBundle.message("project.notAValidOwner", newOwnerName))

case class NotAValidAuthorException(id: Long)
    extends UncheckedMessageException(AuthoringBundle.message("project.notAValidAuthor", Long.box(id)))

// Client errors, not real exceptions
// todo: move other things called exceptions ^ to here
object ProjectError:
  val NameRequired: BundleMessage              = AuthoringBundle.message("project.required.name")
  def invalidProp(prop: String): BundleMessage = AuthoringBundle.message("project.invalidProp", prop)

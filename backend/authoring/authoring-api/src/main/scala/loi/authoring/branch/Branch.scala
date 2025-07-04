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

package loi.authoring.branch

import argonaut.StringWrap.*
import argonaut.{EncodeJson, Json}
import loi.authoring.commit.Commit
import loi.authoring.project.Project
import loi.authoring.project.exception.NotAProjectBranchException
import scaloi.json.ArgoExtras.*

import java.util.Date

case class Branch(
  id: Long,
  name: String,
  // this should be non-optional when all domains are migrated to projects.
  // (hehe, "when"...)
  project: Option[Project],
  head: Commit,
  branchType: BranchType,
  created: Date,
  active: Boolean,
  provisionable: Boolean,
):
  def requireProject: Project = project.getOrElse(throw NotAProjectBranchException(id))

  // head's creator is included because it could be a superuser who isn't an owner/contributor
  lazy val userIds: Set[Long] = project.map(_.userIds).getOrElse(Set.empty) + head.createUser

  val layered: Boolean = project.map(_.id).contains(id)
end Branch

object Branch:

  implicit final val encodeJsonForBranch: EncodeJson[Branch] = EncodeJson(b =>
    Json(
      "id"            := b.id,
      "name"          := b.name,
      "project"       := b.project,
      "head"          := b.head,
      "branchType"    := b.branchType,
      "created"       := b.created,
      "active"        := b.active,
      "provisionable" := b.provisionable,
      "layered"       := b.layered,
    )
  )
end Branch

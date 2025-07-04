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

import argonaut.*
import scaloi.json.ArgoExtras.*
import java.time.LocalDate

/** Dedicated to web clients. Contrast with loi.authoring.project.projectDtos.scala. But can't set to private[web]
  * because BranchWebController also uses this. Todo: Make Jackson/Fiantra Jackson ser/deser tests.
  */

case class CreateProjectRequest(
  projectName: String,
  code: Option[String],
  productType: Option[String],
  category: Option[String],
  subCategory: Option[String],
  revision: Option[Int],
  launchDate: Option[LocalDate],
  liveVersion: Option[String],
  s3: Option[String],
  projectStatus: Option[String],
  courseStatus: Option[String],
  layered: Option[Boolean],
)

case class PutProjectSettingsRequest(
  projectName: String,
  code: Option[String],
  productType: Option[String],
  category: Option[String],
  subCategory: Option[String],
  revision: Option[Int],
  launchDate: Option[LocalDate],
  liveVersion: Option[String],
  s3: Option[String],
)

case class SetProjectContributorsRequest(
  owner: Long,
  contributors: Map[Long, Option[String]]
)

object SetProjectContributorsRequest:
  implicit val codec: CodecJson[SetProjectContributorsRequest] = CodecJson.derive[SetProjectContributorsRequest]

case class AddProjectContributorRequest(
  user: String,
  role: Option[String],
)

case class ReroleProjectContributorRequest(
  role: Option[String],
)

case class TransferProjectOwnerRequest(
  user: String
)

case class ProjectCopyRequest(
  branchId: Long,
  targetDomain: Option[Long],
  projectName: String,
  code: Option[String],
  productType: Option[String],
  category: Option[String],
  subCategory: Option[String],
  revision: Option[Int],
  launchDate: Option[LocalDate],
  liveVersion: Option[String],
  s3: Option[String],
  projectStatus: Option[String],
  courseStatus: Option[String],
)

object ProjectCopyRequest:
  implicit val codec: CodecJson[ProjectCopyRequest] = CodecJson.derive[ProjectCopyRequest]

final case class ProjectDependencyRequest(
  ids: List[Long]
)

object ProjectDependencyRequest:
  implicit val codec: CodecJson[ProjectDependencyRequest] = CodecJson.derive[ProjectDependencyRequest]

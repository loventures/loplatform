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

import java.time.LocalDate

/** Internal. Contrast with loi.authoring.project.web.projectWebDtos.scala.
  */
case class CreateProjectDto(
  projectName: String,
  projectType: ProjectType,
  createdBy: Long,
  code: Option[String] = None,
  productType: Option[String] = None,
  category: Option[String] = None,
  subCategory: Option[String] = None,
  revision: Option[Int] = None,
  launchDate: Option[LocalDate] = None,
  liveVersion: Option[String] = None,
  s3: Option[String] = None,
  layered: Boolean = false,
  projectStatus: Option[String] = None,
  courseStatus: Option[String] = None,
)

case class PutProjectSettingsDto(
  projectName: String,
  code: Option[String] = None,
  productType: Option[String] = None,
  category: Option[String] = None,
  subCategory: Option[String] = None,
  revision: Option[Int] = None,
  launchDate: Option[LocalDate] = None,
  liveVersion: Option[String] = None,
  s3: Option[String] = None,
)

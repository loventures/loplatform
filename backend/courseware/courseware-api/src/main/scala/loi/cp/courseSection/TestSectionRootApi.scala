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

import com.google.common.annotations.VisibleForTesting
import com.learningobjects.cpxp.component.annotation.{Controller, RequestBody, RequestMapping}
import com.learningobjects.cpxp.component.web.{ErrorResponse, Method}
import com.learningobjects.de.authorization.Secured
import loi.cp.admin.right.CourseAdminRight
import loi.cp.courseSection.CourseSectionRootApi.ITSection
import loi.cp.courseSection.SectionRootApi.SectionDTO
import scalaz.\/

/** The root web API for interacting with test sections.
  */
@Controller(value = "testSections", root = true)
@RequestMapping(path = "testSections")
trait TestSectionRootApi extends SectionRootApi:

  /** Create a new empty course section with associated project. Intended for use by integration tests. */
  @VisibleForTesting
  @RequestMapping(method = Method.POST, path = "it")
  @Secured(Array(classOf[CourseAdminRight]))
  def emptySection(@RequestBody it: ITSection): ErrorResponse \/ SectionDTO

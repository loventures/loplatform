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

package loi.cp.course.preview

import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService.{INSTRUCTOR_ROLE_ID, STUDENT_ROLE_ID}
import enumeratum.{Enum, EnumEntry}

sealed abstract class PreviewRole(val roleId: String) extends EnumEntry

object PreviewRole extends Enum[PreviewRole]:
  override def values: IndexedSeq[PreviewRole] = findValues

  case object Instructor extends PreviewRole(INSTRUCTOR_ROLE_ID)
  case object Learner    extends PreviewRole(STUDENT_ROLE_ID)

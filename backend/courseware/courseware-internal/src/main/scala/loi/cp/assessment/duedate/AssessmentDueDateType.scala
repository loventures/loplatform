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

package loi.cp.assessment.duedate

import enumeratum.{ArgonautEnum, Enum, EnumEntry}

/** The type of behavior the due date should have
  */
sealed abstract class AssessmentDueDateType extends EnumEntry

case object AssessmentDueDateType extends Enum[AssessmentDueDateType] with ArgonautEnum[AssessmentDueDateType]:
  val values = findValues

  /** the due date is more of a suggestion than a strict due date */
  case object TargetDateType extends AssessmentDueDateType

  /** no modifications or submissions can be made after the due date */
  case object DeadlineType extends AssessmentDueDateType

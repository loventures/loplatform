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

package loi.cp.lti.role

import enumeratum.{Enum, EnumEntry}

/** An LTI role within an institution. */
sealed abstract class LtiInstitutionRole(val suffix: String) extends EnumEntry with LtiRole:
  override val entryName: String = s"${LtiInstitutionRole.Prefix}$suffix"

object LtiInstitutionRole extends Enum[LtiInstitutionRole]:
  // noinspection TypeAnnotation
  val values = findValues

  final val Prefix = "urn:lti:instrole:ims/lis/"

  case object Student            extends LtiInstitutionRole("Student")
  case object Faculty            extends LtiInstitutionRole("Faculty")
  case object Member             extends LtiInstitutionRole("Member")
  case object Learner            extends LtiInstitutionRole("Learner")
  case object Instructor         extends LtiInstitutionRole("Instructor")
  case object Mentor             extends LtiInstitutionRole("Mentor")
  case object Staff              extends LtiInstitutionRole("Staff")
  case object Alumni             extends LtiInstitutionRole("Alumni")
  case object ProspectiveStudent extends LtiInstitutionRole("ProspectiveStudent")
  case object Guest              extends LtiInstitutionRole("Guest")
  case object Other              extends LtiInstitutionRole("Other")
  case object Administrator      extends LtiInstitutionRole("Administrator")
  case object Observer           extends LtiInstitutionRole("Observer")
  case object None               extends LtiInstitutionRole("None")
end LtiInstitutionRole

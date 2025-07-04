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

/** An LTI role within a system. */
sealed abstract class LtiSystemRole(val suffix: String) extends EnumEntry with LtiRole:
  override val entryName: String = s"${LtiSystemRole.Prefix}$suffix"

object LtiSystemRole extends Enum[LtiSystemRole]:
  // noinspection TypeAnnotation
  val values = findValues

  final val Prefix = "urn:lti:sysrole:ims/lis/"

  case object SysAdmin      extends LtiSystemRole("SysAdmin")
  case object SysSupport    extends LtiSystemRole("SysSupport")
  case object Creator       extends LtiSystemRole("Creator")
  case object AccountAdmin  extends LtiSystemRole("AccountAdmin")
  case object User          extends LtiSystemRole("User")
  case object Administrator extends LtiSystemRole("Administrator")
  case object None          extends LtiSystemRole("None")
end LtiSystemRole

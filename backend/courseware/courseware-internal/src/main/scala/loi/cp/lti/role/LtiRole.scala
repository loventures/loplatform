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

/** An LTI role. */
trait LtiRole

object LtiRole:
  lazy val values: Seq[LtiRole] = LtiSystemRole.values ++ LtiInstitutionRole.values ++ LtiContextRole.values

  def withNameOption(name: String): Option[LtiRole] =
    LtiLoRole.withNameInsensitiveOption(name) orElse LtiContextRole.withNameUnprefixableOption(
      name
    ) orElse LtiSystemRole
      .withNameOption(name) orElse LtiInstitutionRole.withNameOption(name)

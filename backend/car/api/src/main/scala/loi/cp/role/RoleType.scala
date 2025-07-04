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

package loi.cp.role

import com.learningobjects.cpxp.service.relationship.RoleFacade
import com.learningobjects.cpxp.util.FormattingUtils

/** A DTO for [[RoleComponent]] s.
  */
final case class RoleType(id: Long, name: String, roleId: String)

object RoleType:
  def apply(roleFacade: RoleFacade): RoleType =
    Option(roleFacade)
      .map(role =>
        RoleType(
          id = role.getId,
          name = FormattingUtils.contentName(role),
          roleId = role.getRoleId,
        )
      )
      .getOrElse(RoleType(-1.toLong, "guest", "guest"))

  def apply(role: RoleComponent): RoleType =
    RoleType(
      id = role.getId,
      name = role.getName,
      roleId = role.getRoleId,
    )

  implicit val roleTypeOrder: Ordering[RoleType] = Ordering.by(_.name)
end RoleType

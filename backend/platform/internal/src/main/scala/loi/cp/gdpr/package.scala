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

package loi.cp

import loi.cp.admin.right.HostingAdminRight
import loi.cp.right.RightBinding

package object gdpr:
  private[gdpr] type Email  = String
  private[gdpr] type Emails = List[Email]

  @RightBinding(
    name = "right.gdpr.all.name",
    description = "right.gdpr.all.description"
  )
  abstract class AllGdprRight extends HostingAdminRight

  @RightBinding(
    name = "right.gdpr.deidentify.name",
    description = "right.gdpr.deidentify.description"
  )
  abstract class DeidentifyGdprRight extends AllGdprRight

  @RightBinding(
    name = "right.gdpr.test.name",
    description = "right.gdpr.test.description"
  )
  abstract class TestGdprRight extends AllGdprRight
end gdpr

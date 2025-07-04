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

package loi.cp.user

import loi.cp.Widen

/** Any sort of failure when creating a user.
  */
abstract class UserCreationFailure extends Widen[UserCreationFailure]:
  def message: String

case object EmptyGivenName extends UserCreationFailure:
  val message = "Given name must be non-empty."

case object EmptyFamilyName extends UserCreationFailure:
  val message = "Family name must be non-empty."

case class NonUniqueExternalId(externalId: String) extends UserCreationFailure:
  def message: String = s"A user with external id $externalId already exists in the domain."

case class NonUniqueUserName(userName: String) extends UserCreationFailure:
  def message: String = s"A user with name $userName already exists in the domain."

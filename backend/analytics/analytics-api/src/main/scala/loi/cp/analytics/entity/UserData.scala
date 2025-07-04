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

package loi.cp.analytics.entity

case class UserData(
  id: Long,
  externalId: Option[String],
  email: Option[String],
  userName: Option[String],
  givenName: Option[String],
  familyName: Option[String],
  fullName: Option[String],
  subtenantId: Option[String],
  subtenantName: Option[String],
  integration: Option[String],
  uniqueId: Option[String]
)

object UserData:
  extension (self: UserData)
    def eie: ExternallyIdentifiableEntity = ExternallyIdentifiableEntity(self.id, self.externalId)

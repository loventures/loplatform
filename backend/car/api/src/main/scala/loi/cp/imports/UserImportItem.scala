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

package loi.cp.imports

import com.learningobjects.cpxp.scala.json.*

final case class UserImportItem(
  externalId: OptionalField[String] = Absent(),
  userName: String = "",
  givenName: String = "",
  middleName: OptionalField[String] = Absent(),
  familyName: String = "",
  emailAddress: OptionalField[String] = Absent(),
  password: OptionalField[String] = Absent(),
  disabled: Boolean = false,
  integration: Option[IntegrationImportItem] = None,
  role: OptionalField[String] = Absent(),
  sendPasswordReset: OptionalField[Boolean] = Absent(),
  subtenant: OptionalField[String] = Absent(),
) extends ImportItem(UserImportItem.Type)

object UserImportItem:
  final val Type = "User"

  import argonaut.*

  implicit val codec: CodecJson[UserImportItem] =
    CodecJson.derived(using
      E = EncodeJson.derive[UserImportItem],
      D = DecodeJson.derive[UserImportItem]
    )
end UserImportItem

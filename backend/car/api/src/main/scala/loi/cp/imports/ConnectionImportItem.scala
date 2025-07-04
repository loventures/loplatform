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

import com.learningobjects.cpxp.scala.json.{Absent, OptionalField}

/** Import item for social network connection.
  */
final case class ConnectionImportItem(
  // .......... from ..........

  // Identify user by userName
  fromUserName: OptionalField[String] = Absent(),

  // Identify user by externalId
  fromExternalId: OptionalField[String] = Absent(),

  // Identify user by connector
  fromIntegration: Option[IntegrationImportItem] = None,

  // .......... network ..........
  networkId: Option[String] = None,

  // .......... to ..........

  // Identify user by userName
  toUserName: OptionalField[String] = Absent(),

  // Identify user by externalId
  toExternalId: OptionalField[String] = Absent(),

  // Identify user by connector
  toIntegration: Option[IntegrationImportItem] = None,
) extends ImportItem(ConnectionImportItem.Type)

object ConnectionImportItem:
  final val Type = "Connect"

  import argonaut.*

  implicit val codec: CodecJson[ConnectionImportItem] =
    CodecJson.derived(using
      E = EncodeJson.derive[ConnectionImportItem],
      D = DecodeJson.derive[ConnectionImportItem]
    )
end ConnectionImportItem

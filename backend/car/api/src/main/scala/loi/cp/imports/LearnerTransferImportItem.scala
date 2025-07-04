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

case class LearnerTransferImportItem(
  userName: OptionalField[String] = Absent(),
  userExternalId: OptionalField[String] = Absent(),
  userIntegration: Option[IntegrationImportItem] = None,
  sourceCourseId: OptionalField[String] = Absent(),
  sourceCourseExternalId: OptionalField[String] = Absent(),
  sourceCourseIntegration: Option[IntegrationImportItem] = None,
  destinationCourseId: OptionalField[String] = Absent(),
  destinationCourseExternalId: OptionalField[String] = Absent(),
  destinationCourseIntegration: Option[IntegrationImportItem] = None,
) extends ImportItem(LearnerTransferImportItem.Type)

object LearnerTransferImportItem:
  final val Type = "LearnerTransfer"

  import argonaut.*

  implicit val codec: CodecJson[LearnerTransferImportItem] =
    CodecJson.derived(using
      E = EncodeJson.derive[LearnerTransferImportItem],
      D = DecodeJson.derive[LearnerTransferImportItem],
    )
end LearnerTransferImportItem

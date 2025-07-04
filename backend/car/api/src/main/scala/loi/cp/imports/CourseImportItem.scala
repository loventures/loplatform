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

import java.time.Instant

/** Represents an importable Course Section.
  */
final case class CourseImportItem(
  externalId: OptionalField[String] = Absent(),
  courseId: String = "",
  name: String = "",
  offeringId: OptionalField[String] = Absent(),
  termUniqueId: OptionalField[String] = Absent(),
  startDate: OptionalField[Instant] = Absent(),
  endDate: OptionalField[Instant] = Absent(),
  disabled: Option[Boolean] = None,
  integration: Option[IntegrationImportItem] = None,
  subtenant: OptionalField[String] = Absent(),
) extends ImportItem(CourseImportItem.Type)

object CourseImportItem:
  final val Type = "Course"

  import argonaut.*

  implicit val codec: CodecJson[CourseImportItem] =
    CodecJson.derived(using
      E = EncodeJson.derive[CourseImportItem],
      D = DecodeJson.derive[CourseImportItem],
    )
end CourseImportItem

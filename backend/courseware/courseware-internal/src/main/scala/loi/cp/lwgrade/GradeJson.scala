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

package loi.cp.lwgrade
import argonaut.{Argonaut, CodecJson}
import loi.cp.reference.EdgePath
import scaloi.json.ArgoExtras

final case class GradeJson(grades: Map[EdgePath, Grade])

object GradeJson:

  implicit val gradeJsonCodec: CodecJson[GradeJson] =
    Argonaut.casecodec1(GradeJson.apply, ArgoExtras.unapply1)("grades")

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

import argonaut.*
import Argonaut.*
import loi.cp.reference.EdgePath
import loi.cp.lwgrade.Grade.gradeCodec
import scaloi.json.ArgoExtras

final case class GradeChange(edgePath: EdgePath, oldGrade: Option[Grade], newGrade: Option[Grade])
object GradeChange:
  implicit val gradeChangeCodec: CodecJson[GradeChange] =
    CodecJson.casecodec3(GradeChange.apply, ArgoExtras.unapply)("edgePath", "oldGrade", "newGrade")

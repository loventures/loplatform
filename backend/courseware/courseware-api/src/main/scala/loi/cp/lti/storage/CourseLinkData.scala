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

package loi.cp.lti.storage

import argonaut.*
import loi.cp.reference.EdgePath
import scaloi.json.ArgoExtras

/** Configuration for local delivery of a grade from an courseLink.1.
  *
  * @param section
  *   the id of the section to which to report grades.
  * @param edgePath
  *   the edgePath of the gradebook entry.
  * @param user
  *   for preview sections, the id of the origin preview student
  */
case class CourseLinkData(
  section: Long,
  edgePath: EdgePath,
  user: Option[Long],
)

object CourseLinkData:

  implicit val localCodec: CodecJson[CourseLinkData] =
    CodecJson.casecodec3(CourseLinkData.apply, ArgoExtras.unapply)("section", "edgePath", "user")

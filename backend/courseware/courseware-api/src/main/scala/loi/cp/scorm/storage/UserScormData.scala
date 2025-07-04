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

package loi.cp.scorm.storage

import argonaut.Argonaut.*
import argonaut.*
import loi.cp.reference.EdgePath
import loi.cp.storage.CourseStoreable
import scaloi.json.ArgoExtras

//this was going to be a lot more, but then it wasn't
case class UserScormData(apiJsonData: Map[EdgePath, String], sharedJsonData: String):
  def setPathData(edgePath: EdgePath, apiData: String, sharedData: String): UserScormData =
    this.copy(apiJsonData = apiJsonData ++ Map(edgePath -> apiData), sharedJsonData = sharedData)

object UserScormData:
  def empty: UserScormData = UserScormData(Map.empty, "{}")

  implicit val scormDataCodec: CodecJson[UserScormData] =
    CodecJson.casecodec2(UserScormData.apply, ArgoExtras.unapply)("apiData", "sharedData")

  implicit val storageable: CourseStoreable[UserScormData] =
    CourseStoreable("scormData")(empty)

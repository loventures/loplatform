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

package loi.cp.scorm

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserDTO
import loi.asset.html.model.Scorm
import loi.authoring.asset.Asset
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.reference.EdgePath

@Service
trait ScormActivityService:

  /** A JSON object to be provided to the frontend to provided for the window.API object that SCORM uses. Largely, this
    * is just storing and retrieving a JSON entity exactly as it was, but crucially this provides a few useful data
    * elements as well (user's name, credit status, etc)
    *
    * In the case that these data are already stored, this function will overwrite those with the current value as it
    * known to the backend. That is, if some ill-behaving scorm were to have set the user's name to something else, it
    * will get overwritten by this function. We do not read this data in any other way, so this is considered the source
    * of truth.
    *
    * @param contextId
    *   - the course that this SCORM content is for
    * @param user
    *   - the student using this SCORM content
    * @param edgePath
    *   - the specific SCORM content that this data relates to, allowing multiple storages per course
    * @param scorm
    *   - the SCORM asset for this edgepath, providing useful data from the imsmanifest.xml
    * @return
    *   a JSON object of SCORM data to be served to the frontend
    */
  def buildApiJson(context: LightweightCourse, user: UserDTO, edgePath: EdgePath, scorm: Asset[Scorm]): JsonNode

  /** Similar to the above, this is specifically for custom shared data types specified via 'adlcp:data > adlcp:map'
    * fields. This is not well documented on the SCORM website, but it was evident from many of the demo SCORM files as
    * well as support from scorm.com.
    *
    * Unlike the above, this is shared/universal to the course. Example files demonstrated this as a continual storage
    * for student notes, though one could imagine other uses. Because of the prevalence in the SCORM examples, I decided
    * to include it, although it is a significant additional complexity. Also because of its weirdness, it is hard to
    * "fake" include, since data is written using "adl.data.n.store" instead of just "cmi.the.data".
    *
    * That said, while <adlcp:map targetID="com.scorm.example.notes" readSharedData="true" writeSharedData="true"/> is
    * supposed to produce something like { "adl.data.0.id": "com.scorm.example.notes", "adl.data.0.store": "most useful
    * notes" } for the frontend, on the backend we will just store { "com.scorm.example.notes": "most useful notes" }
    * because it is much easier to process, in particular the part where we constrain this shared storage to only the
    * types used by this SCORM asset. That means that "com.scorm.example.notes" and "com.scorm.example.analytics" could
    * both be stored in this object, but this SCORM asset will only served "com.scorm.example.notes" if that is the only
    * one described in its imsmanifest.xml file
    *
    * @param contextId
    *   - the course that this SCORM content is for
    * @param user
    *   - the student using this SCORM content
    * @param scorm
    *   - the SCORM asset, describing which custom data is needed per the imsmanifest.xml
    * @return
    *   a JSON object of shared SCORM data to be served to the frontend
    */
  def buildCustomSharedJson(context: LightweightCourse, user: UserDTO, scorm: Asset[Scorm]): JsonNode
end ScormActivityService

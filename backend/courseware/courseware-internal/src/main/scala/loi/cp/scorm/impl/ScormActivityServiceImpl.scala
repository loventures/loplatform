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

package loi.cp.scorm.impl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.user.UserDTO
import loi.asset.html.model.Scorm
import loi.authoring.asset.Asset
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.reference.EdgePath
import loi.cp.scorm.ScormActivityService
import loi.cp.scorm.storage.UserScormData
import loi.cp.storage.CourseStorageService
import org.apache.commons.lang3.StringUtils

import java.util.Date
import scala.jdk.CollectionConverters.*

/** Implemented using the knowledges from reading
  * https://scorm.com/scorm-explained/technical-scorm/content-packaging/manifest-structure/
  * https://scorm.com/scorm-explained/technical-scorm/run-time/run-time-reference/ and testing out these examples
  * https://scorm.com/scorm-explained/technical-scorm/golf-examples/ side by side on our player and cloud.scorm.com
  */
@Service
class ScormActivityServiceImpl(
  domain: => DomainDTO,
  time: => Date,
  user: => UserDTO,
)(implicit
  courseStorageService: CourseStorageService
) extends ScormActivityService:

  private val mapper = JacksonUtils.getMapper

  override def buildApiJson(
    context: LightweightCourse,
    user: UserDTO,
    edgePath: EdgePath,
    scorm: Asset[Scorm]
  ): JsonNode =
    val userScormData = courseStorageService.get[UserScormData](context, user)
    val apiData       = userScormData.apiJsonData.getOrElse(edgePath, "{}")
    val apiJson       = mapper.reader.readTree(apiData).asInstanceOf[ObjectNode] // I know this to be an object

    // a series of things that just say "we support this". because we accept most strings, we support most things
    loadSupportedFeatures(apiJson)

    // load some important things about the user
    val userId   = StringUtils.defaultIfEmpty(user.emailAddress, user.id.toString)
    val userName = user.givenName + " " + user.familyName
    apiJson.put("cmi.core.student_id", userId)
    apiJson.put("cmi.learner_id", userId)
    apiJson.put("cmi.core.student_name", userName)
    apiJson.put("cmi.learner_name", userName)

    // I really don't think this much matters, every file just returned 1.0 when I tested using scorm.com
    apiJson.put("cmi._version", "1.0")

    // scorm describes this one as "not yet well defined" and "should be used with caution", I have omitted it
    apiJson.put("cmi.launch_data", "") // but seriously who would need to store a constant in the imsmanifest.xml file

    // of the three modes "normal", "browse", and "review", we would only consider the first. I also doubt its use
    // TODO: maybe, we could do mode "browse" for instructors, but again, I doubt its use
    apiJson.put("cmi.core.lesson_mode", "normal")
    apiJson.put("cmi.mode", "normal")

    val credit = if scorm.data.isForCredit then "credit" else "no-credit"
    apiJson.put("cmi.core.credit", credit)
    apiJson.put("cmi.credit", credit)

    // put in all the objectives parsed from the imsmanifest.xml file. each scorm <item> can have multiple objectives,
    // and they sometimes overlap. they all go into one big list.
    scorm.data.objectiveIds.zipWithIndex foreach { case (objectiveId, i) =>
      apiJson.put(s"cmi.objectives.$i.id", objectiveId)
    }

    /** TODO: conceive of how to implement this. I was unable to discern, the best documentation came from this github
      * project https://github.com/gabrieldoty/simplify-scorm/blob/master/src/scormAPI2004.compliance.md
      *
      * However, scorm.com doesn't seem to implement it, and it's complicated because it changed over time. It is also
      * woefully unclear, because it looks to be a universal value (as opposed to cmi.objective.n.id), and yet every
      * objective can have their own threshold set. With sparse documentation on this, or evidence of use, I give ""
      */
    apiJson.put("cmi.completion_threshold", "")

    /** This is also fairly unclear how to implement, and docs are not encouraging. It is described in sequencing here
      * https://scorm.com/scorm-explained/technical-scorm/content-packaging/sequencing-definition-structure/
      *
      * I have not tested whether scorm.com implements this (it would require me to add things not already in the demo
      * scorms). It is also bound to individual activity items, despite, again, being treated as a universal value. The
      * following statement is lifted from the sequencing docs and convinces me to leave it undefined
      *
      * attemptAbsoluteDurationLimit [0:1]Bound to Limit Condition Attempt Absolute Duration Limit (Note, duration-based
      * sequencing is currently not widely supported in SCORM 2004).
      */
    apiJson.put("cmi.max_time_allowed", "")
    apiJson.put("cmi.time_limit_action", "") // sibling to max_time_allowed, describes what it does on time expiration

    // TODO: Could actually do this, but the data format is bothersome P1Y2M3DT4H5M4.56S instead of ms for some reason
    // also I think this feature is mostly for our own use, and we do not really need it
    apiJson.put("cmi.total_time", "")

    /** cmi.exit can be "suspend", "logout", "time-out", or "". Per the spec, the value "" seems to confer the most
      * meaning, so I assume cmi.entry = resume if it is not ""
      *
      * -the spec- cmi.exit (cmi.core.exit) – This value indicates how the learner is exiting the SCO. Setting cmi.exit
      * to “suspend” will ensure that the current attempt is preserved and the run-time data is not reset the next time
      * the SCO is launched. Setting cmi.exit to “” will indicate that the LMS should begin a new attempt with a new set
      * of run-time data on the next launch of the SCO.
      */
    if apiJson.has("cmi.exit") && apiJson.get("cmi.exit").asText() != "" then
      apiJson.put("cmi.core.entry", "resume")
      apiJson.put("cmi.entry", "resume")
    else
      apiJson.put("cmi.core.entry", "ab-initio")
      apiJson.put("cmi.entry", "ab-initio")

    apiJson
  end buildApiJson

  def loadSupportedFeatures(apiJson: ObjectNode) =
    // TODO: in core, we do not support 'total_time' or 'lesson_time', because it is terrible, but not terribly useful
    apiJson.put(
      "cmi.core._children",
      "student_id,student_name,lesson_location,credit,lesson_status,entry,score,lesson_mode,exit"
    )

    apiJson.put("cmi.core.score_children", "raw,min,max")
    apiJson.put("cmi.score._children", "scaled,raw,min,max")
    apiJson.put("cmi.objectives._children", "id,score,status,success_status,completion_status,description")

    apiJson.put("cmi.comments_from_learner._children", "comment,location,timestamp")
    // This is very optional, and providing these comments is strictly up to the LMS. I have decided we do not have them
    apiJson.put("cmi.comments_from_lms._children", "") // omitted: "comment,location,timestamp"

    // Unclear if some SCORM systems let students configure this outside, but these are Read-Write and I let them do it
    apiJson.put("cmi.student_preference._children", "audio,language,speed,text")
    apiJson.put("cmi.learner_preference._children", "audio_level,language,delivery_speed,audio_captioning")

    // these are configured properties that we don't yet provide to authors
    // apiJson.put("cmi.student_data._children","mastery_score,max_time_allowed,time_limit_action")

    // while we "support" type, weighting, latency, etc, this just means we record it. we do nothing with it (for now)
    // much of these are to provide data to LMSes who want to do some kind of analysis on their scormses
    apiJson.put(
      "cmi.interactions._children",
      "id,type,objectives,time,type,timestamp,correct_responses,weighting,student_response,learner_response,result,latency,description"
    )
  end loadSupportedFeatures

  override def buildCustomSharedJson(context: LightweightCourse, user: UserDTO, scorm: Asset[Scorm]): JsonNode =
    val sharedData = courseStorageService.get[UserScormData](context, user).sharedJsonData
    val sharedJson = mapper.reader.readTree(sharedData).asInstanceOf[ObjectNode]

    // filter any fields that are not the dataIds for this SCORM activity
    sharedJson.retain(scorm.data.sharedDataIds.asJava)

    // add in any fields that are not already present for this SCORM activity
    scorm.data.sharedDataIds.foreach { dataId =>
      if !sharedJson.hasNonNull(dataId) then sharedJson.put(dataId, "");
    }

    sharedJson
  end buildCustomSharedJson
end ScormActivityServiceImpl

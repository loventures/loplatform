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

package loi.cp.scorm.api

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.{ApiRootComponent, ErrorResponse, Method}
import com.learningobjects.cpxp.component.{BaseComponent, ComponentInstance}
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.de.authorization.{Secured, SecuredAdvice}
import loi.asset.html.model.Scorm
import loi.cp.admin.right.CourseAdminRight
import loi.cp.analytics.entity.Score
import loi.cp.content.{ContentAccessService, CourseWebUtils}
import loi.cp.context.ContextId
import loi.cp.course.right.{InteractCourseRight, TeachCourseRight}
import loi.cp.lwgrade.GradeService
import loi.cp.progress.{LightweightProgressService, ProgressChange}
import loi.cp.reference.EdgePath
import loi.cp.scorm.ScormActivityService
import loi.cp.scorm.storage.UserScormData
import loi.cp.storage.CourseStorageService
import scalaz.\/
import scaloi.syntax.ʈry.*

import java.time.Instant

/** A web controller for interacting with a SCORM Activity asset on the content path
  *
  * This handles grades and data storage/retrieval
  */
@Component
@Controller(root = true, value = "scorm-activity")
class ScormActivityWebController(
  instance: ComponentInstance,
  contentAccessService: ContentAccessService,
  courseWebUtils: CourseWebUtils,
  gradeService: GradeService,
  courseStorageService: CourseStorageService,
  scormActivityService: ScormActivityService,
  lightweightProgressService: LightweightProgressService,
  user: UserDTO
) extends BaseComponent(instance)
    with ApiRootComponent:
  import ScormWebController.*

  @RequestMapping(path = "lwc/{contextId}/scorm/{edgePath}/apidata", method = Method.GET)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight]))
  def getApiData(
    @PathVariable("contextId") @SecuredAdvice contextId: Long,
    @PathVariable("edgePath") @SecuredAdvice edgePath: EdgePath
  ): ErrorResponse \/ ScormDataResponse =
    for (course, _, scorm) <- contentAccessService
                                .useContentT[Scorm](contextId, edgePath, user)
                                .toRightDisjunction(t => ErrorResponse.forbidden(t.getMessage))
    yield
      val apiJson    = scormActivityService.buildApiJson(course, user, edgePath, scorm)
      val sharedJson = scormActivityService.buildCustomSharedJson(course, user, scorm)

      ScormDataResponse(
        scorm.data.contentHeight,
        scorm.data.contentWidth,
        scorm.data.launchNewWindow,
        scorm.data.resourcePath,
        scorm.data.allRefs,
        apiJson,
        sharedJson
      )

  @RequestMapping(path = "lwc/{contextId}/scorm/{edgePath}/apidata", method = Method.POST)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight]))
  def setApiData(
    @PathVariable("contextId") @SecuredAdvice contextId: Long,
    @PathVariable("edgePath") @SecuredAdvice edgePath: EdgePath,
    @RequestBody req: ScormDataSetJsonRequest
  ): ErrorResponse \/ Unit =
    for _ <- contentAccessService
               .useContentT[Scorm](contextId, edgePath, user)
               .toRightDisjunction(t => ErrorResponse.forbidden(t.getMessage))
    yield courseStorageService.modify[UserScormData](ContextId(contextId), user)(data =>
      data.setPathData(edgePath, req.apiData.toString, req.sharedData.toString)
    )

  /** Submits a SCORM activity. SCORM is very simple and insecure and we trust the students.
    *
    * @param contextId
    *   the course containing the content
    * @param edgePath
    *   the path of the content
    * @param request
    *   the id and score of the scorm activity we are submitting
    */
  @RequestMapping(path = "lwc/{contextId}/scorm/{edgePath}/submit", method = Method.POST)
  @Secured(Array(classOf[TeachCourseRight], classOf[CourseAdminRight], classOf[InteractCourseRight]))
  def submit(
    @PathVariable("contextId") @SecuredAdvice contextId: Long,
    @PathVariable("edgePath") @SecuredAdvice edgePath: EdgePath,
    @RequestBody request: ScormSubmitScoreRequest
  ): ErrorResponse \/ Unit =

    for
      section         <- courseWebUtils.loadCourseSection(contextId).leftMap(e => ErrorResponse.notFound(e))
      // the following provides access control checks, although not a useful course section
      (_, content, _) <- contentAccessService
                           .useContentT[Scorm](contextId, edgePath, user)
                           .toRightDisjunction(t => ErrorResponse.forbidden(t.getMessage))
      score            = Score(request.raw, request.max)
      _               <- gradeService
                           .setGradePercent(user, section, content, score.rawScore, Instant.now())
                           .leftMap(colMissing => ErrorResponse.serverError(colMissing.msg))
      _               <- lightweightProgressService
                           .updateProgress(
                             section,
                             user,
                             gradeService.getGradebook(section, user),
                             List(ProgressChange.visited(content.edgePath))
                           )
                           .leftMap(_ => ErrorResponse.serverError("Error updating progress"))
    yield ()
end ScormActivityWebController

object ScormWebController:

  case class ScormSubmitScoreRequest(
    raw: Double,
    min: Double,
    max: Double
  )

  case class ScormDataResponse(
    contentHeight: Option[Long],
    contentWidth: Option[Long],
    launchNewWindow: Boolean,
    resourcePath: String,
    allRefs: Map[String, String],
    apiData: JsonNode,
    sharedData: JsonNode
  )

  case class ScormDataSetJsonRequest(
    apiData: JsonNode,
    sharedData: JsonNode
  )
end ScormWebController

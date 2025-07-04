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

package loi.cp.course
package preview

import com.google.common.annotations.VisibleForTesting
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.group.GroupConstants.GroupType
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.GuidUtil
import com.learningobjects.de.authorization.Secured
import loi.asset.course.model.Course
import loi.authoring.node.AssetNodeService
import loi.authoring.project.{AccessRestriction, ProjectService}
import loi.authoring.security.right.AccessAuthoringAppRight
import loi.authoring.workspace.service.ReadWorkspaceService
import loi.authoring.workspace.{EdgeInfo, ReadWorkspace}
import loi.cp.config.ConfigurationService
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.courseSection.SectionRootApi.SectionDTO
import loi.cp.courseSection.SectionRootApiImpl
import loi.cp.reference.EdgePathValidationService
import org.apache.commons.lang3.StringUtils
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scalaz.{\/, \/-}
import scaloi.syntax.`try`.*
import scaloi.syntax.boolean.*

import java.util.UUID
import scala.util.Try

@Controller(value = "preview-preview", root = true)
@Secured(Array(classOf[AccessAuthoringAppRight]))
trait PreviewWebController extends ApiRootComponent:

  /** `csrf = false` because the frontend needs to submit a POST request via HTML form submission to properly handle a
    * redirect response.
    *
    * @param edgeNames
    *   List of edge names separated by '.'
    * @return
    *   redirect response to content of preview section
    */
  @RequestMapping(path = "lwc/preview", method = Method.POST, csrf = false)
  def previewCourse(
    @QueryParam("branch") branchId: Long,
    @QueryParam("courseName") courseName: UUID,
    @QueryParam("edgeNames") edgeNames: String,
    @QueryParam("role") role: Option[PreviewRole],
  ): ErrorResponse \/ RedirectResponse

  @RequestMapping(path = "lwc/preview/url", method = Method.POST)
  def previewCourseUrl(@RequestBody request: PreviewRequest): ErrorResponse \/ String

  @VisibleForTesting
  @RequestMapping(path = "lwc/preview/{id}", method = Method.GET)
  def getCourse(@PathVariable("id") id: Long): Option[SectionDTO]
end PreviewWebController

object PreviewWebController:

  /** Header containing the preview section PK. */
  final val CoursePkHeader = "X-CoursePK"
  final val UserPkHeader   = "X-UserPK"

  final val EnrolmentDataSource = "Preview"

final case class PreviewRequest(
  branchId: Long,
  courseName: UUID,
  edgeNames: String,
  role: Option[String],
)

@Component
class PreviewWebControllerImpl(val componentInstance: ComponentInstance)(implicit
  assetService: AssetNodeService,
  projectService: ProjectService,
  facadeService: FacadeService,
  user: UserDTO,
  workspaceService: ReadWorkspaceService,
  edgePathValidationService: EdgePathValidationService,
  configurationService: ConfigurationService,
  courseConfigurationService: CourseConfigurationService,
  previewService: PreviewService,
) extends PreviewWebController
    with ComponentImplementation:

  import ErrorResponse.*
  import GuidUtil.*
  import com.learningobjects.cpxp.service.group.GroupConstants.ID_FOLDER_PREVIEW_SECTIONS as PreviewSectionsFolder

  override def previewCourse(
    branchId: Long,
    courseName: UUID,
    edgeNames: String,
    previewRole: Option[PreviewRole],
  ): ErrorResponse \/ RedirectResponse =

    for
      branch        <- projectService.loadBronch(branchId) \/> notFound(s"no branch: $branchId")
      ws             = workspaceService.requireReadWorkspace(branchId, AccessRestriction.none)
      course        <- assetService.loadA[Course](ws).byName(courseName).disjoin(badRequest)
      edgeNamesList <- edgeNames2Uuids(edgeNames)
      urlStr        <- getUrlString(ws, edgeNamesList)
      init           = new CourseComponent.Init(
                         name = "",
                         groupId = guid(),
                         groupType = GroupType.PreviewSection,
                         createdBy = user,
                         source = (course -> branch).right
                       )
      section        =
        previewFolder.findCourseByBranchAndAsset(branch.id, course.info.name.toString) |
          previewFolder.addCourse(LightweightCourse.Identifier, init)
      _             <- section.getDisabled \/>! ErrorResponse.forbidden
    yield

      val newConfigNode = JacksonUtils.getMapper
        .createObjectNode()
        .put("enableContentFeedback", true)
        .put("enableVideoRecording", true)

      courseConfigurationService.setGroupConfig(CoursePreferences, section, Some(newConfigNode))
      // It is necessary that the *real* you be an instructor in the course in order for
      // your student persona to work, because a couple of the course-lw calls forget to
      // send the user id parameter that makes things work.
      previewService.enrolPreviewer(section, PreviewRole.Instructor, user)

      val (midfix, userId) = previewRole match
        case None       =>
          "#/instructor" -> user.id
        case Some(role) =>
          val previewer = previewService.getOrCreatePreviewer(section, role)
          (role == PreviewRole.Learner).fold("learner#/student", "instructor#/instructor") -> previewer.id
      // Include the course PK as a header to let the integration test validate the resulting section
      RedirectResponse
        .temporary(s"${section.getUrl}/$midfix/$urlStr")
        .copy(headers =
          Map(
            PreviewWebController.CoursePkHeader -> section.id.toString,
            PreviewWebController.UserPkHeader   -> userId.toString
          )
        )

  override def previewCourseUrl(request: PreviewRequest): ErrorResponse \/ String =
    previewCourse(
      request.branchId,
      request.courseName,
      request.edgeNames,
      request.role.map(PreviewRole.withName),
    ) map { case RedirectResponse(url, _, _) =>
      url
    }

  override def getCourse(@PathVariable("id") id: Long): Option[SectionDTO] =
    Option(previewFolder.getCourse(id)).map(SectionRootApiImpl.toDto)

  private def previewFolder: CourseFolderFacade =
    PreviewSectionsFolder.facade[CourseFolderFacade]

  /** If no edgeNames, "dashboard" to main course page else "content/{edgePath}" to playable asset
    */
  private def getUrlString(
    ws: ReadWorkspace,
    edgeNamesList: List[UUID]
  ): ErrorResponse \/ String =

    def str2Cntnt(edgeInfos: List[EdgeInfo]): ErrorResponse \/ String =
      edgePathValidationService
        .validate(ws, edgeInfos)
        .bimap(
          err => badRequest(err.list.toList.mkString(",")),
          path => s"content/${path.toString}"
        )

    edgeNamesList.isEmpty either "dashboard" `orElse` str2Cntnt(ws.getEdgeInfos(edgeNamesList).toList)
  end getUrlString

  def edgeNames2Uuids(eNames: String): ErrorResponse \/ List[UUID] =
    if StringUtils.trimToEmpty(eNames).isEmpty then \/-(List.empty)
    else
      Try(
        eNames
          .split('.')
          .map(UUID.fromString)
          .toList
      ).disjoin(_ => badRequest("edgeNames contains an invalid UUID string."))
end PreviewWebControllerImpl

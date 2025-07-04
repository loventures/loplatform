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

package loi.cp.lti

import com.learningobjects.cpxp.WebContext
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.service.component.misc.LtiToolFinder
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.Comparison
import com.learningobjects.cpxp.service.user.UserDTO
import loi.asset.course.model.Course
import loi.asset.lti.Lti
import loi.authoring.asset.Asset
import loi.authoring.web.AuthoringApiWebUtils
import loi.authoring.workspace.AttachedReadWorkspace
import loi.cp.content.ContentAccessService
import loi.cp.ltitool.*
import loi.cp.ltitool.LtiToolLaunchService.{LaunchInfo, LtiContext, OutcomeInfo}
import loi.cp.reference.EdgePath

import scala.util.Success

@Service
class LtiWebUtils(implicit
  contentAccessService: ContentAccessService,
  currentUrlService: CurrentUrlService,
  webUtils: AuthoringApiWebUtils,
  fs: FacadeService,
  wc: => WebContext
):

  def loadLtiTool(lti: Asset[Lti]) = for
    tool  <- getTool(lti.data.lti.toolId)
    config = lti.data.lti.toolConfiguration.applyDefaultLtiConfig(tool.getLtiConfiguration)
  yield (tool, config)

  def loadContentForCourse(contextId: Long, path: EdgePath, user: UserDTO) =
    for (course, _, lti) <- contentAccessService.useContentT[Lti](contextId, path, user)
    yield
      val context = LtiContext.fromCourse(course)
      (context, lti)

  def loadContentForAuthoring(branchId: Long, assetName: String) =
    val ws      = webUtils.workspaceOrThrow404(branchId)
    val lti     = webUtils.nodeOrThrow404Typed[Lti](ws, assetName)
    val context = ltiContextForAuthoring(ws)

    Success((context, lti))

  // Find the Course asset and pair with the branch ID (normally course ID) to make a psuedo-LtiContext
  def ltiContextForAuthoring(ws: AttachedReadWorkspace) =
    val course = webUtils.nodeOrThrow404Typed[Course](ws, ws.homeName.toString)

    LtiContext(ws.bronchId, course.info.name.toString, course.data.title)

  def defaultLaunchInfo() =
    LaunchInfo(
      wc.getMessages.getLocale,
      baseUrl = currentUrlService.getUrl("/")
    )

  def getLaunchInfo(contextId: Long, path: EdgePath) =
    defaultLaunchInfo().copy(returnUrl = Some(currentUrlService.getUrl(s"/api/v2/lwc/$contextId/lti/return/$path")))

  def getOutcomeInfo(tool: LtiToolComponent, config: LtiLaunchConfiguration, sourceDid: Option[String]) =
    OutcomeInfo(
      graded = config.isGraded.getOrElse(false),
      sourceDid = sourceDid
    )

  def getTool(toolId: String): Option[LtiToolComponent] =
    LtiToolFolderFacade.ID
      .facade[LtiToolFolderFacade]
      .queryLtiTools
      .addCondition(LtiToolFinder.DATA_TYPE_LTI_TOOL_ID, Comparison.eq, toolId)
      .getComponent[LtiToolComponent]
end LtiWebUtils

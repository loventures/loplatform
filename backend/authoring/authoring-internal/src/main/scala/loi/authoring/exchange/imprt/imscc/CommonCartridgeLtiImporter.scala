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

package loi.authoring.exchange.imprt.imscc

import java.io.FileInputStream

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.util.{FormattingUtils, TempFileMap}
import com.learningobjects.de.task.TaskReport
import loi.asset.lti.Lti
import loi.authoring.asset.factory.AssetTypeId
import loi.authoring.exchange.imprt.ImporterUtils.guid
import loi.authoring.exchange.imprt.NodeExchangeBuilder
import loi.authoring.exchange.imprt.exception.{
  BlankLtiLaunchUrlException,
  BlankLtiTitleException,
  LtiCreateNotPermittedException
}
import loi.authoring.exchange.model.NodeExchangeData
import loi.authoring.ltitool.LtiToolService
import loi.cp.i18n.AuthoringBundle
import loi.cp.ltitool.*
import loi.cp.right.RightService

import scala.xml.{Node, XML}

@Service
class CommonCartridgeLtiImporter(
  mapper: ObjectMapper,
  ltiToolService: LtiToolService,
  rightService: RightService
):
  def buildLtiActivity(
    resource: Node,
    files: TempFileMap,
    persistNewTools: Boolean,
    taskReport: TaskReport
  ): NodeExchangeData =
    /* Spec allows inline or external XML. */
    val fileHref = (resource \ "file" \ "@href").text.trim
    val ltiXml   =
      (resource \ "cartridge_basiclti_link").headOption.getOrElse(XML.load(new FileInputStream(files.get(fileHref))))
    val title    = (ltiXml \ "title").text.trim
    val url      = (ltiXml \ "launch_url").text.trim
    if title.isEmpty then
      taskReport.addError(AuthoringBundle.message("imscc.import.blankLtiTitle", url))
      throw BlankLtiTitleException(url)
    if url.isEmpty then
      taskReport.addError(AuthoringBundle.message("imscc.import.blankLtiLaunchUrl", title))
      throw BlankLtiLaunchUrlException(title)
    getOrCreateLtiTool(title, url, persistNewTools, taskReport)
  end buildLtiActivity

  private def getOrCreateLtiTool(
    name: String,
    url: String,
    persistNewTools: Boolean,
    taskReport: TaskReport
  ): NodeExchangeData =
    ltiToolService.getLtiTools
      .find(_.getLtiConfiguration.defaultConfiguration.url.getOrElse("") == url)
      .map(tool => buildLtiAsset(name, tool.getToolId))
      .getOrElse(createLtiTool(name, url, persistNewTools, taskReport))

  private def createLtiTool(
    name: String,
    url: String,
    persistNewTools: Boolean,
    taskReport: TaskReport
  ): NodeExchangeData =
    if !rightService.getUserHasRight(classOf[ManageLtiToolsAdminRight]) then
      throw LtiCreateNotPermittedException(name, url)
    if persistNewTools then
      /* Try not to add tools with duplicate names since that is how they are often displayed. */
      val toolName     =
        if ltiToolService.getLtiTools.exists(_.getName == name) then
          s"$name ${FormattingUtils.formatTime(Current.getTime)}"
        else name
      val launchConfig = LtiLaunchConfiguration(url = Some(url))
      val toolConfig   = LtiToolConfiguration(launchConfig, EditableLtiConfiguration.nothingPermitted)
      val tool         = ltiToolService.addLtiTool(toolName, toolConfig)
      taskReport.addWarning(AuthoringBundle.message("imscc.import.ltiToolCreated", toolName, url))
      buildLtiAsset(name, tool.getToolId)
    else
      taskReport.addWarning(AuthoringBundle.message("imscc.import.ltiToolWillBeCreated", name, url))
      buildLtiAsset(name, "")
    end if
  end createLtiTool

  private def buildLtiAsset(name: String, toolId: String): NodeExchangeData =
    val toolConfig = AssetLtiToolConfiguration(
      toolId = toolId,
      name = name,
      toolConfiguration = LtiLaunchConfiguration.empty
    )
    val data       = Lti(
      title = name,
      duration = None,
      lti = toolConfig,
      author = None,
      attribution = None,
      license = None
    )
    NodeExchangeBuilder.builder(guid, AssetTypeId.Lti.entryName, mapper.valueToTree(data)).build()
  end buildLtiAsset
end CommonCartridgeLtiImporter

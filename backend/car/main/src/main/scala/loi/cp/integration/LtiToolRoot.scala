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

package loi.cp.integration

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults, ApiQuerySupport}
import com.learningobjects.cpxp.component.web.{ErrorResponse, NoContentResponse, WebResponse}
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.util.GuidUtil
import loi.cp.ltitool.*
import org.apache.http.HttpStatus

@Component
class LtiToolRoot(
  val componentInstance: ComponentInstance,
  ltiToolService: LtiToolService,
)(implicit cs: ComponentService)
    extends LtiToolRootComponent
    with ComponentImplementation:

  override def addLtiTool(createLtiToolDto: CreateLtiToolDto): LtiToolComponent =
    val ltiToolComponent: LtiToolComponent = ltiToolService.getLtiToolFolder.addLtiTool()

    ltiToolComponent.setToolId(GuidUtil.guid)
    ltiToolComponent.setName(createLtiToolDto.name)
    ltiToolComponent.setDisabled(createLtiToolDto.disabled)
    ltiToolComponent.setLtiConfiguration(createLtiToolDto.ltiConfiguration)
    ltiToolComponent.setCopyBranchSection(createLtiToolDto.copyBranchSection)

    ltiToolComponent
  end addLtiTool

  override def updateLtiTool(id: Long, createLtiToolDto: CreateLtiToolDto): Option[LtiToolComponent] =
    getLtiTool(id) map { ltiToolComponent =>
      ltiTools.invalidate()
      ltiToolComponent.setName(createLtiToolDto.name)
      ltiToolComponent.setDisabled(createLtiToolDto.disabled)
      ltiToolComponent.setLtiConfiguration(createLtiToolDto.ltiConfiguration)
      ltiToolComponent.setCopyBranchSection(createLtiToolDto.copyBranchSection)
      ltiToolComponent
    }

  override def getLtiTools(query: ApiQuery): ApiQueryResults[LtiToolComponent] =
    ApiQuerySupport.query(ltiTools.queryLtiTools, query, classOf[LtiToolComponent])

  override def getLtiTool(id: Long): Option[LtiToolComponent] =
    getLtiTools(ApiQuery.byId(id)).asOption

  override def deleteLtiTool(id: Long): WebResponse =
    getLtiTool(id)
      .fold[WebResponse](
        ErrorResponse(
          statusCode = HttpStatus.SC_NOT_FOUND,
          body = Some(s"lti tool with id: $id not found")
        )
      )(l =>
        l.delete()
        NoContentResponse
      )

  private def ltiTools: LtiToolFolderFacade =
    ltiToolService.getLtiToolFolder(Current.getDomainDTO)
end LtiToolRoot

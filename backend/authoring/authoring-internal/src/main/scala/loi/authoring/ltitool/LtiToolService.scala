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

package loi.authoring.ltitool

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.util.GuidUtil
import loi.cp.ltitool.{LtiToolComponent, LtiToolConfiguration, LtiToolFolderFacade}

import scala.jdk.CollectionConverters.*

@Service
trait LtiToolService:
  def getLtiTools: Seq[LtiToolComponent]
  def addLtiTool(name: String, config: LtiToolConfiguration): LtiToolComponent

@Service
class BaseLtiToolService(implicit
  facadeService: FacadeService,
) extends LtiToolService:

  def addLtiTool(name: String, config: LtiToolConfiguration): LtiToolComponent =
    val ltiTool = getFolder.addLtiTool().component[LtiToolComponent]
    ltiTool.setToolId(GuidUtil.guid)
    ltiTool.setName(name)
    ltiTool.setDisabled(false)
    ltiTool.setLtiConfiguration(config)
    ltiTool

  def getLtiTools: Seq[LtiToolComponent] =
    getFolder.getLtiTools.asScala.toSeq

  private def getFolder: LtiToolFolderFacade =
    LtiToolFolderFacade.ID.facade[LtiToolFolderFacade]
end BaseLtiToolService

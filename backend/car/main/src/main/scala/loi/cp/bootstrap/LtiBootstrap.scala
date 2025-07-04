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

package loi.cp.bootstrap

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import loi.cp.ltitool.*

/** Bootstrap methods for LTI tools
  */
@Component
sealed class LtiBootstrap(val componentInstance: ComponentInstance)(
  domain: DomainDTO,
  ltiToolService: LtiToolService,
) extends ComponentImplementation:

  /** Bootstrap to create a set of LTI tools
    *
    * @param configs
    *   list of LTI tools
    */
  @Bootstrap("core.ltiTool.create")
  def addLtiTool(configs: List[LtiConfig]): Unit =
    configs.foreach(configureLtiTool)

  /** Creates a new LTI tool
    *
    * @param config
    *   the configuration for the new LTI tool
    */
  private def configureLtiTool(config: LtiConfig): Unit =
    val ltiTool = ltiToolService.getLtiToolFolder.addLtiTool().component[LtiTool]
    ltiTool.setToolId(config.toolId)
    ltiTool.setName(config.name)
    ltiTool.setDisabled(false)

    val settings = config.settings.copy(
      /* kinda silly but saves the QAAuto destrap from being JS */
      url = config.settings.url.map(_.replace("<HOSTNAME>", domain.hostName))
    )

    ltiTool.setLtiConfiguration(
      LtiToolConfiguration(
        defaultConfiguration = settings,
        instructorEditable = EditableLtiConfiguration.nothingPermitted
      )
    )
  end configureLtiTool
end LtiBootstrap

case class LtiConfig(
  toolId: String,
  name: String,
  settings: LtiLaunchConfiguration,
)

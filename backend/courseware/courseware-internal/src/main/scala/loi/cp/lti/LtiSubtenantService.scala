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

import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.facade.FacadeService
import jakarta.servlet.http.HttpServletRequest
import loi.cp.integration.BasicLtiSystemComponent
import loi.cp.subtenant.{Subtenant, SubtenantService}
import scalaz.\/
import scalaz.std.option.*
import scalaz.syntax.cobind.*
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.syntax.OptionOps.*
import scaloi.syntax.TryOps.*

/** Support for subtenants in LTI.
  */
@Service
final class LtiSubtenantService(implicit
  facadeService: FacadeService,
  subtenantService: SubtenantService,
  cs: ComponentService
):
  import LtiSubtenantService.*

  /** Identify the subtenant for this launch. This may come from the connector configuration or launch parameters.
    */
  def processSubtenant(implicit
    request: HttpServletRequest,
    system: BasicLtiSystemComponent
  ): LtiError \/ Option[Subtenant] =
    for
      configuredSubtenant <- system.getBasicLtiConfiguration.subtenant.traverse(getSubtenant)
      maybeSubtenant      <- configuredSubtenant
                               .coflatMap(_.right[LtiError]) // Some(subtenant) -> Some(\/-(Some(subtenant)))
                               .getOrElse(ltiParam(CustomSubtenantIdParameter).flatMap(_.traverse(requestedSubtenant)))
    yield maybeSubtenant

  /** Get the configured subtenant. */
  private def getSubtenant(id: Long): LtiError \/ Subtenant =
    id.component_![Subtenant] \/> { e =>
      InternalLtiError("lti_invalid_subtenant", e)
    }

  /** Get the requested subtenant. */
  private def requestedSubtenant(
    subtenantId: String
  )(implicit request: HttpServletRequest, system: BasicLtiSystemComponent): LtiError \/ Subtenant =
    system.getBasicLtiConfiguration.autoCreateSubtenant.isTrue
      .fold(getOrCreateSubtenant(subtenantId), findSubtenant(subtenantId))

  /** Get or create a subtenant. */
  private def getOrCreateSubtenant(
    subtenantId: String
  )(implicit request: HttpServletRequest, system: BasicLtiSystemComponent): LtiError \/ Subtenant =
    for subtenantName <- ltiParam_!(CustomSubtenantNameParameter)
    yield subtenantService.getOrCreateSubtenant(subtenantId, subtenantName)

  /** Find an existing subtenant. */
  private def findSubtenant(
    subtenantId: String
  )(implicit request: HttpServletRequest, system: BasicLtiSystemComponent): LtiError \/ Subtenant =
    subtenantService.findSubtenantByTenantId(subtenantId) \/> GenericLtiError("lti_unknown_subtenant", subtenantId)
end LtiSubtenantService

object LtiSubtenantService:
  private final val CustomSubtenantIdParameter   = "custom_subtenant"
  private final val CustomSubtenantNameParameter = "custom_subtenant_name"

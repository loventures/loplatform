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

package loi.cp.ltitool

import argonaut.CodecJson
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.service.user.UserDTO
import loi.cp.course.lightweight.LightweightCourse
import loi.cp.course.preview.PreviewRole
import loi.cp.reference.EdgePath
import scaloi.json.ArgoExtras

import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Locale
import java.lang as jl
import scala.compat.java8.OptionConverters.*
import scala.util.Try

/** Support for generating outbound LTI launch parameters.
  */
@Service
trait LtiToolLaunchService:
  import LtiToolLaunchService.*

  def getJWKS(): JWKS

  def getToolPublicKey(ltiConfig: LtiLaunchConfiguration, kid: String): Try[RSAPublicKey]

  /** Get the LTI 1.0 launch parameters. */
  def getLaunchParameters(
    context: LtiContext,
    user: UserDTO,
    ltiTool: LtiToolComponent,
    launchInfo: LaunchInfo,
    resourceLink: Option[ResourceLink],
    outcomeInfo: OutcomeInfo,
    ltiConfig: LtiLaunchConfiguration,
    overrideRoles: List[String] = List.empty
  ): Try[LaunchParameters]

  /** Get the LTI 1.3 initiate login parameters */
  def get1p3LoginParameters(
    contextId: Long,
    role: Option[PreviewRole],
    ltiTool: LtiToolComponent,
    launchInfo: LaunchInfo,
    resourceId: String,
    ltiConfig: LtiLaunchConfiguration,
    isDeepLink: Boolean = false,
    messageHint: Option[String] = None
  ): Try[LaunchParameters]

  /** Get the LTI 1.3 launch parameters */
  def get1p3LaunchParameters(
    context: LtiContext,
    user: UserDTO,
    ltiTool: LtiToolComponent,
    launchInfo: LaunchInfo,
    resourceLink: ResourceLink,
    outcomeInfo: OutcomeInfo,
    state: String,
    nonce: String,
    ltiConfig: LtiLaunchConfiguration,
    overrideRoles: List[String] = List.empty
  ): Try[LaunchParameters]

  /** Get the LTI 1.3 launch parameters */
  def get1p3DeepLinkParameters(
    context: LtiContext,
    user: UserDTO,
    ltiTool: LtiToolComponent,
    launchInfo: LaunchInfo,
    state: String,
    nonce: String,
    ltiConfig: LtiLaunchConfiguration,
    overrideRoles: List[String] = List.empty
  ): Try[LaunchParameters]
end LtiToolLaunchService

object LtiToolLaunchService:

  final val LTI_OUTCOMES_URL = "/lti/services/outcomes"

  final val PLATFORM_FAMILY_CODE = "learning_objects"
  final val PLATFORM_VERSION     = "latest"

  type LaunchParameters = List[(String, String)]

  final case class LtiContext(
    id: Long,
    label: String,
    title: String,
    // these four are more specific to *our* LTI context, and useful for passing fewer objects around
    externalId: Option[String] = None,
    branchId: Option[String] = None,
    startDate: Option[Instant] = None,
    endDate: Option[Instant] = None
  )

  object LtiContext:
    def fromCourse(course: LightweightCourse) = LtiContext(
      id = course.getId,
      label = course.getGroupId,
      title = course.getName,
      externalId = course.getExternalId.asScala,
      branchId = course.getBranchId.asScala.map(_.toString),
      startDate = course.getStartDate,
      endDate = course.getEndDate
    )
  end LtiContext

  final case class ResourceLink(identifier: String, title: String)

  object ResourceLink:
    def apply(path: EdgePath, title: String): ResourceLink = ResourceLink(path.toString, title)
    def apply(id: jl.Long, title: String): ResourceLink    = ResourceLink(id.toString, title)

    final val Fake = ResourceLink(0, "Untitled")

  final case class LaunchInfo(locale: Locale, baseUrl: String, returnUrl: Option[String] = None)

  final case class OutcomeInfo(graded: Boolean, sourceDid: Option[String])

  // classes for ferrying JSON Web Key Sets

  case class JWKS(keys: List[JWKSKey])
  case class JWKSKey(kty: String, alg: String, use: String, e: String, n: String, kid: String)

  implicit val jwksKeyCodec: CodecJson[JWKSKey] =
    CodecJson.casecodec6(JWKSKey.apply, ArgoExtras.unapply)("kty", "alg", "use", "e", "n", "kid")
  implicit val jwksCodec: CodecJson[JWKS]       =
    CodecJson.casecodec1(JWKS.apply, ArgoExtras.unapply1)("keys")

  case class LtiToken(token: String)

  final val ALG_FAMILY = "RSA"
  final val ALG        = "RS256"
  final val USE        = "sig"

  def keyId: String = ???

  def platformPublicKey: String = ???

  def platformPrivateKey: String = ???

end LtiToolLaunchService

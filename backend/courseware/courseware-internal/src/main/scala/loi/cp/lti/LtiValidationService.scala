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

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.util.BeanProxy
import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*
import com.learningobjects.cpxp.service.CurrentUrlService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.integration.IntegrationWebService
import com.learningobjects.cpxp.service.replay.{ReplayException, ReplayService}
import com.learningobjects.cpxp.util.HttpUtils
import loi.cp.integration.{BasicLtiConfiguration, BasicLtiSystemComponent}
import loi.cp.lti.spec.{LtiMessageType, LtiVersion}
import loi.net.oauth.*
import loi.net.oauth.server.OAuthServlet
import loi.net.oauth.signature.OAuthSignatureMethod
import scalaz.\/
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.syntax.BooleanOps.*
import scaloi.syntax.TryOps.*

import java.nio.charset.StandardCharsets
import java.util
import java.util.Date
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import jakarta.servlet.http.HttpServletRequest
import scala.util.Try
import scala.util.control.NonFatal

/** Support for validating the integrity of the request.
  */
@Service
class LtiValidationService(implicit
  facadeService: FacadeService,
  integrationWebService: IntegrationWebService,
  replayService: ReplayService,
  serviceMeta: ServiceMeta,
  cs: ComponentService,
  urlService: CurrentUrlService,
):
  import LtiValidationService.*

  /** Validate the request and lookup the associated connector. */
  def validateLtiRequest(implicit request: HttpServletRequest): LtiError \/ BasicLtiSystemComponent =
    for
      _      <- ltiParamT_![LtiVersion](LtiVersionParameter)
      _      <- ltiParamT_![LtiMessageType](LtiMessageTypeParameter)
      system <- validateOAuthRequest(using request)
    yield system

  /** Validate an OAuth request and lookup the associated connector. */
  def validateOAuthRequest(implicit request: HttpServletRequest): LtiError \/ BasicLtiSystemComponent =
    for
      consumerKey <- ltiParam_!(OAuthConsumerKeyParameter)
      system      <- getSystem(consumerKey)
      _           <- validateOAuth(system)
    yield system

  /** Lookup the LTI connector by system id. */
  private def getSystem(client: String): LtiError \/ BasicLtiSystemComponent =
    for
      id     <- Option(integrationWebService.getById(client)) \/> GenericLtiError("lti_unknown_consumer_key", client).widen
      system <- id.component_![BasicLtiSystemComponent] \/>| GenericLtiError("lti_invalid_consumer_key", client).widen
      _      <- system.getDisabled \/>! GenericLtiError("lti_consumer_key_suspended", client).widen
    yield system

  /** Validate the OAuth integrity of the request. */
  private def validateOAuth(
    system: BasicLtiSystemComponent
  )(implicit request: HttpServletRequest): LtiError \/ Unit =
    for
      _         <- validateSignature(request, system)
      timestamp <- ltiParamT_![Long](OAuthTimestampParameter)
      nonce     <- ltiParam_!(OAuthNonceParameter)
      ip        <- HttpUtils.getRemoteAddr(request, serviceMeta).right
      _         <- validateNonce(system, timestamp, nonce, ip)
    yield {}

  /** Validate the LTI signature. */
  private def validateSignature(request: HttpServletRequest, system: BasicLtiSystemComponent): LtiError \/ Unit =
    Try {
      val url          = request.getRequestURL.toString
      val effectiveUrl =
        if request.header(X_SyntheticUrl).isEmpty then url
        else urlService.getUrl(url.replaceFirst(".*//[^/]*", ""))
      new SimpleOAuthValidator().validateMessage(
        OAuthServlet.getMessage(request, effectiveUrl),
        new OAuthAccessor(new OAuthConsumer("about:blank", system.getSystemId, system.getKey, null))
      )
    } \/> {
      case e: OAuthException =>
        logger.warn(e)("LTI validation error")
        GenericLtiError((e.getMessage == "timestamp_refused").fold("lti_clock_skew", "lti_invalid_signature"))
      case e                 =>
        logger.warn(e)("LTI validation error")
        InternalLtiError("lti_validation_error", e)
    }

  /** Check for clock skew or replay of the request nonce. */
  private def validateNonce(
    system: BasicLtiSystemComponent,
    timestamp: Long,
    nonce: String,
    ip: String
  ): LtiError \/ Unit =
    Try {
      replayService.accept(system.getId, new Date(timestamp * 1000L), nonce, ip)
    } \/> {
      case e: ReplayException if e.isClockSkew =>
        logger.warn(e)("Nonce error")
        GenericLtiError("lti_clock_skew")
      case e                                   =>
        logger.warn(e)("Nonce error")
        GenericLtiError("lti_replay_detected")
    }
end LtiValidationService

object LtiValidationService:
  private final val logger = org.log4s.getLogger

  private final val LtiVersionParameter       = "lti_version"
  private final val LtiMessageTypeParameter   = "lti_message_type"
  private final val OAuthConsumerKeyParameter = "oauth_consumer_key"
  private final val OAuthTimestampParameter   = "oauth_timestamp"
  private final val OAuthNonceParameter       = "oauth_nonce"

  /** Header indicating we should recompute the URL. Used by some integration tests that use cross-domain. */
  val X_SyntheticUrl = "X-SyntheticUrl"

  /** This is for the ltiParam* methods which expect there to be a system in scope */
  private implicit final val NoSystem: BasicLtiSystemComponent =
    BeanProxy.proxy[BasicLtiSystemComponent](NotASystem(BasicLtiConfiguration.empty))

  case class NotASystem(basicLtiConfiguration: BasicLtiConfiguration)

  OAuthSignatureMethod.registerMethodClass("HMAC-SHA256", classOf[HMAC_SHA256])
end LtiValidationService

class OAuthHmac(algorithm: String) extends OAuthSignatureMethod:
  import net.oauth.signature.OAuthSignatureMethod.{base64Encode, decodeBase64}

  override protected def getSignature(baseString: String): String = try base64Encode(this.computeSignature(baseString))
  catch case NonFatal(e) => throw new OAuthException(e)

  override protected def isValid(signature: String, baseString: String): Boolean = try
    util.Arrays.equals(computeSignature(baseString), decodeBase64(signature))
  catch case NonFatal(e) => throw new OAuthException(e)

  private def computeSignature(baseString: String): Array[Byte] =
    val keyString = OAuth.percentEncode(this.getConsumerSecret) + '&' + OAuth.percentEncode(this.getTokenSecret)
    val keyBytes  = keyString.getBytes(StandardCharsets.UTF_8)
    val key       = new SecretKeySpec(keyBytes, algorithm)
    val text      = baseString.getBytes(StandardCharsets.UTF_8)

    val mac = Mac.getInstance(algorithm)
    mac.init(key)
    mac.doFinal(text)
end OAuthHmac

class HMAC_SHA256 extends OAuthHmac("HmacSHA256")

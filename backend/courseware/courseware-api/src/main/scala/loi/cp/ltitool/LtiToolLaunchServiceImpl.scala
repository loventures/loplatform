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

import argonaut.Argonaut.*
import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.component.{ComponentEnvironment, ComponentService}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.relationship.RoleFacade
import com.learningobjects.cpxp.service.user.UserDTO
import com.learningobjects.cpxp.util.{FormattingUtils, OAuthUtils}
import de.tomcat.juli.LogMeta
import loi.cp.course.preview.PreviewRole
import loi.cp.domain.DomainSettingsComponent
import loi.cp.lti.LTIConstants.*
import loi.cp.lti.LtiRoles
import org.apache.commons.codec.binary as apache
import pdi.jwt.*
import scalaz.NonEmptyList
import scalaz.std.list.*
import scalaz.std.option.*
import scalaz.std.string.*
import scalaz.syntax.functor.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.syntax.any.*
import scaloi.syntax.option.*

import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.{RSAPublicKeySpec, X509EncodedKeySpec}
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.{Base64, Properties}
import java.lang as jl
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}

@Service
class LtiToolLaunchServiceImpl(
  enrollmentService: EnrollmentWebService,
  domain: => DomainDTO
)(implicit cs: ComponentService, componentEnvironment: ComponentEnvironment)
    extends LtiToolLaunchService:
  import LtiToolLaunchService.*
  import LtiToolLaunchServiceImpl.*

  override def getJWKS(): JWKS =
    val publicKey = getHostPublicKey()

    JWKS(
      List(
        JWKSKey(
          kty = ALG_FAMILY,
          alg = ALG,
          use = USE,
          e = Base64.getUrlEncoder.encodeToString(publicKey.getPublicExponent.toByteArray),
          n = Base64.getUrlEncoder.encodeToString(publicKey.getModulus.toByteArray),
          kid = keyId
        )
      )
    )
  end getJWKS

  private def getHostPublicKey() =
    val kf        = KeyFactory.getInstance(ALG_FAMILY)
    val encodedPb = apache.Base64.decodeBase64(platformPublicKey)
    val keySpecPb = new X509EncodedKeySpec(encodedPb)
    kf.generatePublic(keySpecPb).asInstanceOf[RSAPublicKey]

  override def getToolPublicKey(ltiConfig: LtiLaunchConfiguration, keyId: String): Try[RSAPublicKey] =
    for
      jwksUrl <- ltiConfig.keysetUrl <@~* new IllegalStateException("No JWKS Keyset URL configured on tool")
      jwksStr  = Using.resource(Source.fromURL(jwksUrl, "UTF-8"))(_.mkString(""))
      jwks    <- jwksStr.decodeOption[JWKS] <@~* new IllegalStateException("Unable to parse JWKS")
      key     <- jwks.keys.find(_.kid == keyId) <@~* new IllegalStateException(s"Unable to find key $keyId")
    yield
      val modulus  = new BigInteger(1, Base64.getUrlDecoder.decode(key.n))
      val exponent = new BigInteger(1, Base64.getUrlDecoder.decode(key.e))
      val spec     = new RSAPublicKeySpec(modulus, exponent)
      val kf       = KeyFactory.getInstance(ALG_FAMILY)
      kf.generatePublic(spec).asInstanceOf[RSAPublicKey]

  override def getLaunchParameters(
    context: LtiContext,
    user: UserDTO,
    ltiTool: LtiToolComponent,
    launchInfo: LaunchInfo,
    resourceLink: Option[ResourceLink],
    outcomeInfo: OutcomeInfo,
    ltiConfig: LtiLaunchConfiguration,
    overrideRoles: List[String] = List.empty
  ): Try[LaunchParameters] =
    for
      url    <- ltiConfig.url <@~* new IllegalStateException("No url")
      key    <- ltiConfig.key <@~* new IllegalStateException("No key")
      secret <- ltiConfig.secret <@~* new IllegalStateException("No secret")
    yield
      val params =
        consumerParams :::
          launchParams(launchInfo) :::
          contextParams(context, ltiConfig, ltiTool) :::
          resourceParams(resourceLink) :::
          outcomeParams(outcomeInfo) :::
          userParams(user, ltiConfig) :::
          roleParams(user, context, ltiConfig, overrideRoles) :::
          customParams(ltiConfig, ltiTool)
      OAuthUtils.getOAuthParameters(url, key, secret, toProperties(params))

  override def get1p3LoginParameters(
    contextId: Long,
    role: Option[PreviewRole],
    ltiTool: LtiToolComponent,
    launchInfo: LaunchInfo,
    resourceId: String,
    ltiConfig: LtiLaunchConfiguration,
    isDeepLink: Boolean = false,
    messageHint: Option[String] = None
  ): Try[LaunchParameters] =
    for
      targetLinkUri <- if isDeepLink then ltiConfig.deepLinkUrl <@~* new IllegalStateException("No deep link URL")
                       else ltiConfig.url <@~* new IllegalStateException("No launch URL")
      clientId      <- ltiConfig.clientId <@~* new IllegalStateException("No client ID")
      deploymentId  <- ltiConfig.deploymentId <@~* new IllegalStateException("No deployment ID")
    yield
      val params = List(
        "iss"               -> launchInfo.baseUrl,
        "target_link_uri"   -> targetLinkUri,
        "client_id"         -> clientId,
        "lti_deployment_id" -> deploymentId,
        "login_hint"        -> (contextId :: resourceId ?: role).mkString(","),
        "lti_message_hint"  -> messageHint.orZ,
      )
      LogMeta.let(params.asJson)(logger.info("LTI 1.3 login parameters"))
      params

  override def get1p3LaunchParameters(
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
  ): Try[LaunchParameters] =

    val stuffs = basic1p3Claims(context, user, launchInfo, nonce, ltiConfig, overrideRoles)

    val customParameters = ltiConfig.customParameters ++
      context.branchId.when(ltiTool.isCopyBranchSection.isTrue).strengthL("context_id_history") ++
      context.startDate.map(_.toString.take(10)).strengthL("course_start_date") ++
      context.endDate.map(_.toString.take(10)).strengthL("course_end_date")

    val allStuffs = stuffs ++ Map(
      MESSAGE_TYPE_CLAIM    -> RESOURCE_LINK_MESSAGE_TYPE,
      TARGET_LINK_URI_CLAIM -> ltiConfig.url,
      PRESENTATION_CLAIM    -> Map(
        "locale"          -> launchInfo.locale.toLanguageTag,
        "document_target" -> (if ltiConfig.launchStyle.contains(LtiLaunchStyle.FRAMED) then "iframe" else "window"),
        "return_url"      -> launchInfo.returnUrl.orZ,
      ).filterNot(_._2.isEmpty),
      RESOURCE_LINK_CLAIM   -> Map(
        "id"    -> resourceLink.identifier,
        "title" -> resourceLink.title
      ),
      CUSTOM_CLAIM          -> customParameters,
//      TODO: support Assignment Grading Services + Names Role Service (parts of LTI Advantage) and Caliper Service here
    )

    import com.learningobjects.cpxp.scala.json.JacksonCodecs.universal.*
    LogMeta.let("params" -> allStuffs.asJson)(logger.info("LTI 1.3 launch parameters"))

    val token = createJwt(user, allStuffs, launchInfo.baseUrl.some, ltiConfig.clientId)

    Try(
      List(
        "id_token" -> token,
        "state"    -> state
      )
    )
  end get1p3LaunchParameters

  override def get1p3DeepLinkParameters(
    context: LtiContext,
    user: UserDTO,
    ltiTool: LtiToolComponent,
    launchInfo: LaunchInfo,
    state: String,
    nonce: String,
    ltiConfig: LtiLaunchConfiguration,
    overrideRoles: List[String] = List.empty
  ): Try[LaunchParameters] =

    val stuffs = basic1p3Claims(context, user, launchInfo, nonce, ltiConfig, overrideRoles)

    val allStuffs = stuffs ++ Map(
      MESSAGE_TYPE_CLAIM          -> DEEP_LINK_MESSAGE_TYPE,
      TARGET_LINK_URI_CLAIM       -> ltiConfig.deepLinkUrl,
      PRESENTATION_CLAIM          -> Map(
        "locale" -> launchInfo.locale.toLanguageTag
      ),
      // https://www.imsglobal.org/spec/lti-dl/v2p0 and https://www.imsglobal.org/specs/lticiv1p0/specification (types)
      DEEP_LINKING_SETTINGS_CLAIM -> Map(
        "accept_types"                         -> List("ltiResourceLink"),
        "accept_presentation_document_targets" -> List("frame", "iframe", "window"),
        "accept_copy_advice"                   -> false,
        "accept_multiple"                      -> true,
        "accept_unsigned"                      -> false,
        "auto_create"                          -> true,
        "can_confirm"                          -> false,
        "deep_link_return_url"                 -> launchInfo.returnUrl,
        // these optional parameters would describe the content item returned by the Tool but... why?
//        "title"                -> ltiTool.getName,
//        "text"                 -> ""
      )
    )

    val token = createJwt(user, allStuffs, launchInfo.baseUrl.some, ltiConfig.clientId)

    Try(
      List(
        "id_token" -> token,
        "state"    -> state
      )
    )
  end get1p3DeepLinkParameters

  private def basic1p3Claims(
    context: LtiContext,
    user: UserDTO,
    launchInfo: LaunchInfo,
    nonce: String,
    ltiConfig: LtiLaunchConfiguration,
    overrideRoles: List[String]
  ) =
    val useExternalId = ltiConfig.useExternalId.isTrue

    Map(
      GIVEN_NAME_CLAIM    -> user.givenName,
      FAMILY_NAME_CLAIM   -> user.familyName,
      EMAIL_CLAIM         -> user.emailAddress,
      NAME_CLAIM          -> FormattingUtils.userStr(user.userName, user.givenName, user.middleName, user.familyName),
      ROLES_CLAIM         -> {
        // LTI 1.3 appears to have no equivalent of "transient" role for launch
        if overrideRoles.nonEmpty then overrideRoles
        else
          val globalRoles = imsRoles(domain.id, user.id, LtiRoles.GlobalMappings1p3)
          val localRoles  = imsRoles(context.id, user.id, LtiRoles.ContextMappings1p3)
          globalRoles ++ localRoles
      },
      CONTEXT_CLAIM       -> Map(
        "id"    -> context.externalId.when(useExternalId).getOrElse(context.id.toString),
        "label" -> context.label,
        "title" -> context.title,
        // Optional, but seems we need - types here https://www.imsglobal.org/spec/lti/v1p3/#context-type-vocabulary
        "type"  -> List("http://purl.imsglobal.org/vocab/lis/v2/course#CourseSection")
      ),
      TOOL_PLATFORM_CLAIM -> Map(
        "name"                -> domain.name,
        "description"         -> domain.name,
        "url"                 -> launchInfo.baseUrl,
        "product_family_code" -> PLATFORM_FAMILY_CODE,
        "version"             -> PLATFORM_VERSION,
        "guid"                -> domain.domainId
      ),
      NONCE_CLAIM         -> nonce,
      VERSION_CLAIM       -> "1.3.0",
      LOCALE_CLAIM        -> launchInfo.locale.toLanguageTag,
      DEPLOYMENT_ID_CLAIM -> ltiConfig.deploymentId.orZ,
    )
  end basic1p3Claims

  private def createJwt(
    user: UserDTO,
    claimsMap: Map[String, Object],
    baseUrl: Option[String],
    clientId: Option[String]
  ) =
    val claim = JwtClaim(
      content = JacksonUtils.getMapper.writeValueAsString(
        claimsMap
      ),
      issuer = baseUrl,
      subject = Some(s"${user.id}"),
      audience = clientId.map(Set(_)),
      issuedAt = Some(Instant.now().toEpochMilli / 1000L), // time in seconds
      // standard 5 minute expiration as per the IMS reference implementation
      expiration = Some(Instant.now().plus(5, ChronoUnit.MINUTES).toEpochMilli / 1000L)
    )

    claim.toJson.parse.foreach(json => LogMeta.let("claim" -> json)(logger.info("JWT launch claim")))

    val token = Jwt.encode(
      JwtHeader(
        Some(JwtAlgorithm.RS256),
        Some(JwtHeader.DEFAULT_TYPE),
        None,
        Some(keyId)
      ),
      claim,
      platformPrivateKey,
    )

    logger.info(s"LTI 1.3 Launch JWT: $token")
    token
  end createJwt

  private def toProperties(params: ParamList): Properties =
    new Properties <| { props =>
      params foreach { case (k, v) =>
        if v ne null then props.setProperty(k, v)
      }
    }

  private def consumerParams: ParamList =
    val domainSettings = domain.component[DomainSettingsComponent]
    List(
      TOOL_CONSUMER_INFO_PRODUCT_FAMILY_CODE -> PLATFORM_FAMILY_CODE,
      TOOL_CONSUMER_INFO_VERSION             -> PLATFORM_VERSION,
      TOOL_CONSUMER_INSTANCE_GUID            -> domain.domainId,
      TOOL_CONSUMER_INSTANCE_NAME            -> domain.name
    ) :::
      (TOOL_CONSUMER_INSTANCE_DESCRIPTION -*> domainSettings.getDescription.toList) :::
      (TOOL_CONSUMER_INSTANCE_CONTACT_EMAIL -*> domainSettings.getSupportEmail.toList)
  end consumerParams

  private def launchParams(launchInfo: LaunchInfo): ParamList =
    "launch_presentation_locale" -> launchInfo.locale.toLanguageTag ::
      ("launch_presentation_return_url" -*> launchInfo.returnUrl.toList)

  private def contextParams(
    context: LtiContext,
    ltiConfig: LtiLaunchConfiguration,
    ltiTool: LtiToolComponent
  ): ParamList =
    val useExternalId = ltiConfig.useExternalId.isTrue
    List(
      "context_type" -> "CourseSection",
      "context_id"   -> context.externalId.when(useExternalId).getOrElse(context.id.toString)
    ) :::
      ltiConfig.includeContextTitle.isTrue ?? List(
        "context_label" -> context.label,
        "context_title" -> context.title
      ) :::
      ltiTool.isCopyBranchSection.isTrue ?? context.branchId.toList
        .map(id => "custom_context_id_history" -> id) :::
      context.startDate.toList.map(i => "custom_course_start_date" -> i.toString.take(10)) :::
      context.endDate.toList.map(i => "custom_course_end_date" -> i.toString.take(10))
  end contextParams

  private def resourceParams(resourceLink: Option[ResourceLink]): ParamList =
    resourceLink foldZ { link =>
      List(
        "resource_link_id"    -> link.identifier,
        "resource_link_title" -> link.title
      )
    }

  // it seems weird to gate given/family on the includeUsername toggle...
  private def userParams(user: UserDTO, ltiConfig: LtiLaunchConfiguration): ParamList =
    ltiConfig.includeUsername.isTrue ?? {
      List(
        "user_id"                -> userId(user, ltiConfig),
        "lis_person_sourcedid"   -> user.userName,
        "lis_person_name_given"  -> user.givenName,
        "lis_person_name_family" -> user.familyName,
      )
    } :::
      ltiConfig.includeEmailAddress.isTrue ?? {
        List("lis_person_contact_email_primary" -> user.emailAddress)
      }

  private def userId(user: UserDTO, ltiConfig: LtiLaunchConfiguration): String =
    if ltiConfig.useExternalId.isTrue then user.externalId.getOrElse(user.userName)
    else user.userName

  private def roleParams(
    user: UserDTO,
    context: LtiContext,
    ltiConfig: LtiLaunchConfiguration,
    overrideRoles: List[String]
  ): ParamList =
    ltiConfig.includeRoles.isTrue ?? {
      val roles =
        if overrideRoles.nonEmpty then LtiRoles.Transient :: overrideRoles
        else
          val globalRoles = imsRoles(domain.id, user.id, LtiRoles.GlobalMappings)
          val localRoles  = imsRoles(context.id, user.id, LtiRoles.ContextMappings)
          localRoles ++ globalRoles

      List("roles" -> roles.mkString(","))
    }

  private def imsRoles(context: Long, user: Long, mappings: Map[String, NonEmptyList[String]]): Seq[String] =
    enrollmentService
      .getActiveUserRoleInfo(user, context)
      .getRoles
      .asScala
      .toSeq
      .flatMap(getImsRoles(mappings))

  private def outcomeParams(outcomeInfo: OutcomeInfo): ParamList =
    outcomeInfo.graded ?? {
      List(
        LIS_OUTCOME_SERVICE_URL -> outcomesUrl
      ) :::
        (LIS_RESULT_SOURCEDID -*> outcomeInfo.sourceDid.toList)
    }

  private def outcomesUrl: String =
    s"https://${domain.hostName}$LTI_OUTCOMES_URL" // support odd ports and http?

  private def customParams(ltiConfig: LtiLaunchConfiguration, ltiTool: LtiToolComponent): ParamList =
    ltiConfig.customParameters.toList.flatMap({ case (k, v) =>
      List(
        "custom_" + k                   -> v,
        "custom_" + convertLegacyKey(k) -> v
      )
    })
end LtiToolLaunchServiceImpl

object LtiToolLaunchServiceImpl:
  private final val logger = org.log4s.getLogger

  type ParamList = List[(String, String)]

  /** Nullsafe truth test for nullable [jl.Boolean]. */
  implicit class JBOptionOps(val self: jl.Boolean) extends AnyVal:
    def isTrue: Boolean = Option(self).exists(_.booleanValue)

  private def getImsRoles(roleMappings: Map[String, NonEmptyList[String]])(role: RoleFacade): List[String] =
    roleMappings.get(role.getRoleId).map(_.list.toList).orZero

  /** The LTI 1 specification forces all characters in key names to lower case and maps anything that is not a number or
    * letter to an underscore.
    *
    * In LTI 2 it is best practice to send the parameter under both names, if different.
    */
  private def convertLegacyKey(key: String): String = key.replaceAll("[^a-zA-Z\\d]", "_").toLowerCase
end LtiToolLaunchServiceImpl

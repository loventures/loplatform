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

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.dto.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.scala.cpxp.Item.*
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.folder.FolderConstants
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.query.{Comparison, QueryBuilder, QueryService, Function as QBFunction}
import com.learningobjects.cpxp.service.site.SiteFinder
import com.learningobjects.cpxp.service.user.{RestrictedLearnerFinder, UserConstants}
import com.learningobjects.cpxp.util.NumberUtils
import jakarta.servlet.http.HttpServletRequest
import loi.cp.config.ConfigurationService
import loi.cp.integration.{BasicLtiSystemComponent, IntegrationService}
import loi.cp.network.NetworkService
import loi.cp.site.SiteService
import loi.cp.subtenant.Subtenant
import loi.cp.user.{RestrictedLearner, UserComponent}
import scalaz.\/
import scalaz.std.list.*
import scalaz.std.option.*
import scalaz.std.string.*
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.misc.{Extractor, TimeSource}
import scaloi.syntax.any.*
import scaloi.syntax.boolean.*
import scaloi.syntax.option.*

import java.math.BigInteger
import scala.jdk.CollectionConverters.*

/** Responsible for locating, provisioning and updating LTI launch users.
  */
@Service
final class LtiUserService(implicit
  facadeService: FacadeService,
  integrationService: IntegrationService,
  networkService: NetworkService,
  configurationService: ConfigurationService,
  siteService: SiteService,
  is: ItemService,
  qs: QueryService,
  ontology: Ontology,
  domain: => DomainDTO,
  now: => TimeSource,
):
  import LtiUserService.*
  import jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN as NOPE

  /** Find, create and/or update the user identified in this LTI launch. */
  def processUser(
    subtenantOpt: Option[Subtenant]
  )(implicit request: HttpServletRequest, system: BasicLtiSystemComponent): LtiError \/ UserComponent =
    for
      userId          <- ltiParam_!(UserIdParameter)
      userNameOpt     <- OptionNZ(system.getUsernameParameter).traverse(ltiParam_!)
      givenNameOpt    <- ltiParam(LisPersonNameGivenParameter)
      familyNameOpt   <- ltiParam(LisPersonNameFamilyParameter)
      emailAddressOpt <- ltiParam(LisPersonContactEmailPrimaryParameter)
      site            <- getOrCreateSite
      init            <- userInit(userNameOpt, givenNameOpt, familyNameOpt, emailAddressOpt).right
      user            <- provisionUser(userId, userNameOpt, init)
      _               <- user.getUserState.getDisabled \/>! FriendlyLtiError("lti_user_suspended", NOPE).widen
      _               <- connectUser(user)
      _               <- site.filter(_.restricted).traverse(restrictUser(user, _))
    yield
      if system.getUseExternalIdentifier then user.setExternalId(userId.jome)
      userNameOpt foreach user.setUserName
      givenNameOpt foreach user.setGivenName
      familyNameOpt foreach user.setFamilyName
      emailAddressOpt foreach user.setEmailAddress
      subtenantOpt.map(_.getId) foreach user.setSubtenant
      integrationService.integrate(user, system, userId)
      user

  def getUser(implicit
    request: HttpServletRequest,
    system: BasicLtiSystemComponent
  ): LtiError \/ Option[UserComponent] =
    for
      userId <- ltiParam_!(UserIdParameter)
      user    = findUser(userId)
      _      <- user.traverse(_.getUserState.getDisabled \/>! FriendlyLtiError("lti_user_suspended", NOPE).widen)
    yield user

  /** Construct user initialization data. */
  private def userInit(
    userNameOpt: Option[String],
    givenName: Option[String],
    familyName: Option[String],
    emailAddress: Option[String]
  ): UserComponent.Init =
    new UserComponent.Init <| { init =>
      init.userName = userNameOpt | randomUserName
      init.givenName = givenName.orNull
      init.familyName = familyName.orNull
      init.emailAddress = emailAddress.orNull
    }

  /** Socially connect a user. */
  private def connectUser(
    user: UserComponent
  )(implicit request: HttpServletRequest, system: BasicLtiSystemComponent): LtiError \/ List[Unit] =
    networkIds.traverse(connectUserOnNetwork(user))

  /** Socially connector a user to the specified users on the given network. */
  private def connectUserOnNetwork(
    user: UserComponent
  )(networkId: String)(implicit request: HttpServletRequest, system: BasicLtiSystemComponent): LtiError \/ Unit =
    for
      network <- networkService.getNetwork(networkId) \/> GenericLtiError("lti_unknown_network", networkId).widen
      userStr <- ltiParam(s"$CustomConnectionParameterPrefix$networkId")
      userIds <- userStr.orZero.split(",").filter(_.nonEmpty).right
      users   <- userIds.flatMap(findUser(_).toSeq).filterNot(_.getUserState.getDisabled).right
    yield networkService.setConnections(user, network, users.toSeq)

  /** Get the network ids included in the request. */
  private def networkIds(implicit request: HttpServletRequest): List[String] =
    request.getParameterMap.keySet.asScala.flatMap(ConnectionNetworkExtractor.unapply).toList

  /** Get the user corresponding to this launch. By default this looks up by external or unique id, optionally falling
    * back to matching by username, creating users as necessary.
    */
  private def provisionUser(userId: String, userNameOpt: Option[String], init: UserComponent.Init)(implicit
    system: BasicLtiSystemComponent
  ): LtiError \/ UserComponent =
    userNameOpt
      .when(system.getBasicLtiConfiguration.usernameFallback.isTrue)
      .cata(findUserByUserIdOrUsername(userId, _), None.right[LtiError])
      .map(userOpt => userOpt.getOrElse(getOrCreateUser(userId, init)))

  /** Find an existing user by user id or username, validating users found by username. */
  private def findUserByUserIdOrUsername(userId: String, userName: String)(implicit
    system: BasicLtiSystemComponent
  ): LtiError \/ Option[UserComponent] =
    lockedFindUser(userId)
      .cata(_.some.right, userFolder.findUserByUsername(userName).traverse(validateUserByUsername))

  /** Validate that a user found by username is not already integrated. */
  private def validateUserByUsername(
    user: UserComponent
  )(implicit system: BasicLtiSystemComponent): LtiError \/ UserComponent =
    user.rightUnless(u => u.getExternalId.isPresent || integrationService.isIntegrated(user, system))(
      GenericLtiError("lti_username_conflict", user.getUserName)
    )

  /** Find a user by external id or unique id, doing a locked re-check if not immediately found. */
  private def lockedFindUser(userId: String)(implicit system: BasicLtiSystemComponent): Option[UserComponent] =
    findUser(userId) orElse {
      userFolder.lock(true)
      userFolder.pollute() // inhibit cache
      findUser(userId)
    }

  /** Find a user by external id or unique id. */
  def findUser(userId: String)(implicit system: BasicLtiSystemComponent): Option[UserComponent] =
    system.getUseExternalIdentifier.fold(
      userFolder.findUserByExternalId(userId),
      userFolder.findUserBySubquery(integrationService.queryIntegrated(system, userId))
    )

  /** Get or create a user by external id or unique id. */
  private def getOrCreateUser(userId: String, init: UserComponent.Init)(implicit
    system: BasicLtiSystemComponent
  ): UserComponent =
    system.getUseExternalIdentifier.fold(
      userFolder.getOrCreateUserByExternalId(userId, init),
      userFolder.getOrCreateUserBySubquery(integrationService.queryIntegrated(system, userId), init)
    )

  /** Look up associated site. */
  private def getOrCreateSite(implicit
    request: HttpServletRequest,
    system: BasicLtiSystemComponent
  ): LtiError \/ Option[SiteFinder] =
    for
      siteIdOpt <- ltiParam(CustomSiteId)
      nameOpt   <- ltiParam(CustomSiteName)
    yield siteIdOpt.zip(nameOpt).when(siteRestrictionsEnabled) map { case (siteId, name) =>
      siteService.getOrCreateSite(siteId, name, restricted = false).update(_.name = name).result
    }

  private def siteRestrictionsEnabled = LtiConfiguration.getDomain.siteRestrictions

  /** Add restricted learner record. */
  private def restrictUser(user: UserComponent, site: SiteFinder)(implicit
    request: HttpServletRequest,
    system: BasicLtiSystemComponent
  ): LtiError \/ Unit =
    for email <- Option(user.getEmailAddress) \/> GenericLtiError("lti_no_email_address")
    yield
      if !RestrictedLearner.isRestricted(domain, email) then
        logger.info(s"Restricting learner ${user.getEmailAddress} for site ${site.name} (${site.siteId})")
        // duplicates are fine so we don't lock the domain. we have a lock on the user anyway
        // so it is pretty unlikely.
        domain.addChild[RestrictedLearnerFinder] { f =>
          f.email = email
          f.created = now.date
        }
      ()

  /** The users folder. */
  private def userFolder = UserConstants.ID_FOLDER_USERS.facade[UserParentFacade]
end LtiUserService

object LtiUserService:
  private val logger = org.log4s.getLogger

  private final val UserIdParameter                       = "user_id"
  private final val LisPersonNameGivenParameter           = "lis_person_name_given"
  private final val LisPersonNameFamilyParameter          = "lis_person_name_family"
  private final val LisPersonContactEmailPrimaryParameter = "lis_person_contact_email_primary"
  private final val CustomConnectionParameterPrefix       = "custom_connection_"

  private final val CustomSiteId   = "custom_site_id"
  private final val CustomSiteName = "custom_site_name"

  private final val ConnectionNetworkExtractor = Extractor `dropPrefix` CustomConnectionParameterPrefix

  /** Support for constructing a username. */
  private def randomUserName       = NumberUtils.toBase20Encoding(random)
  private def random               = new BigInteger(1 + Modulus.bitLength(), NumberUtils.getSecureRandom).mod(Modulus)
  private final val UsernameLength = 12
  private final val Modulus        = BigInteger.valueOf(20).pow(UsernameLength)
end LtiUserService

@FacadeItem(FolderConstants.ITEM_TYPE_FOLDER)
private trait UserParentFacade extends Facade:
  @FacadeComponent
  def getOrCreateUserByExternalId(
    @FacadeCondition(value = UserConstants.DATA_TYPE_EXTERNAL_ID, function = QBFunction.LOWER)
    externalId: String,
    init: UserComponent.Init,
  ): UserComponent

  def findUserByExternalId(
    @FacadeCondition(value = UserConstants.DATA_TYPE_EXTERNAL_ID, function = QBFunction.LOWER)
    externalId: String,
  ): Option[UserComponent]

  def findUserByUsername(
    @FacadeCondition(value = UserConstants.DATA_TYPE_USER_NAME, function = QBFunction.LOWER)
    userName: String,
  ): Option[UserComponent]

  def getOrCreateUserBySubquery(
    @FacadeCondition(value = DataTypes.META_DATA_TYPE_ID, comparison = Comparison.in)
    subQuery: QueryBuilder,
    init: UserComponent.Init,
  ): UserComponent

  def findUserBySubquery(
    @FacadeCondition(value = DataTypes.META_DATA_TYPE_ID, comparison = Comparison.in)
    subQuery: QueryBuilder
  ): Option[UserComponent]

  def lock(pessimistic: Boolean): Unit
end UserParentFacade

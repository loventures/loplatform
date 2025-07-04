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

package loi.cp.user

import com.learningobjects.cpxp.component.annotation.Service
import com.learningobjects.cpxp.component.{ComponentSupport, UserException}
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.domain.DomainConstants
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.exception.ValidationException
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.integration.{IntegrationFacade, IntegrationWebService}
import com.learningobjects.cpxp.service.query.{Comparison, Function as QBFunction}
import com.learningobjects.cpxp.service.session.SessionService
import com.learningobjects.cpxp.service.user.{UserConstants, UserFacade, UserType}
import com.learningobjects.cpxp.util.DigestUtils
import loi.cp.password.{ChangePasswordReceipt, UserPasswordService}
import loi.cp.presence.{LogoutEvent, PresenceService}
import loi.cp.right.RightService
import loi.cp.role.RoleService
import loi.cp.user.UserComponent.UniqueId
import org.apache.commons.codec.digest.DigestUtils.md5
import org.apache.commons.lang3.StringUtils

import scala.jdk.CollectionConverters.*

@Service
class UserRootService(implicit
  enrollmentWebService: EnrollmentWebService,
  facadeService: FacadeService,
  integrationWebService: IntegrationWebService,
  presenceService: PresenceService,
  rightService: RightService,
  roleService: RoleService,
  sessionService: SessionService,
  userPasswordService: UserPasswordService
):

  def parent: UserParentFacade =
    UserConstants.ID_FOLDER_USERS.facade[UserParentFacade]

  def findUserUncached(dataType: String, value: String): Option[UserComponent] =
    parent.queryUsers
      .setCacheQuery(false)
      .addCondition(dataType, Comparison.eq, value, QBFunction.LOWER)
      .getComponent[UserComponent]

  def create(user: UserComponent.Init): UserComponent =
    val overlordDomain = DomainConstants.DOMAIN_TYPE_OVERLORD == Current.getDomainDTO.`type`
    val subtenant      = getSubtenant(user.subtenantId)
    val passwordValid  = user.password != null && !user.password.isEmpty

    // Initialize User
    val init = new UserComponent.Init
    init.userName = user.userName
    init.givenName = user.givenName
    init.familyName = user.familyName
    init.emailAddress = user.emailAddress
    init.middleName = user.middleName
    init.externalId = user.externalId.filter(id => !id.isEmpty)
    if overlordDomain then user.userType = UserType.Overlord
    if !user.emailPassword && passwordValid then init.password = user.password

    // Create User
    val gu     = parent.addUser(init)
    val userId = gu.getId

    // Set subtenant, roles, integrations, send reset/welcome email
    gu.setSubtenant(subtenant)
    if overlordDomain && !user.emailPassword && passwordValid then
      setPasswordOverlord(userId, user.userName, user.password)
    setRoles(user.roles, userId)
    Option(user.uniqueIds) foreach updateUniqueIds(gu)
    if user.emailPassword then sendEmail(userId, user.emailAddress, true)
    gu
  end create

  def update(uc: UserComponent, user: UserComponent.Init): UserComponent =
    val overlordDomain = DomainConstants.DOMAIN_TYPE_OVERLORD == Current.getDomainDTO.`type`
    val subtenant      = getSubtenant(user.subtenantId)
    val passwordValid  = user.password != null && !user.password.isEmpty

    uc.setUserName(user.userName)
    uc.setGivenName(StringUtils.defaultIfBlank(user.givenName, null))
    uc.setFamilyName(StringUtils.defaultIfBlank(user.familyName, null))
    uc.setEmailAddress(user.emailAddress)
    uc.setMiddleName(user.middleName)
    uc.setExternalId(user.externalId.filter(id => !id.isEmpty))
    uc.setSubtenant(subtenant)
    if !user.emailPassword && passwordValid then
      uc.setPassword(user.password)
      presenceService.deliverToUsers(LogoutEvent)(uc.id)
      sessionService.invalidateUserSessions(uc.getId)
    if overlordDomain && !user.emailPassword && passwordValid then
      setPasswordOverlord(uc.getId, user.userName, user.password)
    setRoles(user.roles, uc.getId)

    Option(user.uniqueIds) foreach updateUniqueIds(uc)

    // Send Password Reset Email if necessary
    if user.emailPassword then sendEmail(uc.getId, user.emailAddress, false)

    // Ivalidate Cache
    parent.invalidate()
    uc
  end update

  private def updateUniqueIds(user: UserComponent)(uids: Array[UniqueId]): Unit =
    uids foreach { uid =>
      if Option(integrationWebService.findByUniqueId(uid.systemId, uid.uniqueId, UserConstants.ITEM_TYPE_USER))
          .exists(_ != user.getId)
      then throw new ValidationException("uniqueIds", uid.uniqueId, "Duplicate unique")
    }
    val retain = uids.flatMap(uid => Option(uid.integrationId))
    integrationWebService
      .getIntegrationFacades(user.getId)
      .asScala
      .filter(facade => !retain.contains(facade.getId))
      .foreach(_.delete())
    uids foreach { uid =>
      val facade = Option(uid.integrationId).fold(integrationWebService.addIntegration(user.getId)) { id =>
        id.facade[IntegrationFacade]
      }
      facade.setExternalSystem(uid.systemId)
      facade.setUniqueId(uid.uniqueId)
    }
  end updateUniqueIds

  private def sendEmail(userId: Long, emailAddress: String, creatingUser: Boolean): Unit =
    val usr     = ComponentSupport.get(userId, classOf[UserComponent])
    val receipt = userPasswordService.resetPassword(usr, creatingUser, getUrlGenerator)
    receipt.emailError.foreach((error: ChangePasswordReceipt.EmailFailedReason) =>
      def foo(error: ChangePasswordReceipt.EmailFailedReason) =
        if classOf[ChangePasswordReceipt.MessagingError].isAssignableFrom(error.getClass) then
          throw new UserException("Error sending email to: " + emailAddress)
            .initCause(error.asInstanceOf[ChangePasswordReceipt.MessagingError].messagingException)
            .asInstanceOf[UserException]
        else
          throw new UserException(
            "Error sending email to user with id: " + userId + ", because that user doesn't have an email address"
          )

      foo(error)
    )
  end sendEmail

  private def setRoles(userRoles: Array[String], userId: Long): Unit =
    val domainRoles  = roleService.getDomainRoles.asScala.filter(role => rightService.isSuperiorToRole(role))
    val userRolesSet = userRoles.toSet
    val cleanRoles   = domainRoles.filter(role => userRolesSet.contains(role.getId.toString)).map(role => role.getId)
    enrollmentWebService.setEnrollment(Current.getDomain, cleanRoles.asJava, userId)

  private def setPasswordOverlord(userId: Long, userName: String, password: String): Unit =
    val userFacade    = facadeService.getFacade(userId, classOf[UserFacade])
    val realm         = "Overlord"
    val digestAuthA1  = userName + ":" + realm + ":" + password
    val digestAuthHA1 = DigestUtils.toHexString(md5(digestAuthA1))
    userFacade.setRssPassword(digestAuthHA1)

  // if you are a subtenant admin then create users in your tenant
  private def getSubtenant(subtenantId: Long): Long =
    Current.getUserDTO.subtenantId.fold(subtenantId)(_.longValue)

  private def getUrlGenerator: String => String =
    val isOverlord = DomainConstants.DOMAIN_TYPE_OVERLORD == Current.getDomainDTO.`type`
    if isOverlord then "/control/admin#/resetPassword/".concat else "/etc/ResetPassword/".concat
end UserRootService

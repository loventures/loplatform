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

package loi.cp.account

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance, ComponentService}
import com.learningobjects.cpxp.filter.CurrentFilter
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.domain.{DomainDTO, DomainWebService}
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.user.UserConstants
import jakarta.servlet.AsyncContext
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import loi.cp.accesscode.{AccessCodeRootComponent, AccessCodeState}
import loi.cp.config.ConfigurationService
import loi.cp.context.accesscode.EnrollmentRedemptionSuccess
import loi.cp.course.CourseComponent
import loi.cp.password.*
import loi.cp.role.RoleService
import loi.cp.user.{UserComponent, UserParentFacade}
import scalaz.syntax.applicative.*
import scalaz.syntax.either.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scalaz.{ValidationNel, \/}
import scaloi.syntax.AnyOps.*
import scaloi.syntax.BooleanOps.*

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

@Component
class RegisterRedeemAccountWebControllerImpl(val componentInstance: ComponentInstance)(implicit
  accessCodeRoot: AccessCodeRootComponent,
  componentService: ComponentService,
  configurationService: ConfigurationService,
  domain: DomainDTO,
  domainWebService: DomainWebService,
  enrollmentWebService: EnrollmentWebService,
  executionContext: ExecutionContext,
  facadeService: FacadeService,
  passwordRoot: PasswordRootApi,
  roleService: RoleService
) extends RegisterRedeemAccountWebController
    with ComponentImplementation:
  import AccessCodeRootComponent.AccessCodeRedemption
  import AccountLookup.*
  import RegisterRedeemAccountWebControllerImpl.*
  import RegisterRedeemReason.*

  override def registerRedeem(
    user: UserComponent,
    accessCode: String,
    schema: Option[String],
    ac: AsyncContext
  ): Future[RegisterRedeemFailure \/ Long] =
    for register <- runThrottled(registerRedeemImpl(user, accessCode, schema, ac))
    yield register getOrElse RegisterRedeemFailure(Unavailable).left

  private def registerRedeemImpl(
    user: UserComponent,
    accessCode: String,
    schema: Option[String],
    ac: AsyncContext
  ): RegisterRedeemFailure \/ Long =
    for
      _          <- settings.enabled \/> RegisterRedeemFailure(NotEnabled)
      init       <- userInit(user).toDisjunction.leftMap(e => RegisterRedeemFailure(ValidationError, e.list.toList))
      goc        <- userFolder.getOrCreateUserByUsername(user.getUserName, init).right
      _          <- goc.isCreated \/> RegisterRedeemFailure(DuplicateUser)
      errors     <- passwordRoot.getPasswordErrors(user, user.getPassword).right
      _          <- errors.isEmpty \/> RegisterRedeemFailure(InvalidPassword, errors.asScala.toSeq)
      redemption <- validate(accessCode, schema) \/> RegisterRedeemFailure(InvalidAccessCode)
    yield
      val newUser = goc.result
      CurrentFilter.login(
        ac.getRequest.asInstanceOf[HttpServletRequest],
        ac.getResponse.asInstanceOf[HttpServletResponse],
        newUser.toDTO,
        false
      )
      newUser.setPassword(user.getPassword) // set after login to prevent auto-expiry
      accessCodeRoot.redeemAccessCode(redemption) match
        case ers: EnrollmentRedemptionSuccess =>
          // If the registration successfully enrols the user in a course then associate
          // the new user with the resulting course subtenant and domain role.
          val course = ers.getCourse.component[CourseComponent]
          newUser.setSubtenant(course.getSubtenantId)
          CourseToDomainRoleMapping.get(roleService.getRole(ers.getRole).getRoleId) foreach { roleId =>
            val domainRole = roleService.getRoleByRoleId(roleId)
            enrollmentWebService.createEnrollment(domain.id, domainRole.getId, newUser.getId, AccessCodeDataSource)
          }
        case _                                => // nothing to do
      end match
      newUser.getId

  override def selfRegistration: SelfRegistrationStatus =
    SelfRegistrationStatus(settings.enabled)

  private def settings =
    PasswordRootApiImpl.passwordSettings.getDomain.selfRegistration

  private def validate(accessCode: String, schema: Option[String]): Option[AccessCodeRedemption] =
    val redemption = new AccessCodeRedemption(accessCode, schema.orNull, null)
    (accessCodeRoot.validateAccessCode(redemption) == AccessCodeState.Valid) option redemption

  private def userInit(user: UserComponent) =
    ^(
      nonEmpty("userName", user.getUserName),
      nonEmpty("emailAddress", user.getEmailAddress),
    ) { (userName, emailAddress) =>
      new UserComponent.Init <| { init =>
        init.userName = userName
        init.givenName = tidy(user.getGivenName)
        init.middleName = tidy(user.getMiddleName)
        init.familyName = tidy(user.getFamilyName)
        init.emailAddress = emailAddress
      }
    }

  private def userFolder = UserConstants.ID_FOLDER_USERS.facade[UserParentFacade]
end RegisterRedeemAccountWebControllerImpl

object RegisterRedeemAccountWebControllerImpl:

  private def nonEmpty(field: String, value: String): ValidationNel[String, String] =
    tidy(value).nonEmpty either tidy(value) `orInvalidNel` field

  private def tidy(s: String): String = Option(s).cata(_.trim, "")

  private final val CourseToDomainRoleMapping = Map(
    "student"    -> "student",
    "instructor" -> "faculty"
  )

  private final val AccessCodeDataSource = "AccessCode"
end RegisterRedeemAccountWebControllerImpl

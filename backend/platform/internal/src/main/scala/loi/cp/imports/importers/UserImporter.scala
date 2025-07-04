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

package loi.cp.imports
package importers

import java.util.Optional
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.ApiQuery
import com.learningobjects.cpxp.component.{
  ComponentImplementation,
  ComponentInstance,
  ComponentService,
  ComponentSupport
}
import com.learningobjects.cpxp.scala.json.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.enrollment.EnrollmentWebService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.integration.IntegrationWebService
import com.learningobjects.cpxp.service.query.QueryService
import com.learningobjects.cpxp.service.user.{UserDTO, UserState}
import loi.cp.analytics.AnalyticsConstants.EventActionType
import loi.cp.imports.errors.*
import loi.cp.integration.IntegrationComponent.Init
import loi.cp.password.ChangePasswordReceipt.EmailAddressNotFound
import loi.cp.password.UserPasswordService
import loi.cp.role.{RoleComponent, RoleService}
import loi.cp.subtenant.SubtenantService
import loi.cp.user.{UserComponent, UserRootApi}
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.validator.routines.EmailValidator
import scalaz.{-\/, \/, \/-}
import scalaz.syntax.applicative.*
import scalaz.syntax.either.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scalaz.syntax.validation.*
import scaloi.misc.JavaOptionalInstances.*

import scala.jdk.CollectionConverters.*

@Component(name = "$$name=Users")
@ImportBinding(
  value = classOf[UserImportItem],
  label = "loi.cp.imports.UserImportItem.label"
)
class UserImporter(val componentInstance: ComponentInstance)(implicit
  val integrationWebService: IntegrationWebService,
  val queryService: QueryService,
  val fs: FacadeService,
  val roleService: RoleService,
  val enrollmentWebService: EnrollmentWebService,
  val userPasswordService: UserPasswordService,
  val subtenantService: SubtenantService,
  val componentService: ComponentService
) extends ComponentImplementation
    with ValidatingImporter[UserImportItem]
    with DeserializingCsvImporter
    with ImporterWithIntegration
    with ImporterWithSubtenant:
  import Importer.*

  override val log = org.log4s.getLogger

  override def requiredHeaders: Set[String] = Set("userName", "givenName", "familyName")

  override def allHeaders: Set[String] = Set(
    "userName",
    "familyName",
    "givenName",
    "externalId",
    "middleName",
    "role",
    "emailAddress",
    "password",
    "integrationConnectorId",
    "sendPasswordReset",
    "integrationUniqueId",
    "disabled",
    "subtenant",
  )

  override def validateItem(item: UserImportItem): ViolationNel[UserImportItem] =
    (validateNonEmpty(item, "userName", _.userName)
      *> validateNonEmpty(item, "givenName", _.givenName)
      *> validateNonEmpty(item, "familyName", _.familyName)
      *> item.sendPasswordReset.traverse(validateSendPasswordReset(item.emailAddress))
      *> item.subtenant.traverse(validateSubtenant)
      *> item.emailAddress.toOption.cata(validateEmail, ().successNel[Violation])).map(_ => item)

  private def validateEmail(email: String): ViolationNel[Unit] =
    if EmailValidator.getInstance().isValid(email) then ().successNel
    else FieldViolation("email", email, "malformed email").failureNel

  override def deserializeCsvRow(headers: Seq[String], values: Seq[String]): ValidationError \/ UserImportItem =
    ifHeadersMatchValues(headers, values) { columns =>
      val uniqueId     = columns.getOptionalField("externalId")
      val userName     = columns.failIfNone("userName")
      val givenName    = columns.failIfNone("givenName")
      val middleName   = columns.getOptionalField("middleName")
      val familyName   = columns.failIfNone("familyName")
      val emailAddress = columns.getOptionalField("emailAddress")
      val password     = columns.getOptionalField("password")

      val active: ViolationNel[Boolean] = columns
        .failIfNone("disabled")
        .map({ disabled =>
          deserializeJson[Boolean](disabled) {
            FieldViolation("disabled", disabled, "disabled was not formatted appropriately")
          }
        })
        .getOrElse(false.successNel[Violation])

      // either both integration values must be included, or none should be included
      val integrationUniqueId    =
        columns.getOptional("integrationUniqueId").flatten
      val integrationConnectorId =
        columns.getOptional("integrationConnectorId").flatten

      val integration: ViolationNel[Option[IntegrationImportItem]] =
        getIntegrationFromColumns(integrationUniqueId, integrationConnectorId)

      val sendPasswordResetValidation =
        columns
          .getOptionalField("sendPasswordReset")
          .map(BooleanUtils.toBoolean)
          .traverse(validateSendPasswordReset(emailAddress))

      val subtenantValidation = columns.getOptionalField("subtenant").traverse(validateSubtenant)

      val role = columns.getOptionalField("role")

      val validator =
        userName |@| givenName |@| familyName |@| active |@| integration |@| sendPasswordResetValidation |@| subtenantValidation

      val validated = validator((userName, givenName, familyName, active, integration, sendPasswordReset, subtenant) =>
        UserImportItem(
          uniqueId,
          userName,
          givenName,
          middleName,
          familyName,
          emailAddress,
          password,
          active,
          integration,
          role,
          sendPasswordReset,
          subtenant
        )
      )

      validated
        .leftMap(violations => ValidationError(violations))
        .toDisjunction
    }

  private def validateSendPasswordReset(
    emailAddress: OptionalField[String]
  )(sendReset: Boolean): ViolationNel[Boolean] =
    if sendReset then
      emailAddress.toOption
        .toSuccessNel(
          FieldViolation(
            "sendPasswordReset",
            "true",
            "sendPasswordReset was set to true, but user doesn't have an email address"
          ).widen
        )
        .map(_ => sendReset)
    else sendReset.successNel

  override def execute(invoker: UserDTO, validated: Validated[UserImportItem]): PersistError \/ ImportSuccess =
    val user = validated.item
    user.role.map(roleByRoleId) match
      case Present(-\/(error)) => PersistError(error).left
      case Present(\/-(role))  => execute0(invoker, user, Present(role))
      case Null()              => execute0(invoker, user, Null())
      case Absent()            => execute0(invoker, user, Absent())

  private def execute0(
    invoker: UserDTO,
    user: UserImportItem,
    foundRole: OptionalField[RoleComponent]
  ): PersistError \/ ImportSuccess =
    val userInit = userImportItemToInit(user)
    val newUser  = ComponentSupport
      .get(classOf[UserRootApi])
      .getOrCreateUser(userInit)

    val actualUser = newUser.result

    var analyticsActionType = EventActionType.CREATE

    if newUser.isGotten then
      analyticsActionType = EventActionType.UPDATE
      actualUser.setFamilyName(user.familyName)
      actualUser.setGivenName(user.givenName)

      user.externalId.coapplyTo(actualUser.setExternalId)
      user.emailAddress.applyTo(actualUser.setEmailAddress)
      user.password.applyTo(actualUser.setPassword)
      user.middleName.applyTo(actualUser.setMiddleName)

      if user.disabled then actualUser.transition(UserState.Suspended)
      else actualUser.transition(UserState.Active)

      foundRole match
        case Present(r) =>
          enrollmentWebService.setEnrollment(
            Current.getDomain,
            r.getId,
            actualUser.getId,
            null
          )
        case Null()     =>
          enrollmentWebService.removeGroupEnrollmentsFromUser(Current.getDomain, actualUser.getId)
        case Absent()   =>
      end match
    end if

    importSubtenant(user.subtenant)(sOpt => actualUser.setSubtenant(sOpt.orNull))

    // only create an integration if the user has included one
    for
      integration <- user.integration
      system      <- getSystemForConnectorId(integration.connectorId)
    do
      getIntegrationForUserAndSystem(actualUser, system.getId) match
        case Some(oldIntegration) =>
          oldIntegration.setUniqueId(integration.uniqueId)
        case None                 =>
          val init = new Init()
          init.systemId = system.getId
          init.uniqueId = integration.uniqueId
          actualUser.getIntegrationRoot.addIntegration(init)
    end for

    // handle Password resets
    val emailFailure = user.sendPasswordReset match
      case Present(true) =>
        val receipt = userPasswordService.resetPassword(actualUser, newUser.isCreated, "/#/resetPassword/".concat)
        receipt.emailError match
          case Some(EmailAddressNotFound) =>
            PersistError(
              s"User could not receive email to reset password, because they didn't have an email address."
            ).some
          case _                          => None
      case _             => None

    emailFailure.toLeftDisjunction(ImportSuccess(Some(user)))
  end execute0

  /** Finds role by roleId.
    */
  def roleByRoleId(roleId: String): String \/ RoleComponent =
    \/.attempt(roleService.getRoleByRoleId(roleId))(identity).toOption
      .filter(_ != null)
      .fold[String \/ RoleComponent](s"Role with id: ${roleId} doesn't exist".left)(c => c.right)

  def getIntegrationForUserAndSystem(user: UserComponent, systemId: Long) =
    // I'm not sure why this query doesn't work
    // val qb = queryService.queryParent(user.getId)
    // qb.addCondition(IntegrationConstants.DATA_TYPE_EXTERNAL_SYSTEM, Comparison.eq, systemId)
    // qb.getComponents[IntegrationComponent].headOption
    user.getIntegrationRoot
      .getIntegrations(ApiQuery.ALL)
      .asScala
      .find(_.getSystemId == systemId)

  def getStateFromDisabled(disabled: Boolean) =
    disabled match
      case true  => UserState.Active
      case false => UserState.Suspended

  def userImportItemToInit(user: UserImportItem): UserComponent.Init =
    val userInit = new UserComponent.Init()
    userInit.externalId = user.externalId match
      case Present(a) => Optional.of(a)
      case _          => Optional.empty()
    userInit.middleName = user.middleName match
      case Present(a) => a
      case _          => null
    userInit.familyName = user.familyName
    userInit.givenName = user.givenName
    userInit.emailAddress = user.emailAddress match
      case Present(a) => a
      case _          => null
    userInit.password = user.password match
      case Present(a) => a
      case _          => null
    userInit.userName = user.userName
    userInit.roles = user.role.toOption.toArray
    userInit
  end userImportItemToInit
end UserImporter

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

import com.google.common.net.MediaType
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.query.{ApiQuery, ApiQueryResults, ApiQuerySupport, PredicateOperator}
import com.learningobjects.cpxp.component.web.{ErrorResponse, FileResponse, WebRequest, WebResponse}
import com.learningobjects.cpxp.component.{
  ComponentImplementation,
  ComponentInstance,
  ComponentService,
  ComponentSupport
}
import com.learningobjects.cpxp.controller.login.LoginController
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.enrollment.EnrollmentConstants
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.query.*
import com.learningobjects.cpxp.service.session.SessionService
import com.learningobjects.cpxp.service.user.*
import com.learningobjects.cpxp.service.user.UserConstants.*
import com.learningobjects.cpxp.util.FileOps.*
import com.learningobjects.cpxp.util.FormattingUtils
import com.learningobjects.cpxp.web.ExportFile
import kantan.csv.HeaderEncoder
import loi.cp.right.RightService
import loi.cp.role.RoleService
import scalaz.\/
import scalaz.std.list.*
import scalaz.std.option.*
import scalaz.std.string.*
import scalaz.syntax.std.option.*
import scalaz.syntax.traverse.*
import scaloi.GetOrCreate
import scaloi.json.ArgoExtras
import scaloi.syntax.any.*
import scaloi.syntax.boolean.*
import scaloi.syntax.option.*
import scaloi.syntax.zero.*

import java.text.SimpleDateFormat
import java.util.TimeZone
import scala.compat.java8.OptionConverters.*
import scala.jdk.CollectionConverters.*

@Component
class UserRootApiImpl(val componentInstance: ComponentInstance)(implicit
  facadeService: FacadeService,
  rightService: RightService,
  roleService: RoleService,
  domain: DomainDTO,
  user: UserDTO,
  userRootService: UserRootService,
  sessionService: SessionService,
  cs: ComponentService,
  qs: QueryService,
) extends UserRootApi
    with ComponentImplementation:

  import UserRootApiImpl.*

  override def getSelf: Option[UserComponent] =
    Current.isAnonymous.noption(Current.getUser.component[UserComponent])

  override def getUsers(query: ApiQuery): ApiQueryResults[UserComponent] =
    ApiQuerySupport.query(
      userRootService.parent.queryUsers.addCondition(notGdpred),
      sublet(query),
      classOf[UserComponent]
    )

  override def getUser(id: Long): Option[UserComponent] =
    getUsers(ApiQuery.byId(id, classOf[UserComponent])).asOption

  private def sublet(query: ApiQuery): ApiQuery =
    user.subtenantId.fold(query) { id =>
      new ApiQuery.Builder(query).addPrefilter("subtenant_id", PredicateOperator.EQUALS, id.toString).build()
    }

  override def create(user: UserComponent.Init): ErrorResponse \/ UserComponent =
    for
      _ <- userRootService.parent.lock(true) \/> ErrorResponse.serverError
      _ <- userNameAvailable(None, user.userName) \/> duplicateUserName(user)
      _ <- user.externalId.asScala.forall(id => externalIdAvailable(None, id)) \/> duplicateExternalId(user)
    yield userRootService.create(user)

  override def update(id: Long, user: UserComponent.Init): ErrorResponse \/ UserComponent =
    for
      userToUpdate <- getForUpdate(id)
      _            <- userRootService.parent.lock(true) \/> ErrorResponse.serverError
      _            <- userNameAvailable(Some(id), user.userName) \/> duplicateUserName(user)
      _            <- user.externalId.asScala.forall(eId => externalIdAvailable(Some(id), eId)) \/> duplicateExternalId(user)
    yield userRootService.update(userToUpdate, user)

  override def delete(id: Long): ErrorResponse \/ Unit =
    for user <- getForUpdate(id)
    yield user.delete()

  override def deleteBatch(ids: List[Long]): ErrorResponse \/ Unit =
    for users <- ids.traverseU(_.facade_?[UserFacade] \/> ErrorResponse.notFound)
    yield users.foreach(user => user.delete())

  override def transition(id: Long, transition: UserRootApi.Transition): ErrorResponse \/ Unit =
    for user <- getForUpdate(id)
    yield user.transition(transition.state)

  override def transitionBatch(ids: List[Long], transition: UserRootApi.Transition): ErrorResponse \/ Unit =
    for users <- ids.traverse(getForUpdate)
    yield users.foreach(user => user.transition(transition.state))

  override def sudo(id: Long, returnUrl: Option[String]): ErrorResponse \/ Unit =
    for
      _    <- returnUrl.forall(ReturnUrlRe.matches) \/> ErrorResponse.badRequest(Map("returnUrl" -> returnUrl))
      user <- getForUpdate(id)
    yield loginController.logInAs(user.getId, returnUrl.orNull)

  override def logout(id: Long): ErrorResponse \/ Unit =
    for user <- getForUpdate(id)
    yield sessionService.invalidateUserSessions(user.getId)

  override def downloadAdminReport(request: WebRequest): WebResponse =
    val admins =
      userRootService.parent.queryUsers
        .addCondition(BaseCondition.inQuery(DataTypes.META_DATA_TYPE_ID, adminRoleQuery))
        .setOrder(UserConstants.DATA_TYPE_FAMILY_NAME, Direction.ASC)
        .setOrder(UserConstants.DATA_TYPE_GIVEN_NAME, Direction.ASC)
        .getComponents[UserComponent]

    val out = ExportFile.create(s"Administrator Report.csv", MediaType.CSV_UTF_8, request)
    out.file.writeCsvWithBom[AdminRow] { csv =>
      admins foreach { admin =>
        csv.write(AdminRow(admin))
      }
    }
    FileResponse(out.toFileInfo)
  end downloadAdminReport

  private def adminRoleQuery =
    qs.queryAllDomains(EnrollmentConstants.ITEM_TYPE_ENROLLMENT)
      .addCondition(EnrollmentConstants.DATA_TYPE_ENROLLMENT_GROUP, Comparison.eq, domain)
      .addCondition(BaseCondition.inIterable(EnrollmentConstants.DATA_TYPE_ENROLLMENT_ROLE, adminRoles))
      .setProjection(Projection.PARENT_ID)

  private def adminRoles =
    roleService.getDomainRoles.asScala.filter(rightService.isAdminRole).map(_.getId)

  private def getForUpdate(id: Long): ErrorResponse \/ UserComponent =
    for
      user <- getUser(id) \/> ErrorResponse.notFound(s"No such user: $id")
      _    <- rightService.isSuperiorToUser(user.facade[UserFacade]) \/> ErrorResponse.forbidden("Privilege escalation")
    yield user

  override def getDomainRoles: List[UserRootApi.Role] =
    roleService.getDomainRoles.asScala
      .filterNot(roleService.isHostingRole)
      .map(role =>
        UserRootApi.Role(
          id = role.getId,
          name = FormattingUtils.roleStr(role),
          admin = rightService.isAdminRole(role),
          superior = rightService.isSuperiorToRole(role),
        )
      )
      .toList

  // TODO: killme
  override def getOrCreateUser(user: UserComponent.Init): GetOrCreate[UserComponent] =
    userRootService.parent.getOrCreateUserByUsername(user.userName, user)

  private def userNameAvailable(userId: Option[Long], userName: String): Boolean =
    val user = userRootService.findUserUncached(DATA_TYPE_USER_NAME, userName)
    user ∀ { u => userId ∃ { _ == u.getId.longValue } }

  private def externalIdAvailable(userId: Option[Long], externalId: String): Boolean =
    val user = userRootService.findUserUncached(DATA_TYPE_EXTERNAL_ID, externalId)
    user ∀ { u => userId ∃ { _ == u.getId.longValue } }
end UserRootApiImpl

object UserRootApiImpl:
  private def loginController: LoginController = ComponentSupport.newInstance(classOf[LoginController])

  private def duplicateUserName(user: UserComponent.Init): ErrorResponse =
    ErrorResponse.validationError("userName", user.userName)("Duplicate Username.")

  private def duplicateExternalId(user: UserComponent.Init): ErrorResponse =
    ErrorResponse.validationError("externalId", user.externalId)("Duplicate External Id.")

  private final val ReturnUrlRe = "/Administration/((Course|Test)Sections/\\d+/Enrollments|Users)|/Users".r

  private val notGdpred = BaseCondition.getInstance(UserFinder.DATA_TYPE_USER_STATE, Comparison.ne, UserState.Gdpr)
end UserRootApiImpl

private final case class AdminRow(
  name: String,
  username: String,
  email: String,
  role: String,
  created: String,
  lastLogin: Option[String],
  suspended: Boolean,
)

private object AdminRow:
  def apply(user: UserComponent)(implicit fs: FacadeService, domain: DomainDTO): AdminRow =
    val facade = user.facade[UserFacade]
    new AdminRow(
      name = user.getFullName.zNull,
      username = user.getUserName.zNull,
      email = user.getEmailAddress.zNull,
      role = user.getDomainRoleDisplayNames.asScala.sorted.mkString("\n"),
      created = dateFormat.format(facade.getCreateTime),
      lastLogin = facade.getHistory.asScala.mapNonNull(_.getLoginTime).map(dateFormat.format),
      suspended = user.getUserState.getDisabled,
    )
  end apply

  def dateFormat(implicit domain: DomainDTO) =
    new SimpleDateFormat("YYYY-MM-dd HH:mm:ss") <| {
      _.setTimeZone(TimeZone.getTimeZone(domain.timeZone))
    }

  implicit val adminRowHeaderEncoder: HeaderEncoder[AdminRow] = HeaderEncoder.caseEncoder(
    "Name",
    "Username",
    "Email Address",
    "Role",
    "Created",
    "Last login",
    "Suspended",
  )(ArgoExtras.unapply)
end AdminRow

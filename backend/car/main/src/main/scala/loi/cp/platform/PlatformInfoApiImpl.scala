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

package loi.cp.platform

import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.query.ApiQuery
import com.learningobjects.cpxp.component.web.ServletBinding
import com.learningobjects.cpxp.component.{
  ComponentDescriptor,
  ComponentImplementation,
  ComponentInstance,
  ComponentService
}
import com.learningobjects.cpxp.controller.domain.DomainAppearance
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.util.HttpServletRequestOps.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.attachment.ResourceDTO
import com.learningobjects.cpxp.service.domain.DomainDTO
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.subtenant.SubtenantFacade
import com.learningobjects.cpxp.service.user.{UserDTO, UserType}
import com.learningobjects.cpxp.util.SessionUtils
import jakarta.servlet.http.HttpServletRequest
import loi.cp.admin.right.AdminRight
import loi.cp.enrollment.EnrollmentComponent
import loi.cp.right.{RightMatch, RightService}
import loi.cp.session.SessionComponent
import loi.cp.user.UserComponent
import scalaz.std.string.*
import scalaz.syntax.std.boolean.*
import scalaz.syntax.std.option.*
import scaloi.syntax.BooleanOps.*
import scaloi.syntax.OptionOps.*

import java.lang.annotation.Annotation
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

@Component
class PlatformInfoApiImpl(val componentInstance: ComponentInstance)(
  domain: => DomainDTO,
  rightService: RightService,
  serviceMeta: ServiceMeta,
  userDTO: => Option[UserDTO],
  request: => HttpServletRequest,
)(implicit
  fs: FacadeService,
  cs2: ComponentService,
  da: DomainAppearance
) extends PlatformInfoApi
    with ComponentImplementation:
  import PlatformInfoApiImpl.*

  override def getPlatformInfo: PlatformInfo =
    val user = userDTO.map(_.component[UserComponent])
    val sti  = getSubtenant(user)

    PlatformInfo(
      user = user filterNot isAnonymous map userInfo,
      domain = DomainInfo(
        id = domain.id,
        name = sti.map(_.name) | domain.name,
        shortName = sti.nzMap(_.shortName) | domain.shortName,
        hostName = domain.hostName,
        locale = domain.locale,
        timeZone = domain.timeZone,
        favicon = domain.favicon,
        image = domain.image,
        logo = sti.mapNonNull(_.logo) | domain.logo,
        logo2 = domain.logo2,
        css = domain.css,
        da.getStyleConfigurations.asScala.toMap,
      ),
      session = userDTO.exists(_.userType != UserType.Anonymous) option component[SessionComponent],
      loggedOut = request.cookie(SessionUtils.COOKIE_NAME_INFO).contains(SessionUtils.COOKIE_INFO_LOGGED_OUT),
      // enrollments = user.map(enrollments).orZero,
      adminLink = getAdminLink,
      authoringLink = getAuthoringLink,
      isProduction = serviceMeta.isProduction,
      isProdLike = serviceMeta.isProdLike,
      isOverlord = Current.isRoot,
      clusterType = serviceMeta.getClusterType,
      clusterName = serviceMeta.getCluster,
    )
  end getPlatformInfo

  private def getSubtenant(user: Option[UserComponent]): Option[SubtenantInfo] =
    for
      user <- user if !isAnonymous(user)
      sti  <- subtenantInfo(user)
    yield sti

  // TODO harmonize this with getAdminLink in its right-dependent behavior
  private def getAuthoringLink: Option[String] =
    // TODO: sneak this bit in somewhere
    // ear <- cd.annotation[EnforceAdminRight]
    // if rightService.getUserHasRight(ear.value, ear.`match`)
    servletPath(AuthoringIdentifier)

  private def getAdminLink: Option[String] =
    rightService.getUserHasRight(classOf[AdminRight], RightMatch.ANY) `flatOption` servletPath(AdminIdentifier)

  private def servletPath(ident: String) =
    ident.descriptor.flatMap(_.annotation[ServletBinding]).map(_.path)

  /** Collect various data about the provided user. */
  private def userInfo(user: UserComponent): UserInfo =
    UserInfo(
      component = user,
      rights = user.getDomainRights.asScala.toSet,
      roles = user.getDomainRoleDisplayNames.asScala.toSet,
      preferences = user.getPreferences
    )

  private def subtenantInfo(userComp: UserComponent): Option[SubtenantInfo] =
    val subId     = userComp.getSubtenantId
    val subFacade = Option(fs.getFacade(subId, classOf[SubtenantFacade]))
    subFacade.map(fac =>
      SubtenantInfo(
        tenantId = fac.getTenantId,
        name = fac.getName,
        shortName = fac.getShortName,
        logo = if fac.getLogo != null then ResourceDTO("/api/v2/subtenants/" + subId + "/icon/view") else null
      )
    )
  end subtenantInfo

  private def enrollments(user: UserComponent): Set[EnrollmentComponent] =
    user.getMembership.getEnrollments(ApiQuery.ALL).asScala.toSet

  private def isAnonymous(user: UserComponent): Boolean =
    user.getUserType == UserType.Anonymous
end PlatformInfoApiImpl

object PlatformInfoApiImpl:
  val AdminIdentifier: String     = "loi.platform.admin.Administration"
  val AuthoringIdentifier: String = "loi.authoring.Authoring"

  implicit class CDOps(val cd: ComponentDescriptor) extends AnyVal:
    def annotation[A <: Annotation: ClassTag]: Option[A] =
      cd.getVirtualAnnotations.asScala.flatMap(implicitly[ClassTag[A]].unapply).headOption

  case class SubtenantInfo(tenantId: String, name: String, shortName: String, logo: ResourceDTO)

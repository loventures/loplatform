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

package loi.cp.domain

import com.learningobjects.cpxp.async.async.AsyncOperationActor
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.util.LazySeq
import com.learningobjects.cpxp.component.web.CacheOptions
import com.learningobjects.cpxp.component.{
  ComponentImplementation,
  ComponentInstance,
  ComponentService,
  ComponentSupport
}
import com.learningobjects.cpxp.scala.cpxp.Component.*
import com.learningobjects.cpxp.scala.cpxp.Facade.*
import com.learningobjects.cpxp.service.Current
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.domain.DomainConstants
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.integration.IntegrationConstants
import com.learningobjects.cpxp.service.login.LoginComponent
import com.learningobjects.cpxp.service.query.{Comparison, Direction, Function, QueryService}
import com.learningobjects.cpxp.util.Out
import loi.cp.integration.{LoginSystemComponent, SystemParentFacade}
import loi.cp.session.FallbackLoginProviderComponent
import scaloi.syntax.OptionOps.*

@Component
class DomainRoot(
  val componentInstance: ComponentInstance,
  implicit val fs: FacadeService,
  implicit val qs: QueryService,
)(implicit cs: ComponentService)
    extends DomainRootComponent
    with ComponentImplementation:
  override def getDomain: DomainComponent =
    Current.getDomain.component[DomainComponent]

  override def getDomainSettings: DomainSettingsComponent =
    Current.getDomain.component[DomainSettingsComponent]

  override def loginMechanisms(cacheOptions: Out[CacheOptions]): LazySeq[? <: LoginComponent] =
    val systemsFolder = IntegrationConstants.FOLDER_ID_SYSTEMS.facade[SystemParentFacade]
    cacheOptions.set(CacheOptions.apply(systemsFolder))
    LazySeq {
      val logins = systemsFolder.querySystems
        .addCondition(DataTypes.DATA_TYPE_DISABLED, Comparison.eq, false)
        .addCondition(IntegrationConstants.DATA_TYPE_SYSTEM_ALLOW_LOGIN, Comparison.eq, true)
        .setOrder(IntegrationConstants.DATA_TYPE_SYSTEM_NAME, Function.LOWER, Direction.ASC)
        .getComponents[LoginSystemComponent[?, ? <: LoginComponent]] map {
        _.getLoginComponent
      }
      if logins.isEmpty then fallbackLogin.toList else logins
    }
  end loginMechanisms

  override def testAsync: DomainComponent =
    (1 to 10) foreach { i =>
      Thread.sleep(1000)
      AsyncOperationActor.tellProgress(i.toLong, 10, s"i is $i")
    }
    getDomain

  // Overlord domains have no configured login mechanism so I want to return the fallback so the UI works in that
  // case. However on other domains I don't want to return the mechanism to the UI, because no login mechanism
  // was deliberately configured. When all the front ends send a mechanism id, we will remove fallback login from
  // normal domains.
  private def fallbackLogin: Option[LoginComponent] =
    Option(ComponentSupport.get(classOf[FallbackLoginProviderComponent]))
      .when(isOverlord)
      .map(_.fallbackLoginSystem.getLoginComponent)

  // TODO: Kill this explicit check when we don't have a default direct login mechanism.
  private def isOverlord =
    Current.getDomainDTO.`type` == DomainConstants.DOMAIN_TYPE_OVERLORD
end DomainRoot

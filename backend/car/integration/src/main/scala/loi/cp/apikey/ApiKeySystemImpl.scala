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

package loi.cp.apikey

import com.learningobjects.cpxp.component.annotation.{Component, Configuration}
import com.learningobjects.cpxp.dto.{FacadeComponent, FacadeItem}
import com.learningobjects.cpxp.service.integration.{IntegrationConstants, SystemFacade}
import com.learningobjects.cpxp.service.user.UserType
import com.learningobjects.cpxp.util.StringUtils
import loi.cp.integration.AbstractSystem
import loi.cp.right.{Right, RightService}
import loi.cp.user.UserComponent
import scalaz.syntax.std.option.*
import scaloi.syntax.any.*
import scaloi.syntax.option.*

import scala.jdk.CollectionConverters.*

@Component(name = "API Key", alias = Array("loi.cp.apikey.ApiKeySystem")) // alias for bootstrap
class ApiKeySystemImpl extends AbstractSystem[ApiKeySystem] with ApiKeySystem:
  import ApiKeySystemImpl.*

  override def update(system: ApiKeySystem): ApiKeySystem =
    initUser(system)
    setRights(system.getRights)
    setWhiteList(system.getWhiteList)
    super.update(system)

  @Configuration(label = "$$field_password=Password", `type` = "Password", order = 10)
  override def getKey: String = Option(_self).map(_.getKey).orNull

  def setKey(key: String): Unit =
    _self.setKey(key)

  @Configuration(label = "$$field_rights=Rights", size = 128, order = 11)
  override def getRights: String = Option(_self).map(_.getRights).orNull

  def setRights(rights: String): Unit =
    _self.setRights(rights)

  override def getRightClasses(implicit rs: RightService): Set[Class[? <: Right]] =
    rs.expandRightIds(StringUtils.splitString(getRights).toSeq.asJava).asScala.toSet

  @Configuration(label = "$$field_whiteList=Allowed IPs", size = 128, order = 11)
  override def getWhiteList: String =
    getWhiteListIps.mkString(", ")

  def setWhiteList(whiteList: String): Unit =
    val ips = StringUtils.splitString(whiteList).toSet
    _self.setJsonConfig(getConfig.cata(_.copy(whiteList = ips), ApiKeyConfiguration(ips)))

  override def getWhiteListIps: Set[String] = getConfig.cata(_.whiteList, Set.empty)

  private def getConfig: Option[ApiKeyConfiguration] =
    Option(_self).mapNonNull(_.getJsonConfig(classOf[ApiKeyConfiguration]))

  override def getSystemUser: UserComponent =
    userParent.getOrCreateUser(userInit(this)) // goc for legacy systems

  private def initUser(system: ApiKeySystem): Unit =
    if isCreate then userParent.addUser(userInit(system))
    else
      val user = userParent.getOrCreateUser(userInit(system))
      user.setUserName(system.getSystemId)
      user.setGivenName(system.getName)

  private def isCreate: Boolean = Option(_self.getSystemId).isEmpty

  private def userParent: ApiKeySystemFacade =
    _self.asFacade(classOf[ApiKeySystemFacade])
end ApiKeySystemImpl

object ApiKeySystemImpl:
  private def userInit(system: ApiKeySystem): UserComponent.Init = new UserComponent.Init <| { init =>
    init.userName = system.getSystemId
    init.givenName = system.getName
    init.familyName = FamilyName
    init.userType = UserType.System
    init.url = null
  }

  private val FamilyName = "System"
end ApiKeySystemImpl

@FacadeItem(IntegrationConstants.ITEM_TYPE_SYSTEM)
trait ApiKeySystemFacade extends SystemFacade:
  @FacadeComponent
  def addUser(init: UserComponent.Init): UserComponent
  def getOrCreateUser(init: UserComponent.Init): UserComponent

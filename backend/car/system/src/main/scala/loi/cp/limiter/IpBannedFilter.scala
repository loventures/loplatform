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

package loi.cp.limiter

import com.learningobjects.cpxp.BaseServiceMeta
import com.learningobjects.cpxp.component.AbstractComponent
import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.{FilterBinding, FilterComponent, FilterInvocation}
import com.learningobjects.cpxp.scala.cpxp.Service.*
import com.learningobjects.cpxp.service.component.misc.OverlordDomainConstants
import com.learningobjects.cpxp.service.overlord.OverlordWebService
import com.learningobjects.cpxp.util.HttpUtils
import com.learningobjects.cpxp.util.cache.settings.SystemSettingsCache
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import loi.cp.ip.IpMatch
import loi.cp.overlord.OverlordDomainFacade
import scaloi.syntax.BooleanOps.*

import scala.jdk.CollectionConverters.*

object IpBannedFilter:
  private var ipMatch = IpMatch.empty

  def getBannedIps: Set[String] = getBannedIps(overlordDomain.get)

  def getBannedIps(overlordFacade: OverlordDomainFacade): Set[String] =
    Option(overlordFacade)
      .flatMap(f => Option(f.getBannedIps))
      .fold(Set.empty[String])(_.asScala.toSet)

  def overlordDomain: Option[OverlordDomainFacade] =
    Option(service[OverlordWebService].findOverlordDomain)
      .map(_.asFacade(classOf[OverlordDomainFacade]))

  /** Adds the passed in ip/mask to the existing list of banned ips and stores it persistently. Will validate the string
    * is a valid IP or IP & mask before adding it.
    * @param ip
    *   either an IP address (NNN.NNN.NNN.NNN) or a cidr notation for IP & mask (NNN.NNN.NNN.NNN/MMM)
    * @return
    *   whether the ip was a valid ip or ip/mask.
    */
  def addBannedIp(ip: String)(implicit ssc: SystemSettingsCache): Boolean =
    IpMatch.validateIp(ip) <|? {
      overlordDomain foreach { overlordFacade =>
        val setting = getBannedIps(overlordFacade)
        overlordFacade.setBannedIps((setting + ip).asJava)
        ssc.remove(OverlordDomainConstants.DATA_TYPE_BANNED_IPS)
      }
    }

  def removeBannedIp(ip: String)(implicit ssc: SystemSettingsCache): Unit =
    overlordDomain foreach { overlordFacade =>
      val setting = getBannedIps(overlordFacade)
      overlordFacade.setBannedIps((setting - ip).asJava)
      ssc.remove(OverlordDomainConstants.DATA_TYPE_BANNED_IPS)
    }

  def loadBannedIps()(implicit ssc: SystemSettingsCache): Unit =
    overlordDomain foreach { overlordFacade =>
      val settings = getBannedIps(overlordFacade)
      ipMatch = IpMatch.parse(settings)

      // Cache the new settings.
      ssc.put(
        new SystemSettingsCacheEntry(
          OverlordDomainConstants.DATA_TYPE_BANNED_IPS,
          settings,
        )
      )
    }

  // Look in the cache to see if the banned IPs have changed, but if its still in the cache we don't
  // need to reprocess it.
  def isCacheEmpty(implicit ssc: SystemSettingsCache): Boolean =
    !ssc.test(OverlordDomainConstants.DATA_TYPE_BANNED_IPS)

  def isBanned(ip: String)(implicit ssc: SystemSettingsCache): Boolean =
    if isCacheEmpty then loadBannedIps()
    ipMatch.matches(ip)
end IpBannedFilter

@Component
@FilterBinding(priority = 0, system = true)
class IpBannedFilter(implicit val ssc: SystemSettingsCache) extends AbstractComponent with FilterComponent:
  import IpBannedFilter.*

  override def filter(
    request: HttpServletRequest,
    response: HttpServletResponse,
    invocation: FilterInvocation
  ): Boolean =
    !isBanned(HttpUtils.getRemoteAddr(request, BaseServiceMeta.getServiceMeta)) <|! {
      // TODO: We should asynchronously wait a few seconds before rejecting.
      response.sendError(HttpServletResponse.SC_FORBIDDEN)
    }
end IpBannedFilter

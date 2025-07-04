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

package loi.cp.analytics

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.component.ComponentService
import com.learningobjects.cpxp.component.web.util.JacksonUtils
import com.learningobjects.cpxp.listener.CpxpListener
import com.learningobjects.cpxp.service.ServiceContext
import com.learningobjects.cpxp.service.domain.DomainWebService
import com.learningobjects.cpxp.service.email.EmailService
import com.learningobjects.cpxp.service.facade.FacadeService
import com.learningobjects.cpxp.service.item.ItemService
import com.learningobjects.cpxp.service.overlord.OverlordWebService
import com.learningobjects.cpxp.service.query.QueryService
import loi.cp.bus.BusFailureNotificationService
import loi.cp.config.ConfigurationService
import com.learningobjects.cpxp.scala.cpxp.Service.*

class AnalyticsCpxpListener extends CpxpListener:
  import AnalyticsCpxpListener.*

  override def postComponent(ctx: ServiceContext): Unit =
    logger info "Starting analytics busen by poll"

    AnalyticsServiceImpl.startAnalytics(using
      cs = service[ConfigurationService],
      dws = service[DomainWebService],
      es = service[EmailService],
      is = service[ItemService],
      fs = service[FacadeService],
      qs = service[QueryService],
      ows = service[OverlordWebService],
      fns = service[BusFailureNotificationService],
      sm = ctx.getServiceMeta,
      mapper = mapper,
      cs2 = service[ComponentService]
    )
  end postComponent
end AnalyticsCpxpListener

object AnalyticsCpxpListener:
  private val logger = org.log4s.getLogger

  val mapper: ObjectMapper = JacksonUtils.getMapper

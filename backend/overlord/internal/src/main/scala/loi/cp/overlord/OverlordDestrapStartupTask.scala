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

package loi.cp.overlord

import java.lang as jl

import com.fasterxml.jackson.databind.ObjectMapper
import com.learningobjects.cpxp.service.domain.DomainWebService
import com.learningobjects.cpxp.service.overlord.OverlordWebService
import com.learningobjects.cpxp.startup.StartupTaskScope.Overlord
import com.learningobjects.cpxp.startup.{StartupTask, StartupTaskBinding}
import com.learningobjects.cpxp.util.ManagedUtils
import com.typesafe.config.{Config, ConfigRenderOptions}
import loi.cp.bootstrap.BootstrapInstance
import loi.cp.domain.Destrap

import scala.jdk.CollectionConverters.*

/** Runs overlord destrap from HOCON. For example:
  *
  * loi.cp.destrap: [ { phase: "core.connector.create", config: [ { identifier: "loi.cp.apikey.ApiKeySystemImpl",
  * systemId: "ahoy", name: "Ahoy", key: "secret", rights: "loi.cp.admin.right.HostingAdminRight" } ] } ]
  */
@StartupTaskBinding(version = 20180207, taskScope = Overlord)
class OverlordDestrapStartupTask(config: Config, mapper: ObjectMapper, dws: DomainWebService, ows: OverlordWebService)
    extends StartupTask:
  import OverlordDestrapStartupTask.*

  override def run(): Unit =
    destrap(config.getConfigList("loi.cp.destrap").asScala.toSeq.map(parse))
    dws.setupContext(ows.findOverlordDomainId)

  private def parse(cf: Config): Destrap =
    mapper.readValue(cf.root().render(ConfigRenderOptions.concise.setJson(true)), classOf[Destrap])

  private def destrap(d: Seq[Destrap], context: jl.Long = null): Unit = d foreach {
    case Destrap(phase, config, setup) =>
      logger.info(s"Bootstrap phase $phase")
      Option(BootstrapInstance.lookup(phase)).fold(throw new Exception(s"Unknown bootstrap phase: $phase")) {
        function =>
          if config.exists(_.isArray) && !function.hasCollectionParameter then
            config.get.asScala foreach { cf =>
              function.invoke(context, cf)
            }
            ManagedUtils.commit()
          else
            val subContext = function.invoke(context, config.getOrElse(mapper.nullNode))
            ManagedUtils.commit()
            setup foreach { destrap(_, subContext) }
      }
  }
end OverlordDestrapStartupTask

object OverlordDestrapStartupTask:
  private final val logger = org.log4s.getLogger

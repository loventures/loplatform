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

package loi.cp.prometheus

import com.learningobjects.cpxp.component.annotation.Component
import com.learningobjects.cpxp.component.web.{AbstractComponentServlet, ServletBinding}
import com.learningobjects.cpxp.util.{HttpUtils, S3Statistics}
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import loi.cp.prometheus.collector.{CatalinaCollector, ServiceMetaCollector, SessionServiceCollector}

import scala.util.Using

@Component
@ServletBinding(path = PrometheusServlet.ControlMetrics, system = true)
class PrometheusServlet extends AbstractComponentServlet:
  import PrometheusServlet.*
  import io.prometheus.client.hotspot.DefaultExports

  DefaultExports.initialize
  registry.register(CatalinaCollector)
  registry.register(S3Statistics.collector)
  registry.register(ServiceMetaCollector)
  registry.register(SessionServiceCollector)

  override def get(request: HttpServletRequest, response: HttpServletResponse): Unit =
    Using.resource(HttpUtils.getWriter(request, response)) { writer =>
      TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples)
      TextFormat.write004(writer, registry.metricFamilySamples)
    }
end PrometheusServlet

object PrometheusServlet:
  final val ControlMetrics = "/control/metrics"
  private val registry     = new CollectorRegistry

/*
 * LO Platform copyright (C) 2007–2026 LO Ventures LLC.
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
import io.prometheus.metrics.expositionformats.PrometheusTextFormatWriter
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.simpleclient.bridge.SimpleclientCollector
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import loi.cp.prometheus.collector.{CatalinaCollector, ServiceMetaCollector, SessionServiceCollector}

import scala.util.Using

@Component
@ServletBinding(path = PrometheusServlet.ControlMetrics, system = true)
class PrometheusServlet extends AbstractComponentServlet:
  import PrometheusServlet.*

  override def get(request: HttpServletRequest, response: HttpServletResponse): Unit =
    response.setContentType(PrometheusTextFormatWriter.CONTENT_TYPE)
    Using.resource(HttpUtils.getOutputStream(request, response)) { out =>
      writer.write(out, PrometheusRegistry.defaultRegistry.scrape())
      writer.write(out, registry.scrape())
    }
end PrometheusServlet

object PrometheusServlet:
  final val ControlMetrics = "/control/metrics"

  private val writer = PrometheusTextFormatWriter.create()

  /** Metrics that this servlet owns, as opposed to those that other code registers with the default registry. */
  private val registry = new PrometheusRegistry

  // registered once, when this object initialises; re-registering a collector is an error
  JvmMetrics.builder.register(registry)
  registry.register(CatalinaCollector)
  registry.register(S3Statistics.collector)
  registry.register(ServiceMetaCollector)
  registry.register(SessionServiceCollector)

  // HikariCP still publishes its pool metrics through the 0.x simpleclient registry
  SimpleclientCollector.builder.register(registry)
end PrometheusServlet

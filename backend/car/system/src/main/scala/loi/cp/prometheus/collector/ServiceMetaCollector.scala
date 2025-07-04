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

package loi.cp.prometheus.collector

import com.learningobjects.cpxp.filter.SendFileFilter
import com.learningobjects.cpxp.{BaseServiceMeta, ServiceMeta}
import io.prometheus.client.Collector.MetricFamilySamples
import io.prometheus.client.{Collector, GaugeMetricFamily}

import java.util
import scala.jdk.CollectionConverters.*

object ServiceMetaCollector extends Collector:
  val serviceMeta: ServiceMeta                           = BaseServiceMeta.getServiceMeta
  override def collect(): util.List[MetricFamilySamples] =

    List[MetricFamilySamples](
      new GaugeMetricFamily(
        "cp_info",
        "DE version info",
        util.List.of("name", "host", "build", "version", "cluster", "nio_enabled")
      ).addMetric(
        util.List.of(
          serviceMeta.getNode,
          serviceMeta.getLocalHost,
          serviceMeta.getBuild,
          serviceMeta.getVersion,
          serviceMeta.getCluster,
          SendFileFilter.isNioAvailable.toString
        ),
        1L
      )
    ).asJava
end ServiceMetaCollector

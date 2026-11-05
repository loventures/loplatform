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

package loi.cp.prometheus.collector

import com.learningobjects.cpxp.filter.SendFileFilter
import com.learningobjects.cpxp.{BaseServiceMeta, ServiceMeta}
import io.prometheus.metrics.model.registry.MultiCollector
import io.prometheus.metrics.model.snapshots.GaugeSnapshot.GaugeDataPointSnapshot
import io.prometheus.metrics.model.snapshots.{GaugeSnapshot, Labels, MetricSnapshots}

object ServiceMetaCollector extends MultiCollector:
  val serviceMeta: ServiceMeta = BaseServiceMeta.getServiceMeta

  override def collect(): MetricSnapshots =
    MetricSnapshots.of(
      GaugeSnapshot.builder
        .name("cp_info")
        .help("DE version info")
        .dataPoint(
          GaugeDataPointSnapshot.builder
            .labels(
              Labels.of(
                "name",
                serviceMeta.getNode,
                "host",
                serviceMeta.getLocalHost,
                "build",
                serviceMeta.getBuild,
                "version",
                serviceMeta.getVersion,
                "cluster",
                serviceMeta.getCluster,
                "nio_enabled",
                SendFileFilter.isNioAvailable.toString
              )
            )
            .value(1L)
            .build
        )
        .build
    )
end ServiceMetaCollector

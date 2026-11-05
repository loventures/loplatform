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

import io.prometheus.metrics.model.registry.MultiCollector
import io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot
import io.prometheus.metrics.model.snapshots.GaugeSnapshot.GaugeDataPointSnapshot
import io.prometheus.metrics.model.snapshots.{CounterSnapshot, GaugeSnapshot, Labels, MetricSnapshots}

import java.lang.management.ManagementFactory
import javax.management.ObjectName
import scala.jdk.CollectionConverters.*

object CatalinaCollector extends MultiCollector:

  private val mbs             = ManagementFactory.getPlatformMBeanServer
  private val catalinaManager = new ObjectName("*:type=Manager,context=/,host=localhost")
  private val threadPools     = new ObjectName("*:type=ThreadPool,name=*")

  override def collect(): MetricSnapshots =

    val catalinaCurrentThreadCount = CounterSnapshot.builder
      .name("catalina_total_threads_total")
      .help("The total number of threads for connector")

    val catalinaBusyThreadCount = CounterSnapshot.builder
      .name("catalina_busy_threads_total")
      .help("The number of busy threads for connector")

    val catalinaMaxThreadCount = CounterSnapshot.builder
      .name("catalina_maximum_threads_total")
      .help("The maximum number of threads for connector")

    for threadPool <- mbs.queryNames(threadPools, null).asScala do
      val connector = Labels.of(
        "connector",
        threadPool
          .getKeyProperty("name")
          .replaceAll("\"", "")
      )
      catalinaCurrentThreadCount.dataPoint(
        counter(connector, mbs.getAttribute(threadPool, "currentThreadCount"))
      )
      catalinaBusyThreadCount.dataPoint(
        counter(connector, mbs.getAttribute(threadPool, "currentThreadsBusy"))
      )
      catalinaMaxThreadCount.dataPoint(
        counter(connector, mbs.getAttribute(threadPool, "maxThreads"))
      )
    end for

    val snapshots = MetricSnapshots.builder
      .metricSnapshot(catalinaCurrentThreadCount.build)
      .metricSnapshot(catalinaBusyThreadCount.build)
      .metricSnapshot(catalinaMaxThreadCount.build)

    // there is at most one manager for the root context of the localhost host
    for catalina <- mbs.queryNames(catalinaManager, null).asScala.headOption do
      snapshots
        .metricSnapshot(
          GaugeSnapshot.builder
            .name("catalina_active_sessions")
            .help("the number of currently active sessions")
            .dataPoint(gauge(mbs.getAttribute(catalina, "activeSessions")))
            .build
        )
        .metricSnapshot(
          CounterSnapshot.builder
            .name("catalina_session_total")
            .help("the total number of sessions created by this manager")
            .dataPoint(counter(Labels.EMPTY, mbs.getAttribute(catalina, "sessionCounter")))
            .build
        )

    snapshots.build
  end collect

  private def counter(labels: Labels, value: AnyRef): CounterDataPointSnapshot =
    CounterDataPointSnapshot.builder.labels(labels).value(value.asInstanceOf[Number].doubleValue()).build

  private def gauge(value: AnyRef): GaugeDataPointSnapshot =
    GaugeDataPointSnapshot.builder.value(value.asInstanceOf[Number].doubleValue()).build
end CatalinaCollector

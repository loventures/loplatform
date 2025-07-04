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

import io.prometheus.client.Collector.MetricFamilySamples
import io.prometheus.client.{Collector, CounterMetricFamily, GaugeMetricFamily}

import java.lang.management.ManagementFactory
import java.util
import java.util.Collections
import javax.management.ObjectName
import scala.jdk.CollectionConverters.*

object CatalinaCollector extends Collector:

  private val mbs             = ManagementFactory.getPlatformMBeanServer
  private val catalinaManager = new ObjectName("*:type=Manager,context=/,host=localhost")
  private val threadPools     = new ObjectName("*:type=ThreadPool,name=*")

  override def collect(): util.List[MetricFamilySamples] =

    val catalinaCurrentThreadCount = new CounterMetricFamily(
      "catalina_total_threads_total",
      "The total number of threads for connector",
      Collections.singletonList("connector")
    )

    val catalinaBusyThreadCount = new CounterMetricFamily(
      "catalina_busy_threads_total",
      "The number of busy threads for connector",
      Collections.singletonList("connector")
    )

    val catalinaMaxThreadCount = new CounterMetricFamily(
      "catalina_maximum_threads_total",
      "The maximum number of threads for connector",
      Collections.singletonList("connector")
    )

    for threadPool <- mbs.queryNames(threadPools, null).asScala do
      val prefix = threadPool
        .getKeyProperty("name")
        .replaceAll("\"", "")
      catalinaCurrentThreadCount.addMetric(
        Collections.singletonList(prefix),
        mbs.getAttribute(threadPool, "currentThreadCount").asInstanceOf[Number].doubleValue()
      )
      catalinaBusyThreadCount.addMetric(
        Collections.singletonList(prefix),
        mbs.getAttribute(threadPool, "currentThreadsBusy").asInstanceOf[Number].doubleValue()
      )
      catalinaMaxThreadCount.addMetric(
        Collections.singletonList(prefix),
        mbs.getAttribute(threadPool, "maxThreads").asInstanceOf[Number].doubleValue()
      )
    end for

    val gaugeMetricFamilies = mbs
      .queryNames(catalinaManager, null)
      .asScala
      .flatMap(catalina =>
        List[MetricFamilySamples](
          new GaugeMetricFamily(
            "catalina_active_sessions",
            "the number of currently active sessions",
            mbs.getAttribute(catalina, "activeSessions").asInstanceOf[Number].doubleValue()
          ),
          new CounterMetricFamily(
            "catalina_session_total",
            "the total number of sessions created by this manager",
            mbs.getAttribute(catalina, "sessionCounter").asInstanceOf[Number].doubleValue()
          )
        )
      )

    (gaugeMetricFamilies.toSet + catalinaCurrentThreadCount + catalinaBusyThreadCount + catalinaMaxThreadCount).toList.asJava
  end collect
end CatalinaCollector

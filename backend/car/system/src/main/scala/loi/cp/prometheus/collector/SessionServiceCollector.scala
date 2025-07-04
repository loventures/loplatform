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

import com.learningobjects.cpxp.scala.cpxp.Summon.summon
import com.learningobjects.cpxp.service.session.SessionService
import io.prometheus.client.Collector.MetricFamilySamples
import io.prometheus.client.{Collector, CounterMetricFamily}

import java.util

object SessionServiceCollector extends Collector:

  val sessionService = summon[SessionService]

  override def collect(): util.List[Collector.MetricFamilySamples] =
    val mfs            = new util.ArrayList[MetricFamilySamples]
    val cpSessionCount = new CounterMetricFamily(
      "cp_sessions_total",
      "the number of cp sessions",
      sessionService.getActiveSessionCount.toDouble
    )

    mfs.add(cpSessionCount)
    mfs
  end collect
end SessionServiceCollector

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

import com.learningobjects.cpxp.scala.cpxp.Summon.summon
import com.learningobjects.cpxp.service.session.SessionService
import io.prometheus.metrics.model.registry.MultiCollector
import io.prometheus.metrics.model.snapshots.CounterSnapshot.CounterDataPointSnapshot
import io.prometheus.metrics.model.snapshots.{CounterSnapshot, MetricSnapshots}

object SessionServiceCollector extends MultiCollector:

  val sessionService = summon[SessionService]

  override def collect(): MetricSnapshots =
    MetricSnapshots.of(
      CounterSnapshot.builder
        .name("cp_sessions_total")
        .help("the number of cp sessions")
        .dataPoint(CounterDataPointSnapshot.builder.value(sessionService.getActiveSessionCount.toDouble).build)
        .build
    )
end SessionServiceCollector

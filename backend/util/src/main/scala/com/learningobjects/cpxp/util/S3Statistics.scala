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

package com.learningobjects.cpxp.util

import org.apache.commons.collections4.queue.CircularFifoQueue
import java.util
import java.util.Collections

import io.prometheus.client.{Collector, CounterMetricFamily, GaugeMetricFamily}
import io.prometheus.client.Collector.MetricFamilySamples

import scala.jdk.CollectionConverters.*
import scala.concurrent.duration.*
import scalaz.std.anyVal.*
import scalaz.std.iterable.*
import scalaz.std.tuple.*
import scalaz.syntax.foldable.*
import scalaz.syntax.std.boolean.* // boolean folds

/** S3 statistics accumulator. Fails to capture S3 write error rate as a distinct thing despite it being interesting.
  * Fails to capture actual download time despite it being interesting.
  */
class S3Statistics(statisticsWindow: Int, sizeLimit: Long, maxAge: FiniteDuration):
  private final val histora = new CircularFifoQueue[Historum](statisticsWindow)

  private var operationCount: Long = 0

  private var errorCount: Long = 0

  /** Total S3 operations. */
  def operations: Long = operationCount

  /** Total S3 errors. */
  def errors: Long = errorCount

  /** Recent S3 error rate [0..1]. */
  def errorRate: Double = histora synchronized {
    val minTime = System.currentTimeMillis - maxAge.toMillis
    histora.asScala
      .filter(_.start >= minTime)                  // consider only recent entries
      .foldMap1Opt(h => (h.success.fold(0, 1), 1)) // accumulate the failure count and the total count
      .fold(0.0)(t => t._1.toDouble / t._2)        // if no results then zero, else sum error count over total count
  }

  /** Recent S3 response time in milliseconds. */
  def responseTime: Double = histora synchronized {
    val minTime = System.currentTimeMillis - maxAge.toMillis
    histora.asScala
      .filter(h => h.size.exists(_ < sizeLimit) && (h.start >= minTime)) // consider only recent and small entries
      .foldMap1Opt(h => (h.duration, 1))                                 // accumulate the duration and the total count
      .fold(0.0)(t => t._1.toDouble / t._2)                              // if no results then zero, else sum duration over total count
  }

  /** Begin a transaction. */
  def begin(size: Option[Long]): S3Transaction = new Historum(size)

  /** Record a transaction. */
  private def record(hist: Historum): Unit = histora synchronized {
    operationCount = operationCount + 1
    if !hist.success then errorCount = errorCount + 1
    histora.offer(hist)
  }

  /** A single history entry. */
  private class Historum(val size: Option[Long]) extends S3Transaction:

    /** Transaction start time. */
    private[S3Statistics] val start = System.currentTimeMillis

    /** Transaction success. */
    private[S3Statistics] var success = false

    /** Transaction duration. */
    private[S3Statistics] var duration = 0L

    /** Record success. */
    override def succeeded(): Unit = complete(ok = true, System.currentTimeMillis - start)

    /** Record failure. */
    override def failed(): Unit = complete(ok = false, System.currentTimeMillis - start)

    /** Record completion. */
    override def complete(ok: Boolean, dur: Long): Unit =
      success = ok
      duration = dur
      record(this)
  end Historum
end S3Statistics

/** S3 statistics companion. */
object S3Statistics:

  /** Size limit at which we stop tracking the duration of an S3 operation for performance statistics purposes because
    * the transfer of the content may dominate the operation duration. That is, we don't want a 100G upload to trigger
    * an S3 slow performance alert.
    */
  final val DefaultStatisticsSizeLimit = 128L * 1024

  /** The number of operations over which to aggregate statistics. */
  final val DefaultStatisticsWindow = 100

  /** The maximum statistic age to aggregate. */
  final val DefaultStatisticsMaxAge = 15.minutes

  /** Default instances. */
  private val instances = new util.HashMap[String, S3Statistics]

  /** Get the S3 statistics for a given provider. */
  def apply(provider: String): S3Statistics = instances synchronized {
    instances.computeIfAbsent(
      provider,
      _ => new S3Statistics(DefaultStatisticsWindow, DefaultStatisticsSizeLimit, DefaultStatisticsMaxAge)
    )
  }

  /** Put S3 status into mstatus. */
  def updateStatus(map: util.Map[String, Any]): Unit = instances synchronized {
    instances forEach { case (key: String, stats: S3Statistics) =>
      map.put(s"s3.$key.OperationCount", stats.operations)
      map.put(s"s3.$key.ErrorCount", stats.errors)
      map.put(s"s3.$key.ErrorRate", stats.errorRate)
      map.put(s"s3.$key.ResponseTime", stats.responseTime)
    }
  }

  val collector = new Collector:
    override def collect(): util.List[MetricFamilySamples] =
      val mfs = new util.ArrayList[MetricFamilySamples]

      val errorCount = new CounterMetricFamily(
        "s3_errors_total",
        "The maximum number of errors for provider",
        Collections.singletonList("provider")
      )

      val errorRate = new GaugeMetricFamily(
        "s3_error_rate",
        "Error rate for provider",
        Collections.singletonList("provider")
      )

      val operationCount = new CounterMetricFamily(
        "s3_operations_total",
        "The number of s3 operations for the provider",
        Collections.singletonList("provider")
      )

      val responseTime = new GaugeMetricFamily(
        "s3_average_response_millis",
        "the average response time over recent transactions",
        Collections.singletonList("provider")
      )

      mfs.add(errorCount)
      mfs.add(errorRate)
      mfs.add(operationCount)
      mfs.add(responseTime)

      instances forEach { case (key: String, stats: S3Statistics) =>
        errorCount.addMetric(Collections.singletonList(s"${key}"), stats.errors.toDouble)
        errorRate.addMetric(Collections.singletonList(s"${key}"), stats.errorRate)
        operationCount.addMetric(Collections.singletonList(s"${key}"), stats.operations.toDouble)
        responseTime.addMetric(Collections.singletonList(s"${key}"), stats.responseTime)
      }
      mfs
    end collect
end S3Statistics

/** S3 transaction wrapper. */
trait S3Transaction:

  /** Report success of the transaction. */
  def succeeded(): Unit

  /** Report failure of the transaction. */
  def failed(): Unit

  /** For testing only: Report completion. */
  private[util] def complete(ok: Boolean, dur: Long): Unit
end S3Transaction

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

import java.util.concurrent.Executors
import java.util.function.Consumer
import java.lang as jl

import scala.jdk.CollectionConverters.*
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.control.NonFatal

/** Utility for optionally running startup operations in parallel.
  */
object ParallelStartup:
  private final val logger = org.log4s.getLogger

  // The global execution context targets the available CPU count, but some of our startup services block
  // without informing the executor. This causes startup timing to be a bit unstable as some steps get
  // blocked on peer steps. Using a wide executor gives more reliable startup timing which allows efforts
  // to more accurately target the long poles in our startup timing.
  private lazy val es: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(128))

  // Use an aggressive execution context during startup, then switch to the global executor
  implicit def ec: ExecutionContext = if es.isShutdown then ExecutionContext.global else es

  /** Whether or not to parallelise startup. */
  val parallel = true

  /** Run a computation asynchronously, if parallel mode, else synchronously. */
  def async(f: () => Unit): Unit =
    if parallel then Future(f()).recover({ case NonFatal(e) => logger.warn(e)("Async startup failure") })
    else f()

  /** Run a sequence of computations in parallel, if parallel mode, else serially. */
  def foreach[A](aa: List[A])(f: A => Unit): Unit =
    if parallel then
      mapF(aa)(a => Future(f(a)).recover({ case NonFatal(e) => logger.warn(e)("Async startup failure") }))
    else aa.foreach(f)

  /** Run a sequence of computations in parallel. */
  def mapF[A, B](aa: List[A])(f: A => Future[B]): List[B] =
    Await.result(Future.sequence(aa.map(a => f(a))), Duration.Inf)

  /** Run a sequence of computations in parallel, if parallel mode, else serially. */
  def foreach[A](aa: jl.Iterable[A], f: Consumer[A]): Unit =
    foreach(aa.asScala.toList)(f.accept)

  def shutdown(): Unit = es.shutdown()
end ParallelStartup

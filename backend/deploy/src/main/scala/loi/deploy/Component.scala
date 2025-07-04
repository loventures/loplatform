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

package loi.deploy

import java.util.ServiceLoader

import cats.effect.{Clock, Sync}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import com.learningobjects.cpxp.component.ComponentManager
import com.learningobjects.cpxp.listener.CpxpListener
import com.learningobjects.cpxp.service.ServiceContext
import com.learningobjects.cpxp.service.bootstrap.BootstrapService
import com.learningobjects.cpxp.util.{ManagedUtils, ParallelStartup}
import com.typesafe.config.Config
import loi.deploy.Benchmark.Bench

trait Component[F[_]]:
  def scan: F[Unit]
  def startup(config: Config): F[Unit]
object Component:
  final class Default[F[_]](implicit F: Sync[F])                  extends Component[F]:
    private final val logger = org.log4s.getLogger

    def listeners: F[ServiceLoader[CpxpListener]] = F.pure(ServiceLoader.load(classOf[CpxpListener]))

    override def scan: F[Unit] = F.delay {
      ComponentManager.getComponentRing
    }

    override def startup(config: Config): F[Unit] =
      for
        loader <- listeners
        bss    <- F.delay(ManagedUtils.getService(classOf[BootstrapService]))
        _      <- F.delay {
                    ComponentManager.startup()
                    loader.forEach(l => l.postComponent(ServiceContext.getContext))
                  }
        _      <- F.delay {
                    // TODO: Convert to Async
                    new Thread(
                      () =>
                        ManagedUtils.perform(() =>
                          try
                            val begin = System.currentTimeMillis
                            logger.info("Startup start.")
                            bss.startup()
                            val delta = System.currentTimeMillis - begin
                            logger.info(s"Startup end after ${delta}ms.")
                            ParallelStartup.shutdown()
                          catch
                            case th: Throwable =>
                              logger.warn(th)("Startup error")
                        ),
                      "Startup"
                    ).start()
                  }
      yield ()
  end Default
  final class Benched[F[_]](implicit F: Sync[F], clock: Clock[F]) extends Component[Bench[F, *]]:
    val default                                          = new Default[F]()
    override def scan: Bench[F, Unit]                    = Benchmark(default.scan, "Component Scan")
    override def startup(config: Config): Bench[F, Unit] = Benchmark(default.startup(config), "Component Environment")
end Component

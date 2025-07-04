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

import cats.Parallel
import cats.effect.{Clock, Sync}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.parallel.*
import cats.instances.list.*
import javax.ejb.Local
import com.learningobjects.cpxp.listener.CpxpListener
import com.learningobjects.cpxp.service.ServiceContext
import com.learningobjects.cpxp.service.bootstrap.BootstrapService
import com.learningobjects.cpxp.service.upgrade.UpgradeService
import com.learningobjects.cpxp.util.ManagedUtils
import loi.DifferenceEngine.withEntityManager
import loi.deploy.Benchmark.Bench
import io.github.classgraph.ScanResult

import scala.jdk.CollectionConverters.*

trait Bootstrap[F[_]]:
  def services(cg: ScanResult): F[Unit]
  def bootstrap: F[Unit]
  def unstrap: F[Unit]
object Bootstrap:
  final class Default[F[_]](implicit F: Sync[F], PF: Parallel[F]) extends Bootstrap[F]:

    /** Create an instance of BootstrapService.
      */
    def bootstrapService: F[BootstrapService] = F.delay(ManagedUtils.getService(classOf[BootstrapService]))

    def upgradeService: F[UpgradeService] = F.delay(ManagedUtils.getService(classOf[UpgradeService]))

    def listeners: F[ServiceLoader[CpxpListener]] = F.delay(ServiceLoader.load(classOf[CpxpListener]))

    override def services(cg: ScanResult): F[Unit] = F.delay {
      cg.getClassesWithAnnotation(classOf[Local]).loadClasses().asScala foreach { cls =>
        ManagedUtils.getService(cls)
      }
    }

    override def bootstrap: F[Unit] =
      for
        uss          <- upgradeService
        bss          <- bootstrapService
        loader       <- listeners
        initDomain    = withEntityManager.use(_ => F.delay(uss.initDomains()))
        initScheduler = F.delay(bss.scheduleTasks())
        sc            = ServiceContext.getContext
        initListeners = loader.asScala.toList.map(l => F.delay(l.postBootstrap(sc)))
        _            <- (initDomain :: initScheduler :: initListeners).parSequence_
      yield ()

    override def unstrap: F[Unit] =
      for
        bss <- bootstrapService
        _   <- F.delay(bss.shutdown())
      yield ()
  end Default

  final class Benched[F[_], PF[_]](implicit F: Sync[F], PF: Parallel[F], clock: Clock[F])
      extends Bootstrap[Bench[F, *]]:
    val default                                           = new Default[F]()
    override def services(cg: ScanResult): Bench[F, Unit] = Benchmark(default.services(cg), "Services")
    override def bootstrap: Bench[F, Unit]                = Benchmark(default.bootstrap, "Bootstrap")
    override def unstrap: Bench[F, Unit]                  = Benchmark(default.unstrap, "Unstrapping Boots")
end Bootstrap

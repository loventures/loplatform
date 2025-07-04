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

package loi

import _root_.de.tomcat.DETomcatServer
import _root_.doobie.*
import org.apache.pekko.actor.ActorSystem
import cats.*
import cats.effect.Resource.ExitCase.*
import cats.effect.syntax.all.*
import cats.effect.unsafe.implicits.global
import cats.effect.{Resource as CatsResource, *}
import cats.instances.list.*
import cats.syntax.all.*
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.coherence.ItemCacheActor
import com.learningobjects.cpxp.dto.Ontology
import com.learningobjects.cpxp.locache.CacheReplicationActor
import com.learningobjects.cpxp.shutdown.ShutdownActor
import com.learningobjects.cpxp.util.*
import com.typesafe.config.Config
import loi.deploy.Benchmark.*
import loi.deploy.*
import loi.network.{DBAppClusterStatus, Transactional}
import org.hibernate.internal.SessionFactoryImpl
import org.http4s.server.Server
import io.github.classgraph.ScanResult

import jakarta.persistence.spi.PersistenceUnitInfo
import jakarta.persistence.{EntityManager, EntityManagerFactory}
import javax.sql.DataSource
import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

/** Difference Engine. (Insert propaganda about what the product does.)
  *
  * This is the main entry point for the product. Specifically here we read configuration and the command line options,
  * start tomcat for serving http, connect to the cluster over, set up persistence to the database using Hibernate and
  * JDBC and initialize the Component Framework(tm).
  */
object DifferenceEngine extends IOApp:
  type BenchIO[A]    = Bench[IO, A]
  type BenchParIO[A] = Bench[IO.Par, A]

  private val awaitShutdown = Await()

  override def run(args: List[String]): IO[ExitCode] =
    val server = appStartup[BenchIO, BenchParIO](
      new Startup.Benched[IO],
      new Persistence.Benched[IO],
      new SchemaCheck.Benched[IO, IO.Par],
      SchemaUpdate.Benched[IO, IO.Par],
      new Bootstrap.Benched[IO, IO.Par],
      new Component.Benched[IO],
      new Pekko.Benched[IO]
    )

    printGraph(server.allocated)
      .flatTap(_ => Benchmark.liftF(IO(Notification.notifySuccess())))
      .flatMap({ case (_, shutdown) =>
        Benchmark.liftF(awaitShutdown.block).guarantee(shutdown)
      })
      .run
      .as(ExitCode.Success)
  end run

  override protected def reportFailure(err: Throwable): IO[Unit] =
    super.reportFailure(err).as(Notification.notifyFailure())

  def appStartup[F[_]: Async, PF[_]](
    startup: Startup[F],
    db: Persistence[F],
    check: SchemaCheck[F],
    update: SchemaUpdate[F],
    boot: Bootstrap[F],
    component: Component[F],
    pekko: Pekko[F]
  )(implicit F: Parallel[F]): CatsResource[F, Server] =
    import Parallel.*

    def setupInject(conf: Config, ds: DataSource, cg: ScanResult): F[(ActorSystem, DETomcatServer[F], Ontology)] =
      for
        onto         <- db.ontology(cg)
        _            <- startup.di(conf, ds, onto)
        (as, tomcat) <- onInjectReady(conf, ds, cg)
      yield (as, tomcat, onto)

    def onInjectReady(conf: Config, ds: DataSource, cg: ScanResult): F[(ActorSystem, DETomcatServer[F])] =
      val transactor    =
        Transactor.fromDataSource[F](ds, ExecutionContext.global)
      val clusterStatus = Transactional(transactor, DBAppClusterStatus(conf))
      parMap2(
        pekko.start(conf, clusterStatus),
        boot.services(cg) *> startup.tomcat(conf)
      )((_, _))

    def onConfigReady(config: Config) =
      startup.prelude(config).as(config)

    /** Store the schema checksum in the database if it has changed.
      */
    def storeChecksum(ds: DataSource, schema: SchemaState) =
      if schema.schemaChanged then check.storeChecksum(ds, schema.current)
      else Monad[F].point(())

    /** Read Configuration and setup logging
      */
    def loadConfig: F[Config] = startup.config >>= onConfigReady

    def initAll(config: Config): F[
      (PersistenceUnitInfo, ScanResult, EntityManagerFactory, SchemaState, ActorSystem, DETomcatServer[F], Ontology)
    ] =
      parMap2(
        postConfig(config),
        check.preloadDoobie, // doobie is slow to get ready so just start preloading early
      )((a, _) => a)

    def postConfig(config: Config): F[
      (PersistenceUnitInfo, ScanResult, EntityManagerFactory, SchemaState, ActorSystem, DETomcatServer[F], Ontology)
    ] =
      for
        (pui, cg)                          <- step1(config)
        (emf, cksum, as, server, ontology) <- step2(config, pui, cg)
      yield (pui, cg, emf, cksum, as, server, ontology)

    def step1(config: Config): F[(PersistenceUnitInfo, ScanResult)] =
      parMap3(
        db.persistenceUnitInfo(config),
        startup.reflect,
        startup.cleanup(config)
      )((pui, cg, _) => (pui, cg))

    def step2(
      config: Config,
      pui: PersistenceUnitInfo,
      cg: ScanResult,
    ): F[(EntityManagerFactory, SchemaState, ActorSystem, DETomcatServer[F], Ontology)] =
      val ds = pui.getNonJtaDataSource
      parMap3(
        step3(config, ds, pui, cg),
        component.scan,
        setupInject(config, ds, cg),
      )((sa, _, si) => (sa._1, sa._2, si._1, si._2, si._3))
    end step2

    def step3(
      config: Config,
      ds: DataSource,
      pui: PersistenceUnitInfo,
      cg: ScanResult
    ): F[(EntityManagerFactory, SchemaState)] =
      for
        schema <- parMap2(update.preJPA(ds), check.schemaState(ds, cg))((_, ss) => ss)
        emf    <- db.entityManagerFactory(config, schema.schemaChanged, pui)
      yield (emf, schema)

    /** Do post hibernate DDL changes.
      */
    def postHibernate(meta: ServiceMeta, ds: DataSource, ontology: Ontology, schema: SchemaState) =
      parMap3(
        update.postJPA(meta, ds, ontology, schema.schemaChanged),
        update.addForeignKeys(meta, ds, ForeignKey.keys, schema.schemaChanged),
        storeChecksum(ds, schema)
      )((_, _, _))

    /** Set up cache replication.
      */
    def initReplication(conf: Config, emf: EntityManagerFactory, as: ActorSystem) =
      Monad[F].point {
        if conf.getBoolean("com.learningobjects.cpxp.cache.replicated") then
          CacheReplicationActor.startActors(emf.unwrap(classOf[SessionFactoryImpl]))(using as)
          ItemCacheActor.startActor()(using as)
      }

    val acquire =
      for // this is a bit messy because IO is not memoized so we can't just express a dependency graph and evaluate
        conf                                    <- loadConfig
        (pui, cg, emf, cksum, as, tomcat, onto) <- initAll(conf)
        meta                                    <- startup.meta(conf, emf)
        _                                       <- postHibernate(meta, pui.getNonJtaDataSource, onto, cksum)
        _                                       <- initReplication(conf, emf, as)
        _                                       <- boot.bootstrap
        _                                       <- component.startup(conf)

        // Define effects for shutdown
        shutdown     = (for
                         _ <- tomcat.shutdown
                         _ <- parMap2(pekko.shutdown(as), boot.unstrap)((_, _))
                       yield ())
        shutdownHook = new Thread():
                         override def run(): Unit = awaitShutdown.trigger.unsafeRunSync()
        _            = ShutdownActor.initialize(shutdownHook.run)
        _            = Runtime.getRuntime.addShutdownHook(shutdownHook)
      yield (tomcat, shutdown)

    CatsResource(acquire).widen[Server]
  end appStartup

  @inline def di[T](implicit ev: ClassTag[T]): T = ManagedUtils.newInstance(ev.runtimeClass.asInstanceOf[Class[T]])

  def withEntityManager[F[_]: Sync]: CatsResource[F, EntityManager] =
    cats.effect.Resource.makeCase(acquire[F])((_, exit) =>
      exit match
        case Succeeded   => end[F]
        case Errored(th) => rollback[F] *> Sync[F].raiseError[Unit](th)
        case Canceled    => rollback[F]
    )

  private def acquire[F[_]: Sync]: F[EntityManager] =
    Sync[F].delay({
      ManagedUtils.begin(); ManagedUtils.getEntityContext.getEntityManager
    })
  private def end[F[_]: Sync]: F[Unit]              = Sync[F].delay(ManagedUtils.end())
  private def rollback[F[_]: Sync]: F[Unit]         = Sync[F].delay(ManagedUtils.rollback())
end DifferenceEngine

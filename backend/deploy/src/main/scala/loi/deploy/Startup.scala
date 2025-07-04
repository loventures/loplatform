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

import cats.effect.*
import cats.effect.std.Dispatcher
import cats.instances.list.*
import cats.syntax.all.*
import cats.{effect, ~>}
import com.learningobjects.cpxp.component.ComponentManager
import com.learningobjects.cpxp.component.eval.InferEvaluator
import com.learningobjects.cpxp.dto.Ontology
import com.learningobjects.cpxp.filter.*
import com.learningobjects.cpxp.schedule.Scheduler
import com.learningobjects.cpxp.servlet.{EventServlet, JavascriptExceptionServlet, NameServlet, StaticServlet}
import com.learningobjects.cpxp.util.*
import com.learningobjects.cpxp.{BaseServiceMeta, CpxpClasspath, CpxpCookieProcessor, ServiceMeta}
import com.typesafe.config.{Config, ConfigFactory}
import de.tomcat.config.DETomcatConfig
import de.tomcat.{DETomcatBuilder, DETomcatServer}
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import jakarta.persistence.EntityManagerFactory
import jakarta.servlet.*
import jakarta.servlet.http.*
import loi.DifferenceEngine
import loi.DifferenceEngine.withEntityManager
import loi.cp.i18n.*
import loi.cp.mail.Mail
import loi.cp.presence.PresenceHttpSessionListener
import loi.deploy.Benchmark.Bench
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.reflect.TypeUtils
import io.github.classgraph.ScanResult

import java.io.File
import java.util
import javax.mail.Session
import javax.sql.DataSource
import scala.concurrent.ExecutionContext

/** Some startup functions. No better place to put them.
  */
trait Startup[F[_]]:

  /** Load configuration.
    */
  def config: F[Config]

  /** Reflect.
    */
  def reflect: F[ScanResult]

  /** Stuff that should be done early as soon a configuration is read.
    */
  def prelude(conf: Config): F[Unit]

  /** Run some cleanup before things get going.
    */
  def cleanup(conf: Config): F[Unit]

  /** Start tomcat and start serving pages.
    */
  def tomcat(config: Config): F[DETomcatServer[F]]

  /** Initialize our weird service meta thing.
    */
  def meta(config: Config, emf: EntityManagerFactory): F[ServiceMeta]

  /** Set up Side-effecty Evaluators.
    */
  def di(
    config: Config,
    dataSource: DataSource,
    ontology: Ontology,
  ): F[Unit]

  def threadPools(c: Config): cats.effect.Resource[F, ThreadPools]
end Startup
object Startup:
  final class Default[F[_]](implicit F: Async[F]) extends Startup[F]:

    override def config: F[Config] =
      F.delay { ConfigFactory.load() }

    override def reflect: F[ScanResult] = F.delay {
      CpxpClasspath.reflect()
    }

    /** Hack a few things together as early as possible. Here specifically we monkey around with loggers and set system
      * properties.
      * @return
      */
    override def prelude(config: Config): F[Unit] =
      DETomcatBuilder.loggingSetup[F](DETomcatConfig.load(config)) *> F.delay {
        /* A certain flea-ridden feline has a nasty habit of re-using async response
         * objects whilst they're still being used. */
        System.setProperty("org.apache.catalina.connector.RECYCLE_FACADES", "true")
        /* Stop ehcache from phoning home. */
        System.setProperty("net.sf.ehcache.skipUpdateCheck", "true")

        // Suppress log4j logging for reasons. Reasons to do with its gross verbosity.
        org.apache.logging.log4j.LogManager.getRootLogger()
        org.apache.logging.log4j.core.config.Configurator.setRootLevel(org.apache.logging.log4j.Level.OFF)

        HttpsUtils.setLenientCertPolicy()

        Instrumentation.configure(
          config.getBoolean("apm.enabled"),
          config.getBoolean("apm.trace"),
          config.getBoolean("apm.println"),
        )

        def toMap(config: Config): java.util.Map[String, String] =
          val map = new util.HashMap[String, String]()
          config.entrySet().forEach(entry => map.put(entry.getKey, config.getString(entry.getKey)))
          map
        val clusterName                                          = config.getString("com.learningobjects.cpxp.network.cluster.name")
        GuidUtil.configure(clusterName)

        val schedulerConf   = new Scheduler.SchedulerConfig()
        val schedulerConfig = config.getConfig("com.learningobjects.cpxp.scheduler")
        schedulerConf.poolSize = schedulerConfig.getInt("poolSize")
        schedulerConf.when = toMap(schedulerConfig.getConfig("when"))
        Scheduler.configure(schedulerConf)

        val fileCacheConfig = config.getConfig("com.learningobjects.cpxp.cache.file")
        val baseDir         = new File(fileCacheConfig.getString("basedir"))
        val attachmentSize  = fileCacheConfig.getLong("attachmentSize")
        FileCache.getInstance().configure(baseDir, attachmentSize)
        HttpUtils.configure(config)
        ThreadTerminator.configure(config.getLong("com.learningobjects.cpxp.util.threadTimeout"))
      }

    /** File cache cleanup is slow and just needs to happen before we start doing work.
      */
    override def cleanup(config: Config): F[Unit] = F.delay {
      val fileCacheConfig = config.getConfig("com.learningobjects.cpxp.cache.file")
      val persist         = fileCacheConfig.getBoolean("persist")
      if !persist then FileUtils.cleanDirectory(FileCache.getInstance().getCacheDir)
    }

    /** Start tomcat and mount any servlets/services we're going to serve.
      */
    override def tomcat(config: Config): F[DETomcatServer[F]] =
      Dispatcher.parallel[F] use { dispatcher =>
        F.blocking {
          val emptyDispatch = util.EnumSet.noneOf(classOf[DispatcherType])
          val debug         = config.getBoolean("com.learningobjects.cpxp.debug.web")
          val staticServlet = new StaticServlet("(scripts/|xsl/).*\\.(js|html|xsl)", "static", false)

          val builder = DETomcatBuilder
            .withConfig(DETomcatConfig.load(config))
            .mountFilter(new AwaitStartupFilter, "/*", Some("AwaitStartupFilter"), emptyDispatch)
            .mountFilter(new DebugAttributeFilter(debug), "/*", Some("DebugAttributeFilter"), emptyDispatch)
            .mountFilter(new ServiceContextFilter, "/*", Some("ServiceContextFilter"), emptyDispatch)
            .mountFilter(new AccessFilter, "/*", Some("AccessFilter"), emptyDispatch)
            .mountFilter(new HiddenHttpMethodFilter, "/*", Some("HiddenHttpMethodFilter"), emptyDispatch)
            .mountFilter(new LogFilter, "/*", Some("LogFilter"), emptyDispatch)
            .mountFilter(new RequestTimeoutFilter, "/*", Some("RequestTimeoutFilter"), emptyDispatch)
            .mountFilter(new MessageFilter, "/*", Some("MessageFilter"), emptyDispatch)
            .mountFilter(DifferenceEngine.di[SessionFilter], "/*", Some("SessionFilter"), emptyDispatch)
            .mountFilter(DifferenceEngine.di[SendFileFilter], "/*", Some("SendFileFilter"), emptyDispatch)
            .mountFilter(new CurrentFilter, "/*", Some("CurrentFilter"), emptyDispatch)
            .mountServlet(
              new JavascriptExceptionServlet(),
              "/control/javascriptException",
              Some("JavascriptExceptionServlet")
            )
            .mountServlet(new NameServlet(), "/*", Some("NameServlet"))
            .mountServlet(staticServlet, "/static/*", Some("StaticServlet"))
            .mountServlet(new EventServlet(), "/event/*", Some("EventServlet"))
            .mountListener(new PresenceHttpSessionListener)
            .mountCookieProcessor(new CpxpCookieProcessor)
          // .mountService(configService(config), "/sys/tconfig") //TODO: AuthMiddleWare for requiring admin/overlord/hosting.
          builder.start(dispatcher)
        }.flatten
      }

    override def meta(
      config: Config,
      emf: EntityManagerFactory /* implicitly used by BSM.configure */
    ): F[ServiceMeta] = withEntityManager[F].use { _ =>
      val meta = BaseServiceMeta.getServiceMeta
      BaseServiceMeta.configure(config)
      F.point(meta)
    }

    override def di(
      config: Config,
      dataSource: DataSource,
      ontology: Ontology,
    ): F[Unit] =
      for
        mail <- Mail.fromConfig[F](config).newSession
        _    <- F.delay {
                  val resourceBundleTranslator = new ResourceBundleTranslator(CoreBundle.SRS_MESSAGES.bundle)
                  InferEvaluator.registerInferrer(classOf[Config], (_, _) => config)
                  InferEvaluator.registerInferrer(classOf[DataSource], (_, _) => dataSource)
                  InferEvaluator.registerInferrer(
                    TypeUtils.parameterize(classOf[Translatable[?]], classOf[BundleMessage]),
                    (_, _) => resourceBundleTranslator
                  )
                  InferEvaluator.registerInferrer(
                    TypeUtils.parameterize(classOf[Translatable[?]], classOf[Throwable]),
                    (_, _) => new ThrowableTranslator
                  )
                  InferEvaluator.registerInferrer(classOf[Session], (_, _) => mail)
                  // the Task in classOf[Transactor[Task]] is just to make the type checker happy
                  InferEvaluator.registerInferrer(
                    TypeUtils.parameterize(classOf[Transactor[IO]], classOf[IO[?]]),
                    (_, _) => Transactor.fromDataSource[IO](dataSource, ExecutionContext.global)
                  )
                  InferEvaluator.registerInferrer(classOf[Ontology], (_, _) => ontology)
                }
      yield ()

    private implicit val zMonad: scalaz.Monad[F] = new scalaz.Monad[F]:
      override def point[A](a: => A): F[A] = F.point(a)

      override def bind[A, B](fa: F[A])(f: A => F[B]): F[B] = F.flatMap(fa)(f)

    override def threadPools(c: Config): effect.Resource[F, ThreadPools] =
      // TODO: Configurable
      for
        connectPool  <- ExecutionContexts.fixedThreadPool[F](Runtime.getRuntime.availableProcessors())
        transactPool <- ExecutionContexts.cachedThreadPool[F]
      yield ThreadPools(connectPool, transactPool)
  end Default

  /** DE, but benchmarks the time taken for each task.
    */
  final class Benched[F[_]: Async] extends Startup[Bench[F, *]]:
    val fToBench                          = new (F ~> Bench[F, *]):
      def apply[A](fA: F[A]): Bench[F, A] = Benchmark.liftF(fA)
    val default                           = new Default[F]
    override def config: Bench[F, Config] = Benchmark(default.config, "Load Config")

    override def reflect: Bench[F, ScanResult] = Benchmark(default.reflect, "Reflect")

    override def prelude(conf: Config): Bench[F, Unit] = Benchmark(default.prelude(conf), "Prelude")

    override def cleanup(conf: Config): Bench[F, Unit] = Benchmark(default.cleanup(conf), "Cleanup")

    override def tomcat(config: Config): Bench[F, DETomcatServer[Bench[F, *]]] =
      Benchmark(default.tomcat(config).map(_.mapK(fToBench)), "Start Tomcat")

    override def meta(config: Config, emf: EntityManagerFactory): Bench[F, ServiceMeta] =
      Benchmark(default.meta(config, emf), "Service Meta")

    override def di(
      config: Config,
      dataSource: DataSource,
      ontology: Ontology,
    ): Bench[F, Unit] =
      Benchmark(default.di(config, dataSource, ontology), "Setup DI")

    override def threadPools(c: Config): effect.Resource[Bench[F, *], ThreadPools] =
      default.threadPools(c).mapK(fToBench)
  end Benched
end Startup

/** Awaits startup before let requests through. Allows us to mount tomcat early.
  */
final class AwaitStartupFilter extends HttpFilter:
  private var started = false

  override def doFilter(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain): Unit =
    if !started then
      ComponentManager.awaitStartup()
      started = true
    chain.doFilter(request, response)

/** Sets the an attribute on the request for debugging.
  * @param debug
  */
final class DebugAttributeFilter(debug: Boolean) extends HttpFilter:
  override def doFilter(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain): Unit =
    request.setAttribute("debug", debug)
    chain.doFilter(request, response)

case class ThreadPools(connectionPool: ExecutionContext, transactionPool: ExecutionContext)

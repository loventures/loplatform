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

package de.tomcat

import cats.MonadError
import cats.effect.std.Dispatcher
import cats.effect.{Async, Resource, Sync}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.{HttpFilter, HttpServlet}
import org.apache.catalina.*
import org.apache.catalina.connector.{Connector, Request, Response}
import org.apache.catalina.core.{StandardHost, StandardServer}
import org.apache.catalina.startup.{ContextConfig, Tomcat}
import org.apache.catalina.valves.{ErrorReportValve, RemoteIpValve}
import org.apache.catalina.webresources.StandardRoot
import org.apache.coyote.ActionCode
import org.apache.coyote.http11.Http11Nio2Protocol
import org.apache.tomcat.util.descriptor.web.{FilterDef, FilterMap}
import org.apache.tomcat.util.http.CookieProcessor
import org.apache.tomcat.util.net.{SSLHostConfig, SSLHostConfigCertificate}
import org.apache.tomcat.util.scan.StandardJarScanner
import org.http4s.HttpRoutes
import org.http4s.servlet.AsyncHttp4sServlet
import sun.misc.Signal

import java.nio.charset.StandardCharsets
import java.util.EventListener
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.{Formatter, Level, Logger}
import scala.concurrent.ExecutionContext
import scala.util.Try
import de.tomcat.config.DETomcatConfig
import de.tomcat.internal.ShutdownHook
import de.tomcat.internal.TapDance.*
import de.tomcat.juli.*
import org.http4s.server.{DefaultServiceErrorHandler, Server, ServerBuilder, ServiceErrorHandler, defaults}
import org.http4s.servlet.{ServletContainer, ServletIo}

import java.net.InetSocketAddress
import java.util
import java.util.concurrent.Executor
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

/** Next time you're lost here, look at http4s-tomcat for any update: .
  * https://github.com/http4s/http4s-tomcat/blob/main/tomcat-server/src/main/scala/org/http4s/tomcat/server/TomcatBuilder.scala
  */
sealed class DETomcatBuilder[F[_]] private (
  conf: DETomcatConfig,
  socketAddress: InetSocketAddress,
  externalExecutor: Option[Executor],
  private val idleTimeout: Duration,
  private val asyncTimeout: Duration,
  private val servletIo: ServletIo[F],
  mounts: Vector[Mount[F]],
  private val serviceErrorHandler: ServiceErrorHandler[F],
  classloader: Option[ClassLoader],
)(implicit protected val F: Async[F])
    extends ServletContainer[F]
    with ServerBuilder[F]:
  type Self = DETomcatBuilder[F]

  private def copy(
    socketAddress: InetSocketAddress = socketAddress,
    servletIo: ServletIo[F] = servletIo,
    mounts: Vector[Mount[F]] = mounts,
    serviceErrorHandler: ServiceErrorHandler[F] = serviceErrorHandler
  ): Self =
    new DETomcatBuilder[F](
      conf,
      socketAddress,
      externalExecutor,
      idleTimeout,
      asyncTimeout,
      servletIo,
      mounts,
      serviceErrorHandler,
      classloader
    )

  override def bindSocketAddress(socketAddress: InetSocketAddress): Self =
    copy(socketAddress = socketAddress)

  override def withServletIo(servletIo: ServletIo[F]): Self =
    copy(servletIo = servletIo)

  override def withServiceErrorHandler(serviceErrorHandler: ServiceErrorHandler[F]): Self =
    copy(serviceErrorHandler = serviceErrorHandler)

  override def withBanner(banner: Seq[String]): Self = this

  override def mountServlet(
    servlet: HttpServlet,
    urlMapping: String,
    name: Option[String] = None,
  ): Self =
    copy(mounts = mounts :+ Mount[F] { (ctx, index, _, _) =>
      val servletName = name.getOrElse(s"servlet-$index")
      val wrapper     = Tomcat.addServlet(ctx, servletName, servlet)
      wrapper.addMapping(urlMapping)
      wrapper.setAsyncSupported(true)
    })

  override def mountFilter(
    filter: HttpFilter,
    urlMapping: String,
    name: Option[String],
    dispatches: util.EnumSet[DispatcherType],
  ): Self =
    copy(mounts = mounts :+ Mount[F] { (ctx, index, _, _) =>
      val filterName = name.getOrElse(s"filter-$index")

      val filterDef = new FilterDef
      filterDef.setFilterName(filterName)
      filterDef.setFilter(filter)

      filterDef.setAsyncSupported(true.toString)
      ctx.addFilterDef(filterDef)

      val filterMap = new FilterMap
      filterMap.setFilterName(filterName)
      filterMap.addURLPattern(urlMapping)
      dispatches.asScala.foreach { dispatcher =>
        filterMap.setDispatcher(dispatcher.name)
      }
      ctx.addFilterMap(filterMap)
    })

  def mountRoute(service: HttpRoutes[F], prefix: String): DETomcatBuilder[F] =
    copy(mounts = mounts :+ Mount[F] { (ctx, index, builder, dispatcher) =>
      val servlet = AsyncHttp4sServlet.builder(service.orNotFound, dispatcher).withAsyncTimeout(asyncTimeout).build
      val wrapper = Tomcat.addServlet(ctx, s"servlet-$index", servlet)
      wrapper.addMapping(prefix)
      wrapper.setAsyncSupported(true)
    })

  def mountListener(listener: => EventListener): DETomcatBuilder[F] =
    withContext(ctx =>
      ctx.addLifecycleListener((event: LifecycleEvent) =>
        event.getLifecycle.getState match
          case LifecycleState.STARTING_PREP => ctx.getServletContext.addListener(listener)
          case _                            => ()
      )
    )

  def mountCookieProcessor(processor: => CookieProcessor): DETomcatBuilder[F] =
    withContext(ctx => ctx.setCookieProcessor(processor))

  def withContext(f: Context => Unit): DETomcatBuilder[F] =
    copy(mounts = mounts :+ Mount[F] { (ctx, _, _, _) =>
      f(ctx)
    })

  // TODO: it does Resource blocking
  def resource: Resource[F, Server] =
    Dispatcher.parallel[F].flatMap(dispatcher => Resource.make(start(dispatcher))(_.shutdown).widen[Server])

  def start(dispatcher: Dispatcher[F]): F[DETomcatServer[F]] =
    for
      _      <- DETomcatBuilder.banner(conf)
      tomcat <- DETomcatBuilder.createTomcat(conf)
      _      <- Sync[F].delay:
                  val rootContext = tomcat.getHost.findChild("").asInstanceOf[Context]
                  for (mount, i) <- mounts.zipWithIndex do mount.f(rootContext, i, this, dispatcher)
      _      <- Sync[F].delay:
                  tomcat.start()
      _      <- DETomcatBuilder.ready(tomcat)
    yield new DETomcatServer(tomcat, socketAddress)
end DETomcatBuilder

object DETomcatBuilder:
  def withConfig[F[_]: Async](deConf: DETomcatConfig): DETomcatBuilder[F] =
    new DETomcatBuilder(
      conf = deConf,
      externalExecutor = Some(ExecutionContext.global),
      socketAddress = defaults.IPv4SocketAddress,
      asyncTimeout = defaults.ResponseTimeout,
      idleTimeout = defaults.IdleTimeout,
      servletIo = ServletContainer.DefaultServletIo,
      mounts = Vector.empty,
      serviceErrorHandler = DefaultServiceErrorHandler,
      classloader = None,
    )

  def loggingSetup[F[_]](conf: DETomcatConfig)(implicit F: Sync[F]): F[Unit] =
    for
      _         <- F.delay {
                     /* Override the log manager before any logging anything kicks in. */
                     System.setProperty("java.util.logging.manager", classOf[DeLogManager].getName)
                     Thread.setDefaultUncaughtExceptionHandler(new LoggingExceptionHandler(Console.logger))
                     conf.logDirectory.mkdirs()
                     Console.init(conf.color)(using ExecutionContext.global)
                   }
      logFormat <- parseLogFormat(conf.logger)
      _         <- F.delay {
                     val (formatter, suffix) = logFormat
                     val logHandler          =
                       new SignalReloadingFileHandler(conf.logDirectory, "detomcat", suffix, StandardCharsets.UTF_8, formatter)

                     Console.logger.addHandler(new AnsiStripHandler(logHandler))

                     val rootLogger = Logger.getLogger("")
                     rootLogger.getHandlers foreach {
                       _.setLevel(Level.OFF)
                     }
                     rootLogger.addHandler(logHandler)

                     Signal.handle(SignalReloadingFileHandler.HUP, logHandler)

                     Console.logger.info(s"Redirecting output to ${logHandler.logFile}")
                   }
    yield ()

  def parseLogFormat[F[_]](format: String)(implicit F: MonadError[F, Throwable]): F[(Formatter, String)] =
    format match
      case "json"       => F.pure(new JsonFormatter(true) -> ".json")
      case "jsonsingle" => F.pure(new JsonFormatter(false) -> ".json")
      case "old"        => F.pure(StandardFormatter -> ".log")
      case other        => F.raiseError(new Exception(s"Unknown logger: $other"))

  def banner[F[_]](conf: DETomcatConfig)(implicit F: Async[F]): F[Unit] = F.delay {
    Console.logger.info(s"Starting DETomcat(${de.tomcat.BuildInfo})")
    Console.logger.info(Banner.rainbow(conf.banner))
  }

  def createTomcat[F[_]](conf: DETomcatConfig)(implicit F: Async[F]): F[Tomcat] = F.delay {
    conf.baseDirectory.mkdirs()
    val tomcat = new Tomcat()
    tomcat `setBaseDir` conf.baseDirectory.getAbsolutePath

    val context = tomcat.addContext("", conf.baseDirectory.getAbsolutePath)
    context.setAllowCasualMultipartParsing(true)

    tomcat.getHost.asInstanceOf[StandardHost].setErrorReportValveClass(classOf[DEErrorReportValve].getName)

    context.getPipeline.addValve(remoteIpValve)
    context.addLifecycleListener(new ContextConfig)
    // Drop a shutdown hook in the servlet context attributes. This may not be necessary, it may be
    // possible to extricate the server from the runtime servlet context in application code, but....
    val shutterDowner: Runnable = () => tomcat.getServer.stop()
    context.getServletContext.setAttribute("shutdown", shutterDowner)
    val resources               = new StandardRoot(context)

    val connector = new Connector(classOf[Http11Nio2Protocol].getName)
    connector.setThrowOnFailure(true)
    connector.setPort(conf.port)
    connector.setMaxPostSize(MaxPostSize)
    val protocol  = connector.getProtocolHandler.asInstanceOf[Http11Nio2Protocol]
    protocol.setMinSpareThreads(conf.executor.minSpareThreads)
    protocol.setMaxThreads(conf.executor.maxThreads)
    protocol.setMaxHttpHeaderSize(conf.maxHttpHeaderSize)
    tomcat.getService.addConnector(connector)
    tomcat.setConnector(connector)

    // Disable Jar Scanning.
    val scanner = new StandardJarScanner()
    scanner.setScanClassPath(false)
    scanner.setScanBootstrapClassPath(false)
    scanner.setScanManifest(false)
    scanner.setScanAllDirectories(false)
    scanner.setScanAllFiles(false)
    context.setJarScanner(scanner)

    if conf.ssl then
      val sslConnector = new Connector(classOf[Http11Nio2Protocol].getName)
      sslConnector.setThrowOnFailure(true)
      sslConnector.setSecure(true)
      sslConnector.setPort(conf.sslPort)
      sslConnector.setMaxPostSize(MaxPostSize)
      val sslProtocol  = sslConnector.getProtocolHandler.asInstanceOf[Http11Nio2Protocol]
      sslProtocol.setMinSpareThreads(conf.executor.minSpareThreads)
      sslProtocol.setMaxThreads(conf.executor.maxThreads)
      sslProtocol.setMaxHttpHeaderSize(conf.maxHttpHeaderSize)
      sslConnector.setScheme("https")
      sslConnector.setProperty("allowUnsafeLegacyRenegotiation", "false")
      // sslConnector.setProperty("compression", "on")
      sslConnector.setProperty("bindOnInit", "true")
      sslConnector.setProperty("sslProtocol", "TLS")
      sslConnector.setProperty("SSLEnabled", "true")
      sslConnector.setUseBodyEncodingForURI(true)

      val sslConfig  = new SSLHostConfig
      val trustStore = conf.sslConfig.trustStore
      trustStore foreach { store =>
        sslConfig.setTruststoreFile(store.getAbsolutePath)
        sslConfig.setTruststorePassword(conf.sslConfig.trustStorePassword.orNull)
      }
      val keyStore   = conf.sslConfig.keyStore
      keyStore foreach { store =>
        val sslCert = new SSLHostConfigCertificate(sslConfig, SSLHostConfigCertificate.Type.RSA)
        sslCert.setCertificateKeystoreFile(store.getAbsolutePath)
        sslCert.setCertificateKeystorePassword(conf.sslConfig.keyStorePassword.orNull)
        sslConfig.addCertificate(sslCert)
      }
      sslConnector.addSslHostConfig(sslConfig)

      tomcat.getConnector.setRedirectPort(conf.sslPort)
      tomcat.getService.addConnector(sslConnector)
    end if

    val loader =
      new DEWebappLoader(Seq.empty, resources)
    // loader.setReloadable(false)
    loader.setDelegate(false)
    context.setLoader(loader)
    context.setParentClassLoader(getClass.getClassLoader)

    context.setResources(resources)

    val stopOnFailure: LifecycleListener = (event: LifecycleEvent) =>
      event.getLifecycle.getState match
        case LifecycleState.FAILED =>
          Console.logger.log(Level.SEVERE, s"Context[${context.getName}] failed in ${event.getLifecycle.getStateName}")
          tomcat.getServer.asInstanceOf[StandardServer].stopAwait()
        case _                     =>

    // Shutdown tomcat on startup error
    context.addLifecycleListener(stopOnFailure)
    tomcat.getServer.addLifecycleListener(stopOnFailure)
    Runtime.getRuntime.addShutdownHook(new ShutdownHook(tomcat, Console.logger))
    tomcat
  }

  def ready[F[_]](tomcat: Tomcat)(implicit F: Sync[F]): F[Unit] = F.delay {
    val port    = tomcat.getConnector.getPort
    val sslPort = tomcat.getConnector.getRedirectPort
    println(s"DETomcat(${de.tomcat.BuildInfo}) ready on ports $port/$sslPort")
  }

  private def remoteIpValve: RemoteIpValve =
    new org.apache.catalina.valves.RemoteIpValve <| { valve =>
      valve.setRequestAttributesEnabled(false)
      valve.setProtocolHeader("X-Forwarded-Proto")
    }

  private final val MaxPostSize = -1 // unlimited because it's an int and people upload multi-GB files
end DETomcatBuilder

private final case class Mount[F[_]](f: (Context, Int, DETomcatBuilder[F], Dispatcher[F]) => Unit)

final class DEErrorReportValve extends ErrorReportValve:
  override def report(request: Request, response: Response, throwable: Throwable): Unit =
    if reportError(response) && isIoAllowed(response) then
      Try {
        response.setContentType("text/html")
        response.setCharacterEncoding("utf-8")
        Option(response.getReporter) foreach { writer =>
          val sc = response.getStatus
          val sb = <html>
            <head>
              <title>
                {s"$sc Error"}
              </title>
              <style type="text/css">
                {
            """
                   | .title {
                   | font-family: sans-sel
                   | }
                   | .error {
                   |   font-family: sans-serif;
                   |   font-size: 32pt;
                   |   position: absolute;
                   |   top: 50vh;
                   |   left: 50vw;
                   |   transform: translate(-50%, -50%) rotate(15deg);
                   |   transform-origin: 50% 50%;
                   |   text-align: center;
                   | }
                   | .error .sc {
                   |   font-size: 160pt;
                   | }""".stripMargin
          }
              </style>
            </head>
            <body>
              <div class="error">
                <div>oh noes ☹</div> <div class="sc">
                {sc}
              </div>
              </div>
            </body>
          </html>
          writer.write(sb.toString)
          response.finishResponse()
        }
      }

  // cargo cult from superclass

  private def reportError(response: Response): Boolean =
    response.getStatus >= 400 && response.getContentWritten == 0 && response.setErrorReported()

  private def isIoAllowed(response: Response): Boolean =
    val io = new AtomicBoolean
    response.getCoyoteResponse.action(ActionCode.IS_IO_ALLOWED, io)
    io.get
end DEErrorReportValve

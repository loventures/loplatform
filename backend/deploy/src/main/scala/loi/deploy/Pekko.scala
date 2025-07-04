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

import org.apache.pekko.actor.{ActorSystem, Address, Terminated}
import org.apache.pekko.management.cluster.bootstrap.ClusterBootstrap
import org.apache.pekko.management.scaladsl.PekkoManagement
import cats.effect.IO
import com.learningobjects.cpxp.BaseServiceMeta
import com.learningobjects.cpxp.scala.actor.CpxpActorSystem
import com.learningobjects.cpxp.service.upgrade.SystemInfo
import com.typesafe.config.{Config, ConfigFactory}
import loi.deploy.Benchmark.Bench
import loi.network.ClusterStatus
import org.log4s.*

import scala.jdk.CollectionConverters.*

/** Start up and shutdown an pekko based actor system.
  */
trait Pekko[F[_]]:
  def start(config: Config, clusterStatus: ClusterStatus[F, String]): F[ActorSystem]
  def shutdown(as: ActorSystem): F[Terminated]
object Pekko:
  private val logger = getLogger
  final class Default extends Pekko[IO]:

    final val CLUSTER_SINGLETON = "com.learningobjects.cpxp.network.cluster.singleton"
    final val PORT              = "pekko.remote.artery.canonical.port"
    final val MGMT_HTTP_PORT    = "pekko.management.http.port"
    final val DISCOVERY_METHOD  = "pekko.discovery.method"

    override def start(config: Config, cluster: ClusterStatus[IO, String]): IO[ActorSystem] =
      for
        das            <- cluster.centralHost
        myself         <- cluster.localhost
        port            = config.getInt(PORT)
        mgmtPort        = config.getInt(MGMT_HTTP_PORT)
        discoveryMethod = config.getString(DISCOVERY_METHOD)                                       // shows up as <method>
        singleton       = config.getBoolean(CLUSTER_SINGLETON)
        others         <- cluster.recentHosts.map(_.filterNot(_ == myself))
        clusterNodes    = Set(s"""{ host = $das
                port = $mgmtPort
              }""") ++ others.map(s"""{ host = """ + _ + s"""
                port = $mgmtPort
          }""")
        nodes           = clusterNodes.mkString("[", ",", "]")
        min             = if clusterNodes.size > 2 && !singleton then clusterNodes.size - 1 else 1 // rly -1?
        clusterConfig   = ConfigFactory.parseString(                                               // hostname in application.conf can't handle context.xml merge
                            s"""pekko {
                               |  actor {
                               |    provider = "cluster"
                               |    allow-java-serialization = true
                               |    serializers {
                               |      jackson = "com.learningobjects.cpxp.pekko.JacksonSerializer"
                               |    }
                               |    serialization-bindings {
                               |      "com.fasterxml.jackson.databind.JsonNode" = jackson
                               |      "com.learningobjects.cpxp.async.Event" = jackson
                               |    }
                               |  }
                               |  discovery {
                               |    method = config
                               |    config.services {
                               |        detomcat = {
                               |          endpoints = $nodes
                               |        }
                               |    }
                               |  }
                               |  management {
                               |    http {
                               |      hostname = $myself
                               |      port = $mgmtPort
                               |    }
                               |    cluster.bootstrap {
                               |      contact-point-discovery {
                               |        service-name = "detomcat"
                               |        required-contact-point-nr = $min
                               |      }
                               |    }
                               |  }
                               |  remote {
                               |    artery {
                               |      enabled = on
                               |      transport = tcp
                               |      canonical.hostname = $myself
                               |      canonical.port = $port
                               |    }
                               |  }
                               |}""".stripMargin
                          )
        _              <- IO {
                            logger.info("Pekko Config")
                            logger.info(clusterConfig.root().render())
                          }
        actorSystem    <- IO(ActorSystem("cpxp", clusterConfig.withFallback(config).resolve()))
        _              <- IO(CpxpActorSystem.setActorSystem(actorSystem))
        _              <- IO(PekkoManagement(actorSystem).start())
        _              <- IO(ClusterBootstrap(actorSystem).start())
      yield actorSystem

    override def shutdown(as: ActorSystem): IO[Terminated] =
      for
        terminated <- IO.fromFuture(IO(as.terminate()))
        _          <- IO(CpxpActorSystem.clearActorSystem())
      yield terminated

    def addressFromHostName(name: String, port: Int): Address =
      Address("pekko", "cpxp", name, port)

    def otherHosts(singleton: Boolean, localhost: String, port: Int): IO[Seq[Address]] =
      if singleton then IO.pure(Seq.empty)
      else recentHosts(localhost).map(_.map(sysInfo2Address(_, port)))

    def recentHosts(localhost: String): IO[Seq[SystemInfo]] =
      IO(BaseServiceMeta.findRecentHosts().asScala.toSeq)
        .handleErrorWith(_ => IO.pure(Seq.empty))
        .map(_.filter(_.getCentralHost != localhost))

    def sysInfo2Address(system: SystemInfo, port: Int): Address =
      addressFromHostName(system.getCentralHost, port)
  end Default

  final class Benched[F[_]] extends Pekko[Bench[IO, *]]:
    val default                                                                                                    = new Default()
    override def start(config: Config, clusterStatus: ClusterStatus[Bench[IO, *], String]): Bench[IO, ActorSystem] =
      Benchmark(default.start(config, toIO(clusterStatus)), "Start Pekko")

    override def shutdown(as: ActorSystem): Bench[IO, Terminated] =
      Benchmark(default.shutdown(as), "Shutdown Pekko")

    def toIO(clusterStatus: ClusterStatus[Bench[IO, *], String]): ClusterStatus[IO, String] =
      new ClusterStatus[IO, String]:
        override def centralHost: IO[String]         = clusterStatus.centralHost.run.map(_._2)
        override def acquireCentralHost: IO[Boolean] = clusterStatus.acquireCentralHost.run.map(_._2)
        override def releaseCentralHost: IO[Unit]    = clusterStatus.releaseCentralHost.run.map(_._2)
        override def recentHosts: IO[List[String]]   = clusterStatus.recentHosts.run.map(_._2)
        override def heartbeat: IO[Unit]             = clusterStatus.heartbeat.run.map(_._2)
        override def localhost: IO[String]           = clusterStatus.localhost.run.map(_._2)
  end Benched
end Pekko

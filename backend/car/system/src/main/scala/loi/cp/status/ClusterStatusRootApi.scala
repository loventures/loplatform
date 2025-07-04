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

package loi.cp.status

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.cluster.{Cluster, Member}
import org.apache.pekko.util.Timeout
import com.learningobjects.cpxp.ServiceMeta
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.*
import com.learningobjects.cpxp.component.{ComponentImplementation, ComponentInstance}
import com.learningobjects.cpxp.scala.actor.ActorRefOps.*
import com.learningobjects.cpxp.service.upgrade.{SystemInfo, UpgradeService}
import com.learningobjects.cpxp.status.{PubSubStatusActor, SingletonStatusActor}
import com.learningobjects.de.authorization.Secured
import scaloi.syntax.any.*
import scaloi.syntax.date.*
import scaloi.syntax.seq.*

import java.util.Date
import java.util.concurrent.Executors
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

/** Cluster status root API. This provides some web bindings for establishing the health of the various clustering
  * components of the system.
  */
@Component
@Controller(value = "clusterStatus", root = true)
@RequestMapping(path = "clusterStatus")
@Secured(allowAnonymous = true) // TODO: some level of cluster security token...
class ClusterStatusRootApi(
  val componentInstance: ComponentInstance,
  upgradeService: UpgradeService,
  csc: ClusterStatusClient
)(implicit
  actorSystem: ActorSystem,
  sm: ServiceMeta,
  csCache: ClusterStatusCache
) extends ApiRootComponent
    with ComponentImplementation:
  import ClusterStatusRootApi.*

  /** Identify the members of the cluster as reported in the core database. */
  @RequestMapping(path = "members", method = Method.GET)
  def members: CpxpMembers =
    CpxpMembers(sm.getLocalHost, upgradeService.findRecentHosts.asScala.toSeq.map(CpxpMember.apply))

  /** Establish the health of the pekko/jgroups cluster on this node. This API is unauthenticated because it is called
    * internally between nodes of the cluster.
    */
  @RequestMapping(path = "status", method = Method.GET)
  def status: Future[ClusterStatus] = withLocalExecutor { implicit ec =>
    val clusterState    = Cluster(actorSystem).state
    val leader          = clusterState.leader.flatMap(_.host)
    val members         = clusterState.members.toSeq.map(PekkoMember.apply)
    // start both requests so they run in parallel
    val singletonFuture = singletonStatus
    val pubSubFuture    = pubSubStatus
    val jGroupsFuture   = jGroupsStatus
    for
      singleton <- singletonFuture
      pubSub    <- pubSubFuture
      jGroups   <- jGroupsFuture
    yield ClusterStatus(
      leader,
      members,
      singleton.map(_.node),
      pubSub.fold(List.empty[String])(_.nodes.map(_.node)),
      jGroups
    )
    end for
  }

  /** Establish the status of all members of the cluster. */
  @RequestMapping(path = "clusterStatus", method = Method.GET)
  def clusterStatus: Future[Seq[CpxpMemberStatus]] = withLocalExecutor { implicit ec =>
    Future.sequence(members.members map getMemberStatus).map(_.flatten)
  }

  /** Heuristic local opinion on cluster health. */
  @RequestMapping(path = "healthy", method = Method.GET)
  def healthy: Future[Boolean] = withLocalExecutor { implicit ec =>
    val clusterState = Cluster(actorSystem).state
    val leader       = clusterState.leader.flatMap(_.host)
    val members      = clusterState.members.toSeq.flatMap(_.address.host)
    val recent       = upgradeService.findRecentHosts.asScala
    for pubSub <- pubSubStatus
    yield
      val pekkosOk  = members.hasSize(recent.size)
      val pubSubsOk = pubSub.exists(_.nodes.hasSize(recent.size))
      val leadersOk = leader.exists(l => pubSub.exists(_.nodes.forall(_.leader.contains(l))))
      logger info s"Health check: ${recent.size} recent, ${members.size} pekko, ${pubSub.fold(0)(_.nodes.size)} pub sub, leader $leadersOk"
      pekkosOk && pubSubsOk && leadersOk
  }

  /** Get the status of a particular cluster member by calling them via HTTP. */
  private def getMemberStatus(member: CpxpMember)(implicit ec: ExecutionContext): Future[Option[CpxpMemberStatus]] =
    if member.host == sm.getLocalHost then // often we can't talk to localhost over http... but why?
      status.map(CpxpMemberStatus(member, _)) `orNoneAfter` WaitTime * 2
    else csc.getClusterStatus(member.host).map(CpxpMemberStatus(member, _)) `orNoneAfter` WaitTime * 2

  /** Use a local executor to avoid contention of the global execution context. */
  private def withLocalExecutor[A](f: ExecutionContext => Future[A]): Future[A] =
    val es = Executors.newCachedThreadPool()
    val ec = ExecutionContext.fromExecutorService(es)
    f(ec) <| {
      _.onComplete(_ => es.shutdownNow())(using ec) // shutting down the ec from within the ec #inception
    }
end ClusterStatusRootApi

/** Cluster status root companion.
  */
object ClusterStatusRootApi:

  /** The logger. */
  private val logger = org.log4s.getLogger

  /** The base wait time for accumulating information. */
  private val WaitTime = 10.seconds

  /** The maximum overall timeout for future operations. */
  implicit val OverallTimeout: Timeout = Timeout(30.seconds)

  /** Ask a cluster singleton for status information, waiting a maximum of a few seconds before failing out. This should
    * let you know who, if any, was elected singleton.
    */
  private def singletonStatus(implicit ec: ExecutionContext): Future[Option[SingletonStatusActor.StatusResponse]] =
    SingletonStatusActor.clusterActor
      .fold(Future.successful(Option.empty[SingletonStatusActor.StatusResponse])) { actor =>
        actor
          .askFor[SingletonStatusActor.StatusResponse](SingletonStatusActor.StatusRequest)
          .orNoneAfter(WaitTime)
      }

  /** Broadcast a status request to the distributed pub sub bus, wait a few seconds for responses and then gather the
    * results. This should let you know who is listening on the bus and can respond.
    */
  private def pubSubStatus(implicit
    system: ActorSystem,
    ec: ExecutionContext
  ): Future[Option[PubSubStatusActor.StatusResponse]] =
    PubSubStatusActor.localActor.fold(Future.successful(Option.empty[PubSubStatusActor.StatusResponse])) { actor =>
      actor
        .askFor[PubSubStatusActor.StatusResponse](PubSubStatusActor.StatusRequest(recently))
        .orNoneAfter(WaitTime)
    }

  /** Return the nodes that have recently reported into jgroups. */
  private def jGroupsStatus(implicit
    sm: ServiceMeta,
    ec: ExecutionContext,
    csc: ClusterStatusCache
  ): Future[List[String]] =
    Future.successful(csc.hosts(recently))

  private def recently = new Date - 2.minutes

  /** Pimped futures. */
  implicit class FutureOps[T](self: Future[T]):

    /** Wait a certain amount of time for a future to complete, returning none on error.
      *
      * @param timeout
      *   the length of time to wait
      * @return
      *   a new future
      */
    def orNoneAfter(timeout: Duration)(implicit ec: ExecutionContext): Future[Option[T]] =
      Future { Await.result(self, timeout) } map { Option.apply } recover { case NonFatal(e) =>
        logger.warn(e)("Failure waiting for a future to complete")
        None
      }
  end FutureOps
end ClusterStatusRootApi

/** Cluster member status information. */
case class CpxpMemberStatus(member: CpxpMember, clusterStatus: ClusterStatus)

/** Cluster status information.
  * @param leader
  *   the leader
  * @param members
  *   the cluster members
  * @param singleton
  *   the singleton node, if found
  * @param pubSub
  *   the distributed pub-sub respondents, if any
  * @param jGroups
  *   the JGroups respondents, if any
  */
case class ClusterStatus(
  leader: Option[String],
  members: Seq[PekkoMember],
  singleton: Option[String],
  pubSub: Seq[String],
  jGroups: Seq[String]
)

/** Information about a cluster member from the pekko cluster. */
case class PekkoMember(host: Option[String], status: String)

object PekkoMember:

  /** Extract member information from an pekko {Member} datastructure. */
  def apply(member: Member): PekkoMember =
    PekkoMember(member.address.host, member.status.toString)

/** Cluster members and the current host. */
case class CpxpMembers(current: String, members: Seq[CpxpMember])

/** Information about a cluster member from the core database. */
case class CpxpMember(host: String, name: String, time: Date)

object CpxpMember:

  /** Extract member information from a {SystemInfo} entity. */
  def apply(si: SystemInfo): CpxpMember = CpxpMember(si.getCentralHost, si.getNodeName, si.getCentralHostTime)

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

package loi.network

import cats.Monad
import cats.effect.MonadCancel
import com.learningobjects.cpxp.BaseServiceMeta
import com.typesafe.config.{Config, ConfigFactory}
import doobie.*
import doobie.implicits.*
import doobie.implicits.legacy.instant.*

import java.time.{Duration, Instant}

/** Manage the status of members in the cluster by poking at the database.
  */
case class DBAppClusterStatus(config: Config) extends ClusterStatus[ConnectionIO, String]:

  /** Attempt to become the central host. Return true if this attempt was sucessful.
    */
  override def acquireCentralHost: ConnectionIO[Boolean] =
    for
      _       <- init
      myself  <- localhost
      now     <- HC.delay(Instant.now())
      offset   = now.minus(centralHostOffset)
      changed <- updateCentralHost(myself, now, offset).run
    yield changed > 0

  /** Release central hosts
    */
  override def releaseCentralHost: ConnectionIO[Unit] =
    for
      _      <- init
      myself <- localhost
      now    <- HC.delay(Instant.now())
      _      <- removeCentralHost(myself, now).run
    yield ()

  /** Return a list of recently seen hosts.
    */
  override def recentHosts: ConnectionIO[List[String]] =
    for
      _      <- init
      myself <- localhost
      exists <- schemaExists.unique
      now    <- HC.delay(Instant.now())
      offset  = now.minus(centralHostOffset)
      hosts  <- if exists then findRecentHost(offset).to[List] else HC.delay(List.empty[String])
    yield hosts

  /** Send a heartbeat to the cluster. Acknowledging you're still alive.
    */
  override def heartbeat: ConnectionIO[Unit] =
    for
      _           <- init
      myself      <- localhost
      now         <- HC.delay(Instant.now())
      myClusterId <- clusterId
      rows        <- updateRecentHosts(myself, now, myClusterId).run
      _           <- if rows == 0 then newPK.unique.flatMap(id => insertNewHost(myself, now, id, myClusterId).run)
                     else Monad[ConnectionIO].point(0)
    yield ()

  override def centralHost: ConnectionIO[String] =
    if isSingleton then localhost
    else
      for
        exists <- schemaExists.unique
        name   <- if exists then centralHostName().unique else localhost
      yield name

  final val Singleton                          = "com.learningobjects.cpxp.network.cluster.singleton"
  final val HostName                           = "com.learningobjects.cpxp.network.localhost"
  final val PekkoTagFilters                    = "pekko.discovery.aws-api-ec2-tag-based.filters"
  override def localhost: ConnectionIO[String] = HC.delay(BaseServiceMeta.validateLocalhost(config.getString(HostName)))

  def isSingleton: Boolean = config.getBoolean(Singleton)

  def clusterId: ConnectionIO[String] = HC.delay(ConfigFactory.load().getString(PekkoTagFilters))

  /** Subtracts the offset
    */
  val centralHostOffset = Duration.ofMinutes(5)

  def schemaExists: Query0[Boolean] =
    sql"""
      SELECT count(*) <> 0
      FROM pg_class
      WHERE relname = 'systeminfo'""".query[Boolean]

  def updateCentralHost(myself: String, now: Instant, offset: Instant, singletonId: Long = 0L): Update0 =
    sql"""
      UPDATE systeminfo
      SET centralHost = $myself, centralHostTime = $now
      WHERE id = $singletonId
        AND (centralHost IS NULL OR centralHost = $myself OR centralHostTime < $offset)""".update

  def findRecentHost(offset: Instant, singletonId: Long = 0L): Query0[String] =
    sql"""
      SELECT centralhost
      FROM systeminfo sys
      WHERE sys.id != $singletonId AND sys.centralHostTime > $offset
      """.query[String]

  def updateRecentHosts(myself: String, now: Instant, clusterId: String, singletonId: Long = 0L): Update0 =
    sql"""
        UPDATE systeminfo
        SET centralHost = $myself, centralHostTime = $now, clusterid = $clusterId
        WHERE id != $singletonId
        AND centralHost = $myself
         """.update

  def insertNewHost(myself: String, now: Instant, id: Long, clusterId: String): Update0 =
    sql"""
        INSERT INTO systeminfo (id, centralhost, centralhosttime, clusterid)
        VALUES ($id, $myself, $now, $clusterId)
       """.update

  def newPK: Query0[Int] =
    sql"""SELECT max(id) from systeminfo""".query[Int]

  def removeCentralHost(myself: String, now: Instant): Update0 =
    // If I set central host to null then another host will come along
    // immediately and become DAS. This means that during a system upgrade
    // the DAS will rotate through a bunch of servers, and a *booting*
    // server will *never* acquire DAS, so *bootstrap* things will
    // not run. So I leave myself as DAS and update the time so that I have
    // five minutes to restart and take back DAS.
    sql"""
      UPDATE systeminfo
      SET centralHost = $myself, centralHostTime = $now
      WHERE centralHost = $myself
      """.update

  def centralHostName(singletonId: Long = 0): Query0[String] =
    sql"""SELECT centralhost FROM systeminfo WHERE id = $singletonId""".query[String]

  // Note to future someone, this just just replicates the work hibernate does with the
  // SystemInfo entity, I don't know why.
  def createSystemInfoTable: Update0 =
    sql"""
      CREATE TABLE IF NOT EXISTS systeminfo (
        id bigint NOT NULL,
        centralhost character varying(255),
        centralhosttime timestamp without time zone,
        lastnotificationtime timestamp without time zone,
        version character varying(255),
        cdnversion integer DEFAULT 0 NOT NULL,
        clusterid character varying(255)
      )
      """.update

  def addPrimaryKey =
    sql"""
      ALTER TABLE ONLY systeminfo ADD CONSTRAINT systeminfo_pkey PRIMARY KEY (id);
      """.update

  def init: ConnectionIO[Unit] = for
    newTable <- createSystemInfoTable.run
    _        <- if newTable > 0 then addPrimaryKey.run else Monad[ConnectionIO].point(0)
  yield ()
end DBAppClusterStatus

/** Perform each effect in it's own transaction with the given transactor.
  */
case class Transactional[M[_], A](xa: Transactor[M], status: ClusterStatus[ConnectionIO, A])(implicit
  x: MonadCancel[M, Throwable]
) extends ClusterStatus[M, A]:

  /** Attempt to become the central host. Return true if this attempt was sucessful.
    */
  override def acquireCentralHost: M[Boolean] = status.acquireCentralHost.transact(xa)

  /** Release central hosts
    */
  override def releaseCentralHost: M[Unit] = status.releaseCentralHost.transact(xa)

  /** Return a list of recently seen hosts.
    */
  override def recentHosts: M[List[A]] = status.recentHosts.transact(xa)

  /** Send a heartbeat to the cluster. Acknowledging you're still alive.
    */
  override def heartbeat: M[Unit] = status.heartbeat.transact(xa)

  override def localhost: M[A] = status.localhost.transact(xa)

  override def centralHost: M[A] = status.centralHost.transact(xa)
end Transactional

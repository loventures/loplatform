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

package loi.db

import cats.effect.IO
import cats.syntax.flatMap.*
import com.typesafe.config.ConfigFactory
import doobie.*
import doobie.implicits.*
import loi.doobie.io.{setSearchPath, setTimeZone}

import java.time.ZoneId
import scala.concurrent.ExecutionContext

object Redshift:
  private def config = ConfigFactory.load().getConfig("de.databases.redshift")

  def isConfigured = config.hasPath("datasource.url")

  // TODO get Transactor from a connection pool
  def buildTransactor(useBusUser: Boolean)(implicit logHandler: LogHandler[IO]): Transactor[IO] =

    val resolvedConfig =
      if useBusUser && config.hasPath("busUser.user") then
        // the bus user has a CONNECTION LIMIT for safety in case the
        // pekko brain splits and there are two DAS
        config
          .withValue("datasource.user", config.getValue("busUser.user"))
          .withValue("datasource.pass", config.getValue("busUser.pass"))
      else config

    val dataSource = ConfigPersistenceUnitInfo.dataSource(resolvedConfig)
    Transactor.fromDataSource[IO](
      dataSource,
      ExecutionContext.global,
      logHandler = Some(logHandler),
    )
  end buildTransactor

  // but its actually gonna run the set statements for every transaction /shrug
  def configureSession(schemaName: String, zoneId: ZoneId = ZoneId.systemDefault())(
    xa: Transactor[IO]
  ): Transactor[IO] =
    Transactor.before.modify(
      xa,
      io => setSearchPath(schemaName) >> setTimeZone(zoneId) >> io
    )

  def buildTransactor(
    schemaName: String,
    zoneId: ZoneId = ZoneId.systemDefault(),
    useBusUser: Boolean = false
  )(implicit logHandler: LogHandler[IO]): Transactor[IO] =
    configureSession(schemaName, zoneId)(buildTransactor(useBusUser))
end Redshift

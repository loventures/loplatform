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

package com.learningobjects.cpxp.postgresql

import cats.effect.Async
import doobie.*

sealed trait DB:
  def transactor[F[_]: Async]: Transactor[F]

final case class DBUrl(
  url: String,
  user: String,
  pass: String,
  driver: String,
) extends DB:
  def transactor[F[_]: Async]: Transactor[F] =
    Transactor.fromDriverManager(
      driver = driver,
      url = url,
      user = user,
      password = pass,
      logHandler = None,
    )
end DBUrl

final class DBConnection(cxn0: => java.sql.Connection) extends DB:
  lazy val cxn = cxn0

  // XXX: Using the global execution context is terrible,
  // but thats what was probably used before
  lazy val ec = scala.concurrent.ExecutionContext.global

  // TODO check if this is ok???
  def transactor[F[_]: Async]: Transactor[F] =
    Transactor.fromConnection(cxn, logHandler = None)
end DBConnection

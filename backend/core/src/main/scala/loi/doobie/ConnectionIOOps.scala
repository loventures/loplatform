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

package loi.doobie

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie.implicits.*
import doobie.{ConnectionIO, Transactor}

import scala.language.implicitConversions
import scala.util.Try

class ConnectionIOOps[A](private val self: ConnectionIO[A]) extends AnyVal:

  def ffs(implicit xa: Transactor[IO]): A =
    Try(self.transact(xa).unsafeRunSync()).recover(t => throw new RuntimeException(t)).get

trait ConnectionIOSyntax:
  implicit def connectionIOSyntax[A](cio: ConnectionIO[A]): ConnectionIOOps[A] = new ConnectionIOOps(cio)

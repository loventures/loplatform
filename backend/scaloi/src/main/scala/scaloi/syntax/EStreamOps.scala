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

package scaloi.syntax

import scalaz.EphemeralStream

final class EStreamOps[A](private val self: EphemeralStream[A]) extends AnyVal:

  /** Is this ephemeral stream non-empty.
    */
  def nonEmpty: Boolean = !self.isEmpty

  def foreach[U](f: A => U): Unit =
    self.foldLeft(())((_, b) => f(b))

trait ToEStreamOps:
  import language.implicitConversions

  @inline implicit final def ToEStreamOps[A](self: EphemeralStream[A]): EStreamOps[A] =
    new EStreamOps[A](self)

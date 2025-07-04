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

package clots.syntax

import scala.language.implicitConversions

trait ClotsAnyFSyntax:

  implicit def toClotsAnyFOps[F[_], A](fa: F[A]): ClotsAnyFOps[F, A] = new ClotsAnyFOps(fa)

final class ClotsAnyFOps[F[_], A](private val fa: F[A]) extends AnyVal:

  /** Provides a withFilter so that you can desugar for-comprehension generators. Do not use if you expect withFilter to
    * work, i.e. if you use `if` guards.
    */
  def fauxnad: ClotsFauxnadOps[F, A] = new ClotsFauxnadOps(fa)

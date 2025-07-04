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

import cats.data.NonEmptyList

import scala.language.implicitConversions

trait ClotsNonEmptyListSyntax:

  implicit final def clotsSyntaxNonEmptyList[A](nela: NonEmptyList[A]): ClotsNonEmptyListOps[A] =
    new ClotsNonEmptyListOps(nela)

final class ClotsNonEmptyListOps[A](private val nela: NonEmptyList[A]) extends AnyVal:

  /** Removes duplicates from the list by grouping the elements by `f` and reducing each grouping with `reduce`. The
    * order of the returned list is undefined.
    */
  def distinctBy[K](f: A => K, reduce: (A, A) => A = (_, b) => b): NonEmptyList[A] =
    val la = nela.toList.groupMapReduce(f)(identity)(reduce).values.toList
    NonEmptyList.fromListUnsafe(la)

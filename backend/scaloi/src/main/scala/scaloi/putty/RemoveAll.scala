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

package scaloi.putty

type RemoveAll[S <: Tuple, T <: Tuple] <: Tuple = S match
  case EmptyTuple => T
  case h *: t     => RemoveAll[t, RemoveOne[h, T]]

object RemoveAll:
  trait Aux[S <: Tuple, T <: Tuple] extends (T => (S, RemoveAll[S, T]))

  given [T <: Tuple]: Aux[EmptyTuple, T] = t => (EmptyTuple, t)

  given [A, S <: Tuple, T <: Tuple](using
    SRA: RemoveOne.Aux[A, T],
    SRS: Aux[S, RemoveOne[A, T]]
  ): Aux[A *: S, T] = t =>
    val (a, noA) = SRA(t)
    val (s, noS) = SRS(noA)
    (a *: s, noS)

end RemoveAll

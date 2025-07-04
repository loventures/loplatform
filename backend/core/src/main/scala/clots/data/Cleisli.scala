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

package clots.data

import cats.data.Kleisli
import cats.effect.Sync

object Cleisli:

  /** Lifts a function to a Kleisli. Identical to Kleisli.fromFunction but uses `Sync[M].delay` instead of
    * `Applicative[M].pure` because I think `Kleisli.flatMap`'s apparent ability to delay an IO.pure is neither expected
    * nor reliable. I don't understand it. Whereas, I know Sync[M].delay will delay.
    */
  def fromFunction[M[_], R]: CleisliFromFunctionPartiallyApplied[M, R] =
    new CleisliFromFunctionPartiallyApplied[M, R]

  final class CleisliFromFunctionPartiallyApplied[M[_], R]:
    def apply[A](f: R => A)(implicit M: Sync[M]): Kleisli[M, R, A] = Kleisli(r => M.delay(f(r)))
end Cleisli

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

package scaloi.free.ui

import scalaz.{:<:, Free, Monad, ~>}

sealed trait ConsoleOp[A]
final case class Ask(prompt: String) extends ConsoleOp[String]
final case class Tell(msg: String)   extends ConsoleOp[Unit]

final class Console[F[_]](implicit I: ConsoleOp :<: F):
  type ConsoleIO[A] = Free[F, A]
  private def lift[A](op: ConsoleOp[A]): ConsoleIO[A] = Free.liftF(I.inj(op))

  /** Ask the user for input.
    */
  def ask(prompt: String): ConsoleIO[String] = lift(Ask(prompt))

  /** Write a message to the user's console.
    */
  def tell(msg: String): ConsoleIO[Unit] = lift(Tell(msg))
end Console
object Console:
  implicit def apply[F[_]](implicit I: ConsoleOp :<: F): Console[F] = new Console[F]

final class ToConsole[M[_]: Monad] extends (ConsoleOp ~> M):
  override def apply[A](fa: ConsoleOp[A]): M[A] = fa match
    case Ask(prompt) => Monad[M].point(scala.io.StdIn.readLine(prompt))
    case Tell(msg)   => Monad[M].point(scala.Console.println(msg))

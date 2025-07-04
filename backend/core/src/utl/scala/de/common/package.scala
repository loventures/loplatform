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

package de

import cats.data.Kleisli
import cats.effect.Sync
import cats.syntax.flatMap.*
import cats.syntax.option.*
import cats.{Applicative, InvariantMonoidal, MonadError}
import io.circe.*

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

package object common:

  /** Get an environment variable or fail if unset. */
  def getenv[F[_]](env: String)(implicit F: MonadError[F, Throwable]): F[String] =
    F.point(sys.env.get(env)) *#@% EnvUnset(env)

  /** Prompt for input or fail if eof. */
  def prompt[F[_]: Sync](prompt: String): F[String] =
    Sync[F].delay(Option(System.console.readLine(prompt))) *#@% IOError()

  /** Prompt for password or fail if eof. */
  def promptPassword[F[_]: Sync](prompt: String): F[Array[Char]] =
    Sync[F].delay(Option(System.console.readPassword(prompt))) *#@% IOError()

  implicit class MonadErrorOptionOps[F[_], A](val foa: F[Option[A]]) extends AnyVal:
    def *#@%(ifNone: => Throwable)(implicit F: MonadError[F, Throwable]): F[A] =
      foa.flatMap(_.liftTo[F](ifNone))

  implicit class MonadErrorBooleanOps[F[_], A](val fb: F[Boolean]) extends AnyVal:
    def *#@%(ifFalse: => Throwable)(implicit F: MonadError[F, Throwable]): F[Boolean] =
      fb.flatMap(b => if b then F.point(b) else F.raiseError(ifFalse))

  final class JsonOps(private val self: Json) extends AnyVal:
    def as_![T](msg: => String)(implicit T: Decoder[T]): Try[T] =
      self.as(using T).fold(e => Failure(new RuntimeException(msg, e)), Success.apply)

  @inline implicit final def toCirceJsonOps(j: Json): JsonOps =
    new JsonOps(j)

  /** A function that uses an Http Client. aka `Http[F] => F[A]`
    */
  type Klient[F[_], A] = Kleisli[F, Http[F], A]
  object Klient:
    def pure[F[_]: Applicative, A](x: A): Klient[F, A]            = Kleisli.pure(x)
    def ask[F[_]](implicit F: Applicative[F]): Klient[F, Http[F]] = Kleisli.ask
    def liftF[F[_], B](x: F[B]): Klient[F, B]                     = Kleisli.liftF(x)
    def unit[F[_]: InvariantMonoidal]: Klient[F, Unit]            = Klient.liftF(InvariantMonoidal[F].unit)
    def delay[F[_]: Sync, A](a: => A): Klient[F, A]               = Klient.liftF(Sync[F].delay(a))
end common

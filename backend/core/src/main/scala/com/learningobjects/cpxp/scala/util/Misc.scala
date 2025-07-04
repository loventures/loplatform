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

package com.learningobjects.cpxp.scala.util

import scalaz.Monad

import java.util.{Calendar, Date}
import scala.annotation.nowarn
import scala.concurrent.ExecutionContext
import scala.util.Try

trait Misc:

  /** A type class for representing an isomorphic conversion between a family of types using some type as a base unit.
    */
  trait Conversion[Base]:
    type Target

    def fromBase(value: Base): Target

    def toBase(value: Target): Base

  object TimeFormat:

    implicit object JavaDateTimeFormat extends TimeFormat[Date]:
      override def fromBase(time: Long): Date = new Date(time)

      override def toBase(time: Date): Long = time.getTime

    object LongTimeFormat extends TimeFormat[Long]:
      override def fromBase(time: Long): Long = time

      override def toBase(time: Long): Long = time
  end TimeFormat

  /** A type class that allows functions to implicitly require a format for time based values.
    */
  trait TimeFormat[F] extends Conversion[Long]:
    override type Target = F

    def fromBase(time: Long): F

    def toBase(time: F): Long

  def now[T: TimeFormat]: T =
    implicitly[TimeFormat[T]].fromBase(Calendar.getInstance().getTimeInMillis)

  /** Implicitly unbox a lazy execution context. Useful for having one DI-ed into a service.
    */
  @inline implicit final def unprovideExecutionContext(implicit ecp: () => ExecutionContext): ExecutionContext = ecp()

  /** Try a risky operation which may also return null
    */
  def tryNullable[T](op: => List[T])(default: => Iterable[T]): Iterable[T] =
    val history = Try(op).getOrElse(default)
    Option(history).getOrElse(default)

  /** An error string.
    */
  type ErrorString = String

  type ErrorMessage = I18nMessage

  /** `Try[_]` is a monad. */
  implicit val tryMonad: Monad[Try] =
    new Monad[Try]:
      def bind[A, B](fa: Try[A])(f: (A) => Try[B]): Try[B] =
        fa flatMap f
      def point[A](a: => A): Try[A]                        =
        Try(a)

  /* for use with pure `t` only! */
  @inline @nowarn final def truly[T](t: T): Boolean        = true
  @inline @nowarn final def falsely[T](t: T): Boolean      = false
  @inline @nowarn final def mendaciously[T](t: T): Boolean = false
end Misc

object Misc extends Misc

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

package com.learningobjects.cpxp.scala.cpxp

import java.lang.{Long as JLong, Number as Jumber}

import com.learningobjects.cpxp.Id as CpxpId
import scaloi.GetOrCreate

/** Typeclass describing a type that has an underlying java Long PK.
  * @tparam A
  *   the type
  */
trait PK[A]:

  /** Get the underlying PK. */
  def pk(a: A): JLong

object PK:
  def apply[A](implicit A: PK[A]): PK[A] = A

  implicit val LongPK: PK[Long] = (a: Long) => a

  implicit def JumberPK[N >: Null <: Jumber]: PK[N] =
    (a: N) => if a eq null then null else Long box a.longValue

  implicit def IdPK: PK[CpxpId] = (a: CpxpId) => if a eq null then null else a.getId

  implicit def IdSubPK[I <: CpxpId]: PK[I] = (a: I) => if a eq null then null else a.getId

  implicit def providedPk[T: PK]: PK[() => T] = (p: () => T) => PK[T].pk(p())

  implicit def gocPK[T: PK]: PK[GetOrCreate[T]] = (goc: GetOrCreate[T]) => PK[T].pk(goc.result)

  object ops: // lacrimose simulacrulaculum
    final class PKOps[A](private val self: A) extends AnyVal:
      def pk(implicit PK: PK[A]): JLong = PK.pk(self)

    import language.implicitConversions

    implicit def pkOps[A: PK](a: A): PKOps[A] = new PKOps(a)
end PK

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

import com.learningobjects.cpxp.Id as CpxpId

final class UnboxedIdOps[T](private val self: T) extends AnyVal:

  @inline def id(implicit unboxer: IdUnboxer[T]): Long =
    unboxer `unbox` self

object UnboxedIdOps extends ToUnboxedIdOps

trait IdUnboxer[T]:
  def unbox(t: T): Long

object IdUnboxer extends IdUnboxer0:

  implicit val cpxpIdUnboxer: IdUnboxer[CpxpId] =
    (id: CpxpId) => id.getId.longValue

sealed abstract class IdUnboxer0:
  this: IdUnboxer.type =>
  /*
  import language.experimental.macros

  implicit final def jstructuralUnboxer[T <: { def getId: jl.Long }]: IdUnboxer[T] = macro IdUnboxerMacro.impl[T]
  // just for consistency
  implicit final def structuralUnboxer[T <: { def getId: Long }]: IdUnboxer[T] = macro IdUnboxerMacro.impl[T]

}

object IdUnboxerMacro {
  import reflect.macros.blackbox

  def impl[T: c.WeakTypeTag](c: blackbox.Context): c.Tree = {
    import c.universe._

    val T         = weakTypeOf[T]
    val resultTpe = appliedType(symbolOf[IdUnboxer[_]], T)
    val t         = c.freshName(TermName("t"))

    q""" new $resultTpe { def unbox($t: $T): Long = $t.getId } """
  }
   */

trait ToUnboxedIdOps:
  import language.implicitConversions

  @inline implicit final def ToUnboxedIdOps[T: IdUnboxer](t: T): UnboxedIdOps[T] =
    new UnboxedIdOps[T](t)

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

package com.learningobjects.cpxp.util.lang

import javax.inject.Provider

/** Utility methods for working with objects that produce values out of nowhere. */
object ProviderLike:

  /** The type of a JSR-330 `Provider`. */
  final val Provider: Class[?] = classOf[Provider[?]]

  /** The type of a Scala nullary function. */
  final val Function0: Class[?] = classOf[() => ?]

  /** Whether `clas` is one of the above two classes. */
  def isProviderLike(clas: Class[?]): Boolean =
    (Provider `isAssignableFrom` clas) || (Function0 `isAssignableFrom` clas)

  /** Produce an instance of `clas` which evaluates `f0` lazily. */
  def wrapLazy(clas: Class[?])(f0: => Any): AnyRef = clas match
    case Provider  =>
      new Provider[Any]:
        override def get = f0
    case Function0 => () => f0
    case _         => wrongType(clas)

  /** Produce an instance of `clas` which returns `o`. */
  def wrapStrict(clas: Class[?])(o: Any): AnyRef = wrapLazy(clas)(o)

  def gimme(o: Any): AnyRef = o.getClass match
    case pc if Function0 `isAssignableFrom` pc => o.asInstanceOf[() => AnyRef].apply()
    case pc if Provider `isAssignableFrom` pc  => o.asInstanceOf[Provider[AnyRef]].get()

  private def wrongType(clas: Class[?]): Nothing =
    throw new IllegalArgumentException(s"Class $clas is not provider-like")
end ProviderLike

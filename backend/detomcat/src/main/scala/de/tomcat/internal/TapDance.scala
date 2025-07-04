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

package de.tomcat.internal

import scala.language.implicitConversions

/** Because we like to tap.
  */
object TapDance:
  class TapAny[A](val self: A) extends AnyVal:
    def <|[B](f: A => B): A =
      f(self); self
  implicit def tapAny[A](a: A): TapAny[A] = new TapAny(a)

  class TapBool(val self: Boolean) extends AnyVal:
    def <|?[B](f: => B): Boolean =
      if self then f; self
  implicit def tapBool(b: Boolean): TapBool = new TapBool(b)
end TapDance

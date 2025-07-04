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

import scaloi.putty.Labels

/** Support for creating a list of the human-readable names of the constructor parameters to a type.
  *
  * {{{
  *   case class Foo(bar: Int, fooBaz: String)
  *   val headers = MkHeader[Foo]
  *   headers: List[String]("Bar", "Foo Baz")
  * }}}
  */
object MkHeader:

  /** Extract the human-readable constructor parameter names of a type.
    * @tparam T
    *   the type
    * @return
    *   the constructor parameter names
    */
  def apply[T <: Product](using Labels: Labels[T]): List[String] =
    Labels.labels.map(str => toSeparateWords(str.capitalize))

  private def toSeparateWords(s: String): String = CamelRE.replaceAllIn(s, "$1 ")

  private val CamelRE = "([a-z](?=[A-Z])|[A-Z](?=[A-Z][a-z])|[A-Z](?=[A-Z]))".r
end MkHeader

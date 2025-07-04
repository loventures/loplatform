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

/** Type aliases for the java collections and boxed primitives.
  */
object JTypes:

  type JList[E]   = java.util.List[E]
  type JMap[A, B] = java.util.Map[A, B]
  type JSet[A]    = java.util.Set[A]
  type JLong      = java.lang.Long
  type JInteger   = java.lang.Integer
  type JBoolean   = java.lang.Boolean
  type JDouble    = java.lang.Double
  type JFloat     = java.lang.Float
end JTypes

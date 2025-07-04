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

package com.learningobjects.cpxp.service.query

/** A JSON path element for constructing queries into json(b) fields. */
sealed trait PathElem extends Product:

  /** This element, in a format consumable by postgres */
  def toString: String

object PathElem:

  /** A pointer into a JSON object */
  case class Field(name: String) extends PathElem:
    override def toString = s"'$name'"

  /** A pointer into a JSON array */
  case class Element(ix: Int) extends PathElem:
    override def toString = s"'$ix'"

  import language.implicitConversions

  @inline implicit def apply(name: String): PathElem = Field(name)
  @inline implicit def apply(ix: Int): PathElem      = Element(ix)
end PathElem

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

package loi.cp.storage

import argonaut.CodecJson

/** A Storeable[T] is like a fragment of JSON from a larger JSON document */
trait Storeable[T]:
  val key: String
  val codec: CodecJson[T]
  val empty: T

object Storeable:

  def instance[T](key0: String)(empty0: T)(implicit codec0: CodecJson[T]): Storeable[T] =
    new Storeable[T]:
      override val key: String         = key0
      override val codec: CodecJson[T] = codec0
      override val empty: T            = empty0

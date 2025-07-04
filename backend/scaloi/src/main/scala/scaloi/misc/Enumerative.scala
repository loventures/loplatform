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

package scaloi
package misc

import enumeratum.*
import scalaz.Memo

import scala.reflect.ClassTag

trait Enumerative[E <: EnumEntry]:
  val `enum`: Enum[E]

object Enumerative:
  @inline def apply[E <: EnumEntry](implicit E: Enumerative[E]): E.type = E

  given [E <: EnumEntry](using ClassTag: ClassTag[E]): Enumerative[E] with
    val `enum`: Enum[E] = memo(ClassTag.runtimeClass).asInstanceOf[Enum[E]]

  val memo = Memo.immutableHashMapMemo: (traitClass: Class[?]) =>
    val moduleClass = Class.forName(traitClass.getName + "$")
    val module      = moduleClass.getField("MODULE$").get(null)
    module.asInstanceOf[enumeratum.Enum[? <: EnumEntry]]
end Enumerative

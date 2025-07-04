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

package scaloi.misc

import java.lang.Enum as Jnum
import java.util.EnumSet as JnumSet

import scalaz.{Ordering, Enum as Znum}
import scaloi.syntax.ClassTagOps.*

import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

/** ScalaZ enum evidence of a Java enum.
  * @param values
  *   the Java enum values
  * @tparam A
  *   the Java enum type
  */
final class JEnumEnum[A <: Jnum[A]](values: Array[A]) extends Znum[A]:
  private val n = values.length

  override def pred(a: A): A               = values((values.indexOf(a) + n - 1) % n)
  override def succ(a: A): A               = values((values.indexOf(a) + 1) % n)
  override def min: Option[A]              = values.headOption
  override def max: Option[A]              = values.lastOption
  override def order(x: A, y: A): Ordering =
    Ordering.fromInt(values.indexOf(x) - values.indexOf(y))

/** Jumenum (the real slim shady) companion.
  */
object JEnumEnum:

  /** Get ScalaZ enum evidence of a Java enum.
    * @tparam A
    *   the Java enum type
    * @return
    *   the evidence
    */
  implicit def jEnumEnum[A <: Enum[A]: ClassTag]: Znum[A] =
    new JEnumEnum(JnumSet.allOf(classTagClass[A]).iterator.asScala.toArray)
end JEnumEnum

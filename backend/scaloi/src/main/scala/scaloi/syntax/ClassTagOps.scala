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
package syntax

import scala.language.implicitConversions
import scala.reflect.ClassTag

/** Enhancements on class tags.
  *
  * @param self
  *   the class tag instance
  * @tparam C
  *   the class type
  */
final class ClassTagOps[C](private val self: ClassTag[C]) extends AnyVal:

  /** Cast a value to this class type, if it is type compatible.
    *
    * @param o
    *   the value
    * @return
    *   the value as the target type, if it is compatible
    */
  def option(o: AnyRef): Option[C] = self unapply o
end ClassTagOps

/** Class tag operations companion.
  */
object ClassTagOps extends ToClassTagOps with ClassTagFns

/** Class tag functions.
  */
trait ClassTagFns:

  /** Returns the runtime class of a type with ClassTag evidence.
    *
    * @tparam T
    *   the type of interest
    * @return
    *   the runtime class
    */
  implicit def classTagClass[T: ClassTag]: Class[T] =
    scala.reflect.classTag[T].runtimeClass.asInstanceOf[Class[T]]
end ClassTagFns

/** Implicit conversion for class tag operations.
  */
trait ToClassTagOps:

  /** Implicit conversion from class tag to the class tag enhancements.
    * @param ct
    *   the class tag
    * @tparam C
    *   its type
    */
  implicit def toClassTagOps[C](ct: ClassTag[C]): ClassTagOps[C] = new ClassTagOps(ct)

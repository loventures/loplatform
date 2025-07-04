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

import java.lang.annotation.Annotation

import scala.language.implicitConversions
import scala.reflect.ClassTag

/** Enhancements on classes.
  *
  * @param c
  *   the class instance
  * @tparam C
  *   the class type
  */
final class ClassOps[C](private val self: Class[C]) extends AnyVal:
  import ClassTagOps.classTagClass
  import scalaz.syntax.std.boolean.*

  /** Get an annotation on this class.
    *
    * @tparam T
    *   the annotation type
    * @return
    *   the annotation, if present
    */
  def annotation[T <: Annotation: ClassTag]: Option[T] =
    Option(self.getAnnotation(classTagClass[T]))

  /** Test whether this class is annotated.
    *
    * @tparam T
    *   the annotation type
    * @return
    *   whether the annotation is present
    */
  def annotated[T <: Annotation: ClassTag]: Boolean =
    self.isAnnotationPresent(classTagClass[T])

  /** Cast a value to this class type, if it is type compatible.
    *
    * @param o
    *   the value
    * @return
    *   the value as the target type, if it is compatible
    */
  def option(o: AnyRef): Option[C] =
    self.isInstance(o) option self.cast(o)

  /* scala 3
  import scala.reflect.runtime.{universe => ru}
  /** Convert this Java reflection class object into a Scala reflection symbol.
   */
  def asScala: ru.ClassSymbol =
    mirror.classSymbol(self)

  /** Produce a reflection mirror which could load this class.
   */
  def mirror: ru.Mirror =
    ru.runtimeMirror(self.getClassLoader)
  end scala 3*/
end ClassOps

/** Implicit conversion for class tag operations.
  */
trait ToClassOps:

  /** Implicit conversion from class to the class enhancements.
    * @param c
    *   the class
    * @tparam C
    *   its type
    */
  implicit def toClassOps[C](c: Class[C]): ClassOps[C] = new ClassOps(c)

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
import java.lang.reflect.Proxy

import scala.language.implicitConversions

/** Enhancements on annotation values.
  *
  * @param self
  *   the annotationvalue
  * @tparam A
  *   the annotation type
  */
final class AnnotationOps[A <: Annotation](private val self: A) extends AnyVal:

  /** Override attributes of an annotation value with those specified in a map.
    *
    * This can almost be achieved typely by using a scala proxy macro (e.g. autoproxy or scala-macro-aop) as {{class
    * OverideFoo(val itemType: String,
    * @@delegate
    *   proxy: Foo) extends Foo}} but ultimately scalac fails to generate valid bytecode for the synthetic annotation
    *   class.
    *
    * @param attributes
    *   the attributes to override on the annotation
    * @return
    *   an annotation proxy
    */
  @inline final def ++(attributes: Map[String, AnyRef]): A =
    Proxy
      .newProxyInstance(
        self.getClass.getClassLoader,
        self.getClass.getInterfaces,
        (_, method, _) => attributes.getOrElse(method.getName, method.invoke(self))
      )
      .asInstanceOf[A]
end AnnotationOps

/** Implicit conversion for annotation operations.
  */
trait ToAnnotationOps:

  /** Implicit conversion from annotation to the annotation enhancements.
    * @param a
    *   the annotational thing
    * @tparam A
    *   its type
    */
  implicit def toAnnotationOps[A <: Annotation](a: A): AnnotationOps[A] = new AnnotationOps(a)

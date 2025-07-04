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

import com.learningobjects.cpxp.util.StringUtils
import com.learningobjects.de.web.jackson.JacksonReflectionUtils
import scalaz.syntax.std.boolean.*

import java.lang.reflect.{Method, Proxy}
import scala.compat.java8.OptionConverters.*
import scala.reflect.ClassTag
import scala.util.Try

/** Support for synthetically implementing bean-like interfaces backed by case classes.
  */
trait BeanProxy:

  /** Bean proxy pimping operations.
    * @param o
    *   the object to be pimped
    */
  implicit class BeanProxyOps(o: AnyRef):

    /** Returns an implementation of a bean-like interface, proxying all bean getter methods onto scala-like property
      * accessors on an underlying object. For example, getFoo() is translated into a foo() invocation on the underlying
      * object. Typically this is used to pimp a case class out as a component interface.
      *
      * @param tt
      *   the bean-like interface class tag
      * @tparam A
      *   the bean-like type
      * @return
      *   an implementation of the interface
      */
    def beanProxy[A](implicit tt: ClassTag[A]): A = proxy(o)(using tt)
  end BeanProxyOps

  /** Returns an implementation of a bean-like interface, proxying all bean getter methods onto scala-like property
    * accessors on an underlying object. For example, getFoo() is translated into a foo() invocation on the underlying
    * object. Typically this is used to pimp a case class out as a component interface.
    *
    * @param o
    *   the underlying object
    * @param tt
    *   the bean-like interface class tag
    * @tparam A
    *   the bean-like type
    * @return
    *   an implementation of the bean-like interface
    */
  def proxy[A](o: AnyRef)(implicit tt: ClassTag[A]): A =
    Proxy
      .newProxyInstance(
        o.getClass.getClassLoader,
        Array(tt.runtimeClass),
        (proxy, method, args) =>
          propertyName(method).fold[AnyRef](
            throw new RuntimeException(s"Not a getter: ${method.getName}")
          ) { property =>
            Try(o.getClass.getMethod(property).invoke(o)).toOption.orNull
          }
      )
      .asInstanceOf[A]

  // returns the json property name corresponding with a method, preferring any jackson annotation
  private def propertyName(method: Method): Option[String] =
    JacksonReflectionUtils.getPropertyName(method).asScala.orElse {
      method.getName
        .startsWith("get")
        .option(StringUtils.toLowerCaseFirst(method.getName.substring(3)))
    }
end BeanProxy

object BeanProxy extends BeanProxy

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

package com.learningobjects.cpxp.util

import com.learningobjects.cpxp.util.proxy.ProxyInvocationHandler
import scaloi.syntax.ClassTagOps.classTagClass

import java.lang.reflect.{Method, Proxy}
import scala.reflect.ClassTag
import scala.util.control.NonFatal

/** Support for instrumenting the execution of code by use of runtime proxies.
  */
object InstrumentedProxy:
  // a macro could do this better...

  /** Return an instrumented proxy over an implementation of an interface.
    *
    * @param a
    *   the implementation
    * @tparam A
    *   the interface type
    * @return
    *   the instrumented proxy instance
    */
  def apply[A <: AnyRef: ClassTag](a: A): A =
    Proxy
      .newProxyInstance(a.getClass.getClassLoader, Array(classTagClass[A]), new InstrumentedInvocationHandler[A](a))
      .asInstanceOf[A]

  /** Return an instrumented proxy over an implementation of an interface.
    *
    * @param a
    *   the implementation
    * @param cls
    *   the interface class
    * @tparam A
    *   the interface type
    * @return
    *   the instrumented proxy instance
    */
  def instrument[A <: AnyRef](a: A, cls: Class[A]): A =
    cls.cast(
      Proxy
        .newProxyInstance(a.getClass.getClassLoader, Array(cls), new InstrumentedInvocationHandler[A](a))
    )
end InstrumentedProxy

/** An invocation handler for instrumenting method invocation.
  */
private class InstrumentedInvocationHandler[+A <: AnyRef](a: A) extends ProxyInvocationHandler[A](a):

  /** Invoke a method with APM instrumentation. */
  override protected def invokeImpl(method: Method, args: Array[AnyRef]): AnyRef =
    val tracer = Instrumentation.getTracer(method, a, true)
    try tracer.success(method.invoke(a, args*))
    catch case NonFatal(e) => throw tracer.failure(e)

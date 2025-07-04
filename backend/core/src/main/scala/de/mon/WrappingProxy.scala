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

package de.mon

import com.learningobjects.cpxp.util.proxy.ProxyInvocationHandler
import scaloi.syntax.ClassTagOps.classTagClass

import java.lang.reflect.Method
import scala.reflect.ClassTag

/** Invocation handler that wraps a value of type `A` and proxies any returned values of type `B`.
  *
  * @param a
  *   the value to wrap
  * @tparam A
  *   the type of the proxied object
  * @tparam B
  *   the return type to wrap in a proxy invocation handler
  */
class WrappingProxy[A <: AnyRef, B <: AnyRef: ClassTag: Proxied](a: A) extends ProxyInvocationHandler[A](a):

  /** The runtime class of the return type to wrap. */
  private final val bClass = classTagClass[B]

  /** Invoke the requested method and wrap the resulting value if it is of type `B`. */
  override protected def invokeImpl(method: Method, args: Array[AnyRef]): AnyRef =
    val o = method.invoke(a, args*)
    if bClass.isInstance(o) && !ProxyInvocationHandler.isProxied(o) then MonitorProxy(o.asInstanceOf[B], args) else o

/** Wrapping proxy companion. */
object WrappingProxy:

  /** Wrap a value of type `A` and return a handler that proxies return values of type `B`. */
  def apply[A <: AnyRef, B <: AnyRef: ClassTag: Proxied]: Proxied[A] =
    (a: A, args: Array[AnyRef]) => new WrappingProxy[A, B](a)

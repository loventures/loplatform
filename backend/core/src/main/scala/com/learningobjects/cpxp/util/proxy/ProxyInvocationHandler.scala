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

package com.learningobjects.cpxp.util.proxy

import java.lang.reflect.{InvocationHandler, Method, Proxy}

/** Abstract invocation handler that proxies calls onto an underlying value. Implements somewhat correct equality
  * checking.
  *
  * @param self
  *   the object onto which to proxy calls
  * @tparam A
  *   the type of the proxied object
  */
abstract class ProxyInvocationHandler[+A <: AnyRef](private val self: A) extends InvocationHandler:

  /** Invocation method. */
  override def invoke(proxy: Any, method: Method, args: Array[AnyRef]): AnyRef =
    if method == ProxyInvocationHandler.equalsMethod then method.invoke(self, ProxyInvocationHandler.unwrap(args.head))
    else invokeImpl(method, args)

  /** Invoke a method other than equals.
    *
    * @param method
    *   the method to invoke
    * @param args
    *   the arguments
    * @return
    *   the method result
    */
  protected def invokeImpl(method: Method, args: Array[AnyRef]): AnyRef
end ProxyInvocationHandler

/** Proxy invocation handler companion. */
object ProxyInvocationHandler:

  /** Test whether a value is in fact a proxy backed by this invocation handler.
    *
    * @param o
    *   the value to test
    * @return
    *   whether it is a proxy
    */
  def isProxied(o: AnyRef): Boolean =
    Proxy.isProxyClass(o.getClass) && Proxy.getInvocationHandler(o).isInstanceOf[ProxyInvocationHandler[?]]

  /** The core object equals method. */
  private final val equalsMethod = classOf[Object].getMethod("equals", classOf[Object])

  /** Unwrap a value if it is wropped in a proxy invocation handler.
    *
    * @param a
    *   the value to unwrap
    * @tparam A
    *   the value type
    * @return
    *   the unwrapped value, if it is wrapped, otherwise the original value
    */
  private def unwrap[A <: AnyRef](a: A): A =
    if (a eq null) || !isProxied(a) then a
    else Proxy.getInvocationHandler(a).asInstanceOf[ProxyInvocationHandler[A]].self
end ProxyInvocationHandler

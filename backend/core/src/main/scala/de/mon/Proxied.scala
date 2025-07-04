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

/** A typeclass describing a type that has a proxy invocation handler. */
trait Proxied[A <: AnyRef]:

  /** Get the proxy invocation handler for a value.
    *
    * @param a
    *   the value
    * @param args
    *   arguments used to create the value
    * @return
    *   the proxy invocation handler
    */
  def handler(a: A, args: Array[AnyRef]): ProxyInvocationHandler[A]
end Proxied

/** Proxied companion object. */
object Proxied:

  /** Summon the invocation handler for a proxied type. */
  def handler[A <: AnyRef: Proxied](a: A, args: Array[AnyRef]): ProxyInvocationHandler[A] =
    implicitly[Proxied[A]].handler(a, args)

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

package com.learningobjects.cpxp.component

/** A mixin for non-proxied component implementations. */
trait ComponentImplementation extends ComponentInterface:
  def componentInstance: ComponentInstance

  // useful methods

  override def getComponentInstance: ComponentInstance = componentInstance

  override def isComponent(iface: Class[? <: ComponentInterface]): Boolean =
    componentInstance.getComponent.isSupported(iface)

  override def asComponent[T <: ComponentInterface](iface: Class[T], args: AnyRef*): T =
    componentInstance.getInstance(iface, args*)

  // equality methods

  override def hashCode: Int = componentInstance.hashCode

  override def equals(o: Any): Boolean = o match
    case c: ComponentInterface =>
      componentInstance.equals(c.getComponentInstance)
    case _                     => false

  // to string

  override def toString: String =
    s"Component[${componentInstance.getItem}/${componentInstance.getIdentifier}]"
end ComponentImplementation

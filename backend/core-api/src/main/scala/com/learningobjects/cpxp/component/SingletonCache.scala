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

import com.learningobjects.cpxp.component.internal.DelegateDescriptor

/** Contains singleton instances of eligible components to reduce memory churn.
  *
  * All methods are synchronized.
  */
trait SingletonCache:

  /** Get-or-create a lazily-initialized instance of `service`, which implements all of `service`'s `@Service`-annotated
    * interfaces. Returns `None` if `service` has no such interfaces.
    *
    * Use this in preference to `getRealService` to avoid potential issues with dependency loops.
    */
  private[component] def getInterfaceService(service: DelegateDescriptor): Option[AnyRef]

  /** Get-or-create a real instance of `service`. This will always return an object, not a proxy.
    *
    * Consider using `getInterfaceService` instead for above-mentioned reasons.
    */
  private[component] def getRealService(service: DelegateDescriptor): AnyRef

  private[component] def getComponent(component: ComponentDescriptor): ComponentInstance

  /** Get-or-create an instance of `service` with type `tt`.
    *
    * Throws if `tt` is an interface not implemented by `service`, or if `tt` is a class and not assignable from the
    * implementation class of `service`.
    */
  private[component] def getService[T <: AnyRef](service: DelegateDescriptor, tt: Class[T]): T =
    tt cast {
      if tt.isInterface then
        getInterfaceService(service).getOrElse(
          throw new RuntimeException(s"Could not make an instance of $service as $tt")
        )
      else getRealService(service)
    }
end SingletonCache

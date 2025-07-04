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
import com.google.common.annotations.VisibleForTesting
import com.learningobjects.cpxp.component.internal.DelegateDescriptor

import scala.collection.mutable

class BaseSingletonCache private[component] (env: ComponentEnvironment) extends SingletonCache:
  private val interfaceServices: mutable.Map[DelegateDescriptor, Option[AnyRef]] = mutable.Map.empty
  private val realServices: mutable.Map[DelegateDescriptor, AnyRef]              = mutable.Map.empty
  private val components: mutable.Map[DelegateDescriptor, ComponentInstance]     = mutable.Map.empty

  @VisibleForTesting def setInterfaceService(x: DelegateDescriptor, a: AnyRef): Unit =
    interfaceServices.put(x, Some(a))

  private[component] def getInterfaceService(service: DelegateDescriptor) =
    synchronized {
      def newInstance = service.getComponent
        .getInstance(env, null, null)
        .newServiceInstance(service, false, obj => interfaceServices.put(service, Option(obj)))
      interfaceServices.getOrElseUpdate(
        service,
        if service.getServiceInterfaces.isEmpty then None else Some(newInstance)
      )
    }

  private[component] def getRealService(service: DelegateDescriptor) =
    synchronized {
      def newInstance =
        service.getComponent
          .getInstance(env, null, null)
          .newServiceInstance(service, true, obj => realServices.put(service, obj))
      realServices.getOrElseUpdate(service, newInstance)
    }

  private[component] def getComponent(component: ComponentDescriptor) =
    synchronized {
      def newInstance = component.getInstance(env, null, null)
      components.getOrElseUpdate(component.getDelegate(), newInstance)
    }
end BaseSingletonCache

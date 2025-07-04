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

package com.learningobjects.cpxp.scala.cpxp

import com.learningobjects.cpxp.component.ComponentSupport

import scala.reflect.ClassTag
import scala.util.Try

object Service extends Service

trait Service:
  def service[T](implicit service: ServiceInjector[T]) = service.inject

trait ServiceInjector[T]:
  def inject: T

object ServiceInjector extends LowPrioRequiredServiceInjector:
  implicit def optionalService[T](implicit tt: ClassTag[T]): ServiceInjector[Option[T]] =
    new ServiceInjector[Option[T]]:
      def inject =
        Option(ComponentSupport.lookupService(tt.runtimeClass.asInstanceOf[Class[T]]))

  implicit def tryService[T](implicit service: ServiceInjector[T]): ServiceInjector[Try[T]] =
    new ServiceInjector[Try[T]]:
      def inject = Try(service.inject)

trait LowPrioRequiredServiceInjector:
  implicit def requiredService[T](implicit tt: ClassTag[T]): ServiceInjector[T] = new ServiceInjector[T]:
    def inject =
      Option(ComponentSupport.lookupService(tt.runtimeClass.asInstanceOf[Class[T]]))
        .getOrElse(throw new RuntimeException(s"Unsupported service: $tt"))

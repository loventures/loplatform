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

import org.hibernate.property.access.spi.*

/** Support for finding the Scala-var accessor/mutator for a hibernate entity..
  *
  * You must place this on an entity class PK field if it uses Scala vars for attributes. Doing so makes Hibernate
  * recognize the scalac-generated accessor as the identifier method for the identifier attribute. Without this
  * recognition, the accessor is not the getter and invocation of the accessor will initialize the proxy (undesired when
  * merely accessing id)
  */
class ScalaPropertyAccessStrategy extends PropertyAccessStrategy:
  override def buildPropertyAccess(
    containerJavaType: Class[?],
    propertyName: String,
    setterRequired: Boolean
  ): PropertyAccess =
    new PropertyAccess():
      private val getter = containerJavaType.getDeclaredMethod(propertyName)
      private val setter = containerJavaType.getDeclaredMethod(propertyName + "_$eq", getter.getReturnType)

      override def getPropertyAccessStrategy: PropertyAccessStrategy = ScalaPropertyAccessStrategy.this

      override val getGetter: Getter = new GetterMethodImpl(containerJavaType, propertyName, getter)

      override val getSetter: Setter = new SetterMethodImpl(containerJavaType, propertyName, setter)
end ScalaPropertyAccessStrategy

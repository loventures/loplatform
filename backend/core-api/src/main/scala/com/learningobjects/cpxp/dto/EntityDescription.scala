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

package com.learningobjects.cpxp.dto
import com.learningobjects.cpxp.service.finder.ItemRelation

import java.beans.PropertyDescriptor
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

// Like EntityDescriptor but without any data-access methods (newInstance/get/set)
// Can represent a plain JPA entity and a Finder JPA entity
case class EntityDescription(
  itemType: String,
  entityName: String,
  tableName: String,
  itemRelation: Option[ItemRelation],
  propertyDescriptors: Map[String, PropertyDescriptor]
):

  val isPeered: Boolean   = itemRelation.exists(_.isPeered)
  val isDomained: Boolean = itemRelation.contains(ItemRelation.DOMAIN)

  def containsDataType(name: String): Boolean = propertyDescriptors.contains(name)
  def getPropertyName(name: String): String   = propertyDescriptors(name).getName
end EntityDescription

object EntityDescription:
  def fromJava(
    itemType: String,
    entityName: String,
    tableName: String,
    itemRelation: java.util.Optional[ItemRelation],
    propertyDescriptors: java.util.Map[String, PropertyDescriptor]
  ): EntityDescription =
    EntityDescription(itemType, entityName, tableName, itemRelation.toScala, propertyDescriptors.asScala.toMap)

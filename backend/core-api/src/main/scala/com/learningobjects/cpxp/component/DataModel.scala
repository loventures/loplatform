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

import com.learningobjects.cpxp.dto.Ontology
import com.learningobjects.cpxp.service.finder.Finder
import com.learningobjects.de.web.QueryHandler

import scala.reflect.ClassTag
import scaloi.syntax.classTag.*

/** Data model description of a particular component type.
  *
  * @param itemType
  *   the associated item type
  * @param singleton
  *   whether there exists a single implementation of the component
  * @param schemaMapped
  *   if polymorphic, whether the implementation schema should be used as the identifier stored in the database
  * @param dataTypes
  *   a mapping from API properties to the underlying data types
  * @param handlers
  *   a mapping from API properties to query handlers
  * @tparam T
  *   the component type
  */
final case class DataModel[T](
  itemType: String,
  singleton: Boolean,
  schemaMapped: Boolean,
  dataTypes: Map[String, String],
  handlers: Map[String, Class[? <: QueryHandler]] = Map.empty
)

object DataModel:

  /** Summon a data model from the implicit nether-realm. */
  @inline final def apply[T](implicit dm: DataModel[T]): DataModel[T] = dm

  /** Summon a data model for a finder. */
  implicit def finderDataModel[T <: Finder: ClassTag](implicit o: Ontology): DataModel[T] =
    val itemType = o.getItemTypeForFinder(classTagClass[T])
    DataModel[T](
      itemType = itemType,
      singleton = true,
      schemaMapped = false,
      dataTypes = o.getEntityDescription(itemType).propertyDescriptors map { case (dataType, descriptor) =>
        descriptor.getName -> dataType
      }
    )
  end finderDataModel
end DataModel

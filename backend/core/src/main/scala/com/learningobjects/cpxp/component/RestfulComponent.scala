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

import com.fasterxml.jackson.annotation.JsonView
import com.learningobjects.cpxp.Id
import com.learningobjects.cpxp.component.annotation.*
import com.learningobjects.cpxp.component.web.Method
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.de.web.Queryable

import java.lang.Long as JLong
import javax.validation.groups.Default

/** A restful component has a PK and supports GET, DELETE and PUT.
  *
  * WARNING: Use of this interface implies that any caller who has the right to view this component has the right to
  * update or delete it. Use with caution.
  */
trait RestfulComponent[A <: RestfulComponent[A]] extends ComponentInterface with Id:
  @JsonView(Array(classOf[Default]))
  @Queryable(dataType = DataTypes.META_DATA_TYPE_ID)
  def getId: JLong

  @RequestMapping(method = Method.DELETE)
  def delete(): Unit

  /** Updates this instance. */
  @RequestMapping(method = Method.PUT)
  def update(@RequestBody a: A): A
end RestfulComponent

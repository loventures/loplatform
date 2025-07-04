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

package com.learningobjects.cpxp.service.integration

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.service.item.Item
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

import java.lang as jl

@Entity
@HCache(usage = READ_WRITE)
class RoleMappingFinder extends LeafEntity:
  import RoleMappingFinder.*

  @ManyToOne(fetch = FetchType.LAZY)
  var mappedRole: Item = scala.compiletime.uninitialized

  @Column
  var roleId: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_ROLE_MAPPING_ROLE_TYPE)
  var roleMappingType: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_INDEX)
  var index: jl.Long = scala.compiletime.uninitialized
end RoleMappingFinder

object RoleMappingFinder:
  final val ITEM_TYPE_ROLE_MAPPING             = "RoleMapping"
  final val DATA_TYPE_ROLE_MAPPING_ROLE_ID     = "RoleMapping.roleId"
  final val DATA_TYPE_INDEX                    = "index"
  final val DATA_TYPE_ROLE_MAPPING_MAPPED_ROLE = "RoleMapping.mappedRole"
  final val DATA_TYPE_ROLE_MAPPING_ROLE_TYPE   = "RoleMapping.type"

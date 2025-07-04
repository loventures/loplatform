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

package com.learningobjects.cpxp.service.enrollment

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.integration.IntegrationConstants
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.service.relationship.RoleFinder
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

import java.lang as jl
import java.util.Date

@Entity
@HCache(usage = READ_WRITE)
@SqlIndex(name = "enrollment_role_idx", value = "(parent_id, group_id) WHERE del IS NULL")
class EnrollmentFinder extends LeafEntity:
  import EnrollmentConstants.*

  // do not change this to GroupFinder!
  @ManyToOne(fetch = FetchType.LAZY)
  var group: Item = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var role: RoleFinder = scala.compiletime.uninitialized

  @Column
  @DataType(IntegrationConstants.DATA_TYPE_DATA_SOURCE)
  var dataSource: String = scala.compiletime.uninitialized

  @Column
  @DataType(DataTypes.DATA_TYPE_DISABLED)
  var disabled: jl.Boolean = scala.compiletime.uninitialized

  @Column
  @DataType(DataTypes.DATA_TYPE_START_TIME)
  var startTime: Date = scala.compiletime.uninitialized

  @Column
  @DataType(DataTypes.DATA_TYPE_STOP_TIME)
  var stopTime: Date = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_ENROLLMENT_CREATED_ON)
  var createdOn: Date = scala.compiletime.uninitialized
end EnrollmentFinder

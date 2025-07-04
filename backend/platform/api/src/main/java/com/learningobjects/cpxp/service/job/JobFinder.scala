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

package com.learningobjects.cpxp.service.job

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import com.learningobjects.cpxp.service.component.ComponentConstants
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.lang as jl

@Entity
@HCache(usage = READ_WRITE)
class JobFinder extends PeerEntity:

  @Column
  var schedule: String = scala.compiletime.uninitialized

  @Column
  @DataType(ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER)
  var componentId: String = scala.compiletime.uninitialized

  @Column
  var disabled: jl.Boolean = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var json: JsonNode = scala.compiletime.uninitialized

  @Column
  @FunctionalIndex(function = IndexType.LCASE, byParent = true, nonDeleted = true)
  var name: String = scala.compiletime.uninitialized
end JobFinder

object JobFinder:
  final val ITEM_TYPE_JOB          = "Job"
  final val DATA_TYPE_JOB_NAME     = "Job.name"
  final val DATA_TYPE_JOB_JSON     = "Job.json"
  final val DATA_TYPE_JOB_SCHEDULE = "Job.schedule"
  final val DATA_TYPE_JOB_DISABLED = "Job.disabled"

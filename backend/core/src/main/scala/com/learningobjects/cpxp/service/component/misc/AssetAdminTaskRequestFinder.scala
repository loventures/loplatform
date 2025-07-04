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

package com.learningobjects.cpxp.service.component.misc

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import com.learningobjects.cpxp.service.item.Item
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.util.Date

@Entity
@HCache(usage = READ_WRITE)
class AssetAdminTaskRequestFinder extends PeerEntity:

  @Column(columnDefinition = "TEXT")
  var affectedNodes: String = scala.compiletime.uninitialized

  @Column
  var endTime: Date = scala.compiletime.uninitialized

  @Column
  var operationType: String = scala.compiletime.uninitialized

  @Column
  var reportGuid: String = scala.compiletime.uninitialized

  @Column
  var startTime: Date = scala.compiletime.uninitialized

  @Column
  var status: String = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var targetNode: Item = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var taskReport: JsonNode = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var user: Item = scala.compiletime.uninitialized
end AssetAdminTaskRequestFinder

object AssetAdminTaskRequestFinder:
  final val ITEM_TYPE_ASSET_ADMIN_TASK_REQUEST = "AssetAdminTaskRequest"
  final val DATA_TYPE_AFFECTED_NODES           = "AssetAdminTaskRequest.affectedNodes"
  final val DATA_TYPE_REQUEST_STATUS           = "AssetAdminTaskRequest.status"
  final val DATA_TYPE_REQUEST_USER             = "AssetAdminTaskRequest.user"
  final val DATA_TYPE_TARGET_NODE              = "AssetAdminTaskRequest.targetNode"
  final val DATA_TYPE_REPORT_GUID              = "AssetAdminTaskRequest.reportGuid"
  final val DATA_TYPE_OPERATION_START_TIME     = "AssetAdminTaskRequest.startTime"
  final val DATA_TYPE_TASK_REPORT              = "AssetAdminTaskRequest.taskReport"
  final val DATA_TYPE_OPERATION_END_TIME       = "AssetAdminTaskRequest.endTime"
  final val DATA_TYPE_OPERATION_TYPE           = "AssetAdminTaskRequest.operationType"
end AssetAdminTaskRequestFinder

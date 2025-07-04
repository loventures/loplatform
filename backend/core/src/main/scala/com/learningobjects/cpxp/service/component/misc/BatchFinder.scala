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

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.service.item.Item
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

import java.lang as jl
import java.util.Date

@Entity
@HCache(usage = READ_WRITE)
class BatchFinder extends PeerEntity:
  import BatchFinder.*

  @Column
  var callbackUrl: String = scala.compiletime.uninitialized

  @Column
  var createTime: Date = scala.compiletime.uninitialized

  @Column
  var endTime: Date = scala.compiletime.uninitialized

  @Column
  var failureCount: jl.Long = scala.compiletime.uninitialized

  @Column
  var identifier: String = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var importFile: Item = scala.compiletime.uninitialized

  @Column
  var startTime: Date = scala.compiletime.uninitialized

  @Column
  var status: String = scala.compiletime.uninitialized

  @Column
  var successCount: jl.Long = scala.compiletime.uninitialized

  @Column
  var total: jl.Long = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_IMPORT_TYPE)
  var batchType: String = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var userStarted: Item = scala.compiletime.uninitialized
end BatchFinder

object BatchFinder:
  final val ITEM_TYPE_BATCH                   = "Batch"
  final val DATA_TYPE_BATCH_STATUS            = "Batch.status"
  final val DATA_TYPE_BATCH_IDENTIFIER        = "Batch.identifier"
  final val DATA_TYPE_IMPORT_TYPE             = "Batch.type"
  final val DATA_TYPE_BATCH_SUCCESS_COUNT     = "Batch.successCount"
  final val DATA_TYPE_IMPORT_TASK_IMPORT_FILE = "Batch.importFile"
  final val DATA_TYPE_BATCH_START_TIME        = "Batch.startTime"
  final val DATA_TYPE_BATCH_FAILURE_COUNT     = "Batch.failureCount"
  final val DATA_TYPE_BATCH_CREATE_TIME       = "Batch.createTime"
  final val DATA_TYPE_BATCH_CALLBACK_URL      = "Batch.callbackUrl"
  final val DATA_TYPE_BATCH_USER_STARTED      = "Batch.userStarted"
  final val DATA_TYPE_BATCH_TOTAL             = "Batch.total"
  final val DATA_TYPE_BATCH_END_TIME          = "Batch.endTime"
end BatchFinder

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

package com.learningobjects.cpxp.service.trash

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.service.user.UserFinder
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

import java.util.Date

@Entity
@HCache(usage = READ_WRITE)
class TrashRecordFinder extends LeafEntity:

  @Column
  var created: Date = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var creator: UserFinder = scala.compiletime.uninitialized

  @Column
  var trashId: String = scala.compiletime.uninitialized
end TrashRecordFinder

object TrashRecordFinder:
  final val ITEM_TYPE_TRASH_RECORD = "TrashRecord"
  final val DATA_TYPE_CREATOR      = "TrashRecord.creator"
  final val DATA_TYPE_CREATED      = "TrashRecord.created"
  final val DATA_TYPE_TRASH_ID     = "TrashRecord.trashId"

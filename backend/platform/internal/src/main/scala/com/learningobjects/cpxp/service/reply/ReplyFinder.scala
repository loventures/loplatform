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

package com.learningobjects.cpxp.service.reply

import java.util.Date
import java.lang as jl

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.FunctionalIndex
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

@Entity
@HCache(usage = READ_WRITE)
class ReplyFinder extends PeerEntity:

  @Column
  @FunctionalIndex(function = IndexType.NORMAL, byParent = true, nonDeleted = true)
  var date: Date = scala.compiletime.uninitialized

  @Column
  @FunctionalIndex(function = IndexType.NORMAL, byParent = true, nonDeleted = true)
  var entity: jl.Long = scala.compiletime.uninitialized

  @Column
  @FunctionalIndex(function = IndexType.NORMAL, byParent = true, nonDeleted = true)
  var messageId: String = scala.compiletime.uninitialized

  @Column
  @FunctionalIndex(function = IndexType.NORMAL, byParent = true, nonDeleted = true)
  var sender: String = scala.compiletime.uninitialized
end ReplyFinder

object ReplyFinder:
  final val ITEM_TYPE_REPLY            = "Reply"
  final val DATA_TYPE_REPLY_SENDER     = "Reply.sender"
  final val DATA_TYPE_REPLY_DATE       = "Reply.date"
  final val DATA_TYPE_REPLY_ENTITY     = "Reply.entity"
  final val DATA_TYPE_REPLY_MESSAGE_ID = "Reply.messageId"

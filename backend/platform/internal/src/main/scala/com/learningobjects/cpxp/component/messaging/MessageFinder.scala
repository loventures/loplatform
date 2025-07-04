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

package com.learningobjects.cpxp.component.messaging

import java.lang as jl

import com.learningobjects.cpxp.entity.*
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

@Entity
@HCache(usage = READ_WRITE)
class MessageFinder extends LeafEntity:

  @Column(length = 16)
  var label: String = scala.compiletime.uninitialized

  @Column
  var read: jl.Boolean = scala.compiletime.uninitialized

  @JoinColumn(nullable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  var storage: MessageStorageFinder = scala.compiletime.uninitialized
end MessageFinder

object MessageFinder:
  final val ITEM_TYPE_MESSAGE         = "Message"
  final val DATA_TYPE_MESSAGE_STORAGE = "Message.storage"
  final val DATA_TYPE_MESSAGE_READ    = "Message.read"
  final val DATA_TYPE_MESSAGE_LABEL   = "Message.label"

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

package com.learningobjects.cpxp.service.item

import java.lang as jl

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

@Entity
@HCache(usage = READ_WRITE)
class FolderFinder extends PeerEntity:
  import FolderFinder.*

  @Column
  @DataType(DATA_TYPE_COMPONENT_IDENTIFIER)
  var componentId: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_TYPE)
  @FriendlyName
  var xtype: String = scala.compiletime.uninitialized

  @Column
  var generation: jl.Long = scala.compiletime.uninitialized
end FolderFinder

object FolderFinder:
  final val ITEM_TYPE_FOLDER               = "Folder"
  final val DATA_TYPE_TYPE                 = "type"
  final val DATA_TYPE_COMPONENT_IDENTIFIER = "componentId"
  final val DATA_TYPE_FOLDER_GENERATION    = "Folder.generation"

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

package com.learningobjects.cpxp.service.script

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.service.attachment.AttachmentFinder
import com.learningobjects.cpxp.service.data.DataTypes
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

import java.lang as jl

@Entity
@HCache(usage = READ_WRITE)
class ComponentArchiveFinder extends PeerEntity:
  import ComponentArchiveFinder.*

  @ManyToOne(fetch = FetchType.LAZY)
  var file: AttachmentFinder = scala.compiletime.uninitialized

  @Column
  var identifier: String = scala.compiletime.uninitialized

  @Column
  @FriendlyName
  var name: String = scala.compiletime.uninitialized

  @Column
  var prefix: String = scala.compiletime.uninitialized

  @Column
  var strip: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_SCRIPT_ARCHIVE_VERSION)
  var componentArchiveVersion: String = scala.compiletime.uninitialized

  @Column
  var generation: jl.Long = scala.compiletime.uninitialized

  @Column
  @DataType(DataTypes.DATA_TYPE_URL)
  var url: String = scala.compiletime.uninitialized
end ComponentArchiveFinder

object ComponentArchiveFinder:
  final val ITEM_TYPE_SCRIPT_ARCHIVE            = "ComponentArchive"
  final val DATA_TYPE_SCRIPT_ARCHIVE_PREFIX     = "ComponentArchive.prefix"
  final val DATA_TYPE_SCRIPT_ARCHIVE_IDENTIFIER = "ComponentArchive.identifier"
  final val DATA_TYPE_SCRIPT_ARCHIVE_NAME       = "ComponentArchive.name"
  final val DATA_TYPE_SCRIPT_ARCHIVE_FILE       = "ComponentArchive.file"
  final val DATA_TYPE_SCRIPT_ARCHIVE_STRIP      = "ComponentArchive.strip"
  final val DATA_TYPE_SCRIPT_ARCHIVE_VERSION    = "ComponentArchive.version"
  final val DATA_TYPE_SCRIPT_ARCHIVE_GENERATION = "ComponentArchive.generation"

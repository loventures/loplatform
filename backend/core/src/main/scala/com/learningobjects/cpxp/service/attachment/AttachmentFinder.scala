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

package com.learningobjects.cpxp.service.attachment

import com.learningobjects.cpxp.component.annotation.ItemMapping
import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.document.DocumentConstants
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.de.web.Queryable
import com.learningobjects.de.web.Queryable.Trait
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

import java.lang as jl
import java.util.Date

@Entity
@HCache(usage = READ_WRITE)
@ItemMapping(AttachmentFinder.ITEM_TYPE_ATTACHMENT)
class AttachmentFinder extends PeerEntity:
  @Column(length = 32)
  var access: String = scala.compiletime.uninitialized

  @Column
  var clientId: String = scala.compiletime.uninitialized

  @Column
  var digest: String = scala.compiletime.uninitialized

  @Column
  var disposition: String = scala.compiletime.uninitialized

  @Column
  @Queryable(dataType = AttachmentFinder.DATA_TYPE_ATTACHMENT_FILE_NAME, traits = Array(Trait.CASE_INSENSITIVE))
  var fileName: String = scala.compiletime.uninitialized

  @Column
  var geometry: String = scala.compiletime.uninitialized

  @Column
  var provider: String = scala.compiletime.uninitialized

  @Column
  var size: jl.Long = scala.compiletime.uninitialized

  @Column
  var thumbnail: String = scala.compiletime.uninitialized

  @Column
  var height: jl.Long = scala.compiletime.uninitialized

  @Column
  var width: jl.Long = scala.compiletime.uninitialized

  @Column
  var generation: jl.Long = scala.compiletime.uninitialized

  @Column
  var reference: jl.Long = scala.compiletime.uninitialized // a reference to the owner when the owner isn't the parent

  @Column
  @DataType(DocumentConstants.DATA_TYPE_CREATE_TIME)
  @Queryable(dataType = DocumentConstants.DATA_TYPE_CREATE_TIME)
  var createTime: Date = scala.compiletime.uninitialized

  @DataType(DocumentConstants.DATA_TYPE_CREATOR)
  @ManyToOne(fetch = FetchType.LAZY)
  var creator: Item = scala.compiletime.uninitialized

  @Column
  @DataType(DocumentConstants.DATA_TYPE_EDIT_TIME)
  var editTime: Date = scala.compiletime.uninitialized

  @DataType(DocumentConstants.DATA_TYPE_EDITOR)
  @ManyToOne(fetch = FetchType.LAZY)
  var editor: Item = scala.compiletime.uninitialized

  @Column
  @DataType(DataTypes.DATA_TYPE_URL)
  var url: String = scala.compiletime.uninitialized
end AttachmentFinder

object AttachmentFinder extends AttachmentConstants:
  final val ITEM_TYPE_ATTACHMENT             = "Attachment"
  final val DATA_TYPE_ATTACHMENT_SIZE        = "Attachment.size"
  final val DATA_TYPE_ATTACHMENT_GEOMETRY    = "Attachment.geometry"
  final val DATA_TYPE_ATTACHMENT_THUMBNAIL   = "Attachment.thumbnail"
  final val DATA_TYPE_ATTACHMENT_ACCESS      = "Attachment.access"
  final val DATA_TYPE_ATTACHMENT_PROVIDER    = "Attachment.provider"
  final val DATA_TYPE_ATTACHMENT_REFERENCE   = "Attachment.reference"
  final val DATA_TYPE_ATTACHMENT_FILE_NAME   = "Attachment.fileName"
  final val DATA_TYPE_ATTACHMENT_HEIGHT      = "Attachment.height"
  final val DATA_TYPE_ATTACHMENT_CLIENT_ID   = "Attachment.clientId"
  final val DATA_TYPE_ATTACHMENT_DIGEST      = "Attachment.digest"
  final val DATA_TYPE_ATTACHMENT_DISPOSITION = "Attachment.disposition"
  final val DATA_TYPE_ATTACHMENT_WIDTH       = "Attachment.width"
  final val DATA_TYPE_ATTACHMENT_GENERATION  = "Attachment.generation"
end AttachmentFinder

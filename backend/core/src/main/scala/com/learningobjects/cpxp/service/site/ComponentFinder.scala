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

package com.learningobjects.cpxp.service.site

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import com.learningobjects.cpxp.service.attachment.AttachmentFinder
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.service.user.UserFinder
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.util.Date
import java.lang as jl

@Entity
@HCache(usage = READ_WRITE)
class ComponentFinder extends PeerEntity:
  import ComponentFinder.*

  @Column
  @FunctionalIndex(byParent = false, nonDeleted = false, function = IndexType.NORMAL)
  var category: String = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_ARCHETYPE)
  @ManyToOne(fetch = FetchType.LAZY)
  var archetype: Item = scala.compiletime.uninitialized

  @Column(columnDefinition = "TEXT")
  @DataType(DATA_TYPE_BODY)
  var body: String = scala.compiletime.uninitialized

  @Column(columnDefinition = "TEXT")
  @DataType(DATA_TYPE_COMPONENT_CONFIGURATION)
  var componentConfiguration: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_COMPONENT_IDENTIFIER)
  var componentId: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_CREATE_TIME)
  var createTime: Date = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_CREATOR)
  @ManyToOne(fetch = FetchType.LAZY)
  var creator: UserFinder = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_DISABLED)
  var disabled: jl.Boolean = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_ICON)
  @ManyToOne(fetch = FetchType.LAZY)
  var icon: AttachmentFinder = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_ICON_NAME)
  var iconName: String = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_IMAGE)
  @ManyToOne(fetch = FetchType.LAZY)
  var image: AttachmentFinder = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_INDEX)
  var index: jl.Long = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @DataType(DATA_TYPE_JSON)
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var json: JsonNode = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_NAME)
  var name: String = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_PROTOTYPE)
  @ManyToOne(fetch = FetchType.LAZY)
  var prototype: Item = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_URL)
  var url: String = scala.compiletime.uninitialized

  @Column
  var generation: jl.Long = scala.compiletime.uninitialized
end ComponentFinder

object ComponentFinder:
  final val ITEM_TYPE_SITE                    = "Component"
  final val DATA_TYPE_BODY                    = "body"
  final val DATA_TYPE_NAME                    = "name"
  final val DATA_TYPE_URL                     = "url"
  final val DATA_TYPE_JSON                    = "json"
  final val DATA_TYPE_IMAGE                   = "image"
  final val DATA_TYPE_CREATE_TIME             = "createTime"
  final val DATA_TYPE_ICON                    = "icon"
  final val DATA_TYPE_COMPONENT_IDENTIFIER    = "componentId"
  final val DATA_TYPE_ARCHETYPE               = "archetype"
  final val DATA_TYPE_CREATOR                 = "creator"
  final val DATA_TYPE_ICON_NAME               = "iconName"
  final val DATA_TYPE_DISABLED                = "disabled"
  final val DATA_TYPE_PROTOTYPE               = "prototype"
  final val DATA_TYPE_COMPONENT_CATEGORY      = "Component.category"
  final val DATA_TYPE_COMPONENT_GENERATION    = "Component.generation"
  final val DATA_TYPE_COMPONENT_CONFIGURATION = "componentConfiguration"
  final val DATA_TYPE_INDEX                   = "index"
end ComponentFinder

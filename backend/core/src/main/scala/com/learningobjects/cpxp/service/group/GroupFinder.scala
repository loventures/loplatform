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

package com.learningobjects.cpxp.service.group

import com.fasterxml.jackson.databind.JsonNode
import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.postgresql.JsonNodeUserType
import com.learningobjects.cpxp.service.attachment.AttachmentFinder
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.subtenant.SubtenantFinder
import com.learningobjects.cpxp.service.user.UserFinder
import jakarta.persistence.*
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE
import org.hibernate.annotations.{JdbcType, Type, Cache as HCache}
import org.hibernate.dialect.`type`.PostgreSQLJsonPGObjectJsonbType

import java.lang as jl
import java.util.Date

@Entity
@HCache(usage = READ_WRITE)
class GroupFinder extends PeerEntity:
  import GroupFinder.*

  @Column(columnDefinition = "JSONB")
  @DataType(DATA_TYPE_CONFIGURATION_BLOB)
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var configuration: JsonNode = scala.compiletime.uninitialized

  @Column
  var externalIdentifier: String = scala.compiletime.uninitialized

  @Column
  var linkedAsset_id: jl.Long = scala.compiletime.uninitialized

  @Column
  var linkedAsset: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_COMPONENT_IDENTIFIER)
  var componentId: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_CREATE_TIME)
  var createTime: Date = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  @DataType(DataTypes.DATA_TYPE_CREATOR)
  var creator: UserFinder = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_DISABLED)
  var disabled: jl.Boolean = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_END_DATE)
  var endDate: Date = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_SHUTDOWN_DATE)
  var shutdownDate: Date = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_GROUP_ID)
  @FriendlyName
  var groupId: String = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_IMAGE)
  @ManyToOne(fetch = FetchType.LAZY)
  var image: AttachmentFinder = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_IN_DIRECTORY)
  var inDirectory: jl.Boolean = scala.compiletime.uninitialized

  @Column(columnDefinition = "JSONB")
  @DataType(DATA_TYPE_JSON)
  @Type(classOf[JsonNodeUserType])
  @JdbcType(classOf[PostgreSQLJsonPGObjectJsonbType])
  var json: JsonNode = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_LOGO)
  @ManyToOne(fetch = FetchType.LAZY)
  var logo: AttachmentFinder = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_NAME)
  @FunctionalIndex(byParent = false, nonDeleted = false, function = IndexType.FULL_TEXT_SIMPLE)
  var name: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_START_DATE)
  var startDate: Date = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_URL)
  var url: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_TYPE)
  var xtype: String = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var subtenant: SubtenantFinder = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var masterCourse: GroupFinder = scala.compiletime.uninitialized

  @Column
  var project: jl.Long = scala.compiletime.uninitialized

  @Column
  var branch: jl.Long = scala.compiletime.uninitialized

  @Column
  var commit: jl.Long = scala.compiletime.uninitialized

  @Column
  var generation: jl.Long = scala.compiletime.uninitialized // nonnull for lightweight courses

  @Column
  var archived: jl.Boolean = scala.compiletime.uninitialized

  @Column
  var selfStudy: jl.Boolean = scala.compiletime.uninitialized
end GroupFinder

object GroupFinder:
  final val ITEM_TYPE_GROUP                     = "Group"
  final val DATA_TYPE_NAME                      = "name"
  final val DATA_TYPE_GROUP_EXTERNAL_IDENTIFIER = "Group.externalIdentifier"
  final val DATA_TYPE_END_DATE                  = "endDate"
  final val DATA_TYPE_SHUTDOWN_DATE             = "shutdownDate"
  final val DATA_TYPE_URL                       = "url"
  final val DATA_TYPE_JSON                      = "json"
  final val DATA_TYPE_IMAGE                     = "image"
  final val DATA_TYPE_CREATE_TIME               = "createTime"
  final val DATA_TYPE_GROUP_SUBTENANT           = "Group.subtenant"
  final val DATA_TYPE_COMPONENT_IDENTIFIER      = "componentId"
  final val DATA_TYPE_CONFIGURATION_BLOB        = "Configuration.configuration"
  final val DATA_TYPE_GROUP_ID                  = "groupId"
  final val DATA_TYPE_GROUP_LINKED_ASSET_ID     = "Group.linkedAsset_id"
  final val DATA_TYPE_GROUP_LINKED_ASSET_NAME   = "Group.linkedAsset"
  final val DATA_TYPE_LOGO                      = "logo"
  final val DATA_TYPE_DISABLED                  = "disabled"
  final val DATA_TYPE_TYPE                      = "type"
  final val DATA_TYPE_START_DATE                = "startDate"
  final val DATA_TYPE_IN_DIRECTORY              = "inDirectory"
  final val DATA_TYPE_GROUP_MASTER_COURSE       = "Group.masterCourse"
  final val DATA_TYPE_GROUP_PROJECT             = "Group.project"
  final val DATA_TYPE_GROUP_BRANCH              = "Group.branch"
  final val DATA_TYPE_GROUP_GENERATION          = "Group.generation"
  final val DATA_TYPE_GROUP_ARCHIVED            = "Group.archived"
  final val DATA_TYPE_GROUP_SELF_STUDY          = "Group.selfStudy"
end GroupFinder

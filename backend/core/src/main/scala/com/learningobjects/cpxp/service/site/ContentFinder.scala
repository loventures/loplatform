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

import java.util.Date
import java.lang as jl

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.service.user.UserFinder
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

@Entity
@HCache(usage = READ_WRITE)
class ContentFinder extends PeerEntity:
  import ContentFinder.*

  @Column
  var replyThread: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_HIERARCHY_INDEX)
  var index: jl.Long = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_HIERARCHY_PARENT)
  @ManyToOne(fetch = FetchType.LAZY)
  var hierarchyParent: Item = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_ACTIVITY_TIME)
  var activityTime: Date = scala.compiletime.uninitialized

  @Column(columnDefinition = "TEXT")
  @DataType(DATA_TYPE_BODY)
  var body: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_CREATE_TIME)
  var createTime: Date = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_CREATOR)
  @ManyToOne(fetch = FetchType.LAZY)
  var creator: UserFinder = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_DELETED)
  var deleted: jl.Boolean = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_EDIT_TIME)
  var editTime: Date = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_EDITOR)
  @ManyToOne(fetch = FetchType.LAZY)
  var editor: Item = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_ICON_NAME)
  var iconName: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_LOCKED)
  var locked: jl.Boolean = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_NAME)
  var name: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_PUBLISH_TIME)
  var publishTime: Date = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_URL)
  var url: String = scala.compiletime.uninitialized
end ContentFinder

object ContentFinder:
  final val ITEM_TYPE_CONTENT          = "Content"
  final val DATA_TYPE_BODY             = "body"
  final val DATA_TYPE_NAME             = "name"
  final val DATA_TYPE_URL              = "url"
  final val DATA_TYPE_CREATE_TIME      = "createTime"
  final val DATA_TYPE_HIERARCHY_INDEX  = "Hierarchy.index"
  final val DATA_TYPE_HIERARCHY_PARENT = "Hierarchy.parent"
  final val DATA_TYPE_LOCKED           = "locked"
  final val DATA_TYPE_CREATOR          = "creator"
  final val DATA_TYPE_REPLY_THREAD     = "Content.replyThread"
  final val DATA_TYPE_ICON_NAME        = "iconName"
  final val DATA_TYPE_EDITOR           = "editor"
  final val DATA_TYPE_PUBLISH_TIME     = "publishTime"
  final val DATA_TYPE_ACTIVITY_TIME    = "activityTime"
  final val DATA_TYPE_DELETED          = "deleted"
  final val DATA_TYPE_EDIT_TIME        = "editTime"
end ContentFinder

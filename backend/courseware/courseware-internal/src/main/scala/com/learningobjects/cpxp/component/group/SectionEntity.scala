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

package com.learningobjects.cpxp.component.group

import java.util.Date
import java.lang as jl

import com.learningobjects.cpxp.entity.PeerEntity
import com.learningobjects.cpxp.entity.annotation.{DataType, FriendlyName}
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.subtenant.SubtenantFinder
import com.learningobjects.cpxp.service.user.UserFinder
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

/** A view on the group finder table for a course section.
  *
  * @see
  *   GroupFinder
  */
@Entity
@Table(name = "groupfinder")
@HCache(usage = READ_WRITE)
class SectionEntity extends PeerEntity:
  import com.learningobjects.cpxp.service.group.GroupFinder

  @Column
  @DataType(GroupFinder.DATA_TYPE_COMPONENT_IDENTIFIER)
  var componentId: String = scala.compiletime.uninitialized

  @Column
  @DataType(GroupFinder.DATA_TYPE_NAME)
  var name: String = scala.compiletime.uninitialized

  @Column
  @DataType(GroupFinder.DATA_TYPE_GROUP_ID)
  @FriendlyName
  var groupId: String = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  @DataType(DataTypes.DATA_TYPE_CREATOR)
  var creator: UserFinder = scala.compiletime.uninitialized

  @Column
  @DataType(GroupFinder.DATA_TYPE_CREATE_TIME)
  var createTime: Date = scala.compiletime.uninitialized

  @Column
  @DataType(GroupFinder.DATA_TYPE_START_DATE)
  var startDate: Date = scala.compiletime.uninitialized

  @Column
  @DataType(GroupFinder.DATA_TYPE_END_DATE)
  var endDate: Date = scala.compiletime.uninitialized

  @Column
  @DataType(GroupFinder.DATA_TYPE_SHUTDOWN_DATE)
  var shutdownDate: Date = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var subtenant: SubtenantFinder = scala.compiletime.uninitialized

  @ManyToOne(fetch = FetchType.LAZY)
  var masterCourse: GroupFinder = scala.compiletime.uninitialized

  @Column
  @DataType(GroupFinder.DATA_TYPE_TYPE)
  var xtype: String = scala.compiletime.uninitialized

  @Column
  @DataType(GroupFinder.DATA_TYPE_GROUP_PROJECT)
  var project: jl.Long = scala.compiletime.uninitialized

  @Column
  @DataType(GroupFinder.DATA_TYPE_GROUP_BRANCH)
  var branch: jl.Long = scala.compiletime.uninitialized

  @Column
  var commit: jl.Long = scala.compiletime.uninitialized

  @Column
  @DataType(GroupFinder.DATA_TYPE_GROUP_LINKED_ASSET_ID)
  var linkedAsset_id: jl.Long = scala.compiletime.uninitialized

  @Column
  @DataType(GroupFinder.DATA_TYPE_GROUP_LINKED_ASSET_NAME)
  var linkedAsset: String = scala.compiletime.uninitialized

  @Column
  @DataType(GroupFinder.DATA_TYPE_GROUP_GENERATION)
  var generation: jl.Long = scala.compiletime.uninitialized

  @Column
  @DataType(GroupFinder.DATA_TYPE_GROUP_ARCHIVED)
  var archived: jl.Boolean = scala.compiletime.uninitialized

  @Column
  @DataType(GroupFinder.DATA_TYPE_DISABLED)
  var disabled: jl.Boolean = scala.compiletime.uninitialized

  @Column
  @DataType(GroupFinder.DATA_TYPE_GROUP_SELF_STUDY)
  var selfStudy: jl.Boolean = scala.compiletime.uninitialized

  @Column
  @DataType(GroupFinder.DATA_TYPE_URL)
  var url: jl.String = scala.compiletime.uninitialized
end SectionEntity

object SectionEntity:
  val itemType = "Group"

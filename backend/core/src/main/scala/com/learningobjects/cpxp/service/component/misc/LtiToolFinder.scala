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

package com.learningobjects.cpxp.service.component.misc

import com.learningobjects.cpxp.entity.*
import com.learningobjects.cpxp.entity.annotation.*
import com.learningobjects.cpxp.service.attachment.AttachmentFinder
import jakarta.persistence.*
import org.hibernate.annotations.Cache as HCache
import org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE

import java.lang as jl

@Entity
@HCache(usage = READ_WRITE)
class LtiToolFinder extends PeerEntity:
  import LtiToolFinder.*

  @Column
  var adminOnly: jl.Boolean = scala.compiletime.uninitialized

  @Column(columnDefinition = "TEXT")
  var configuration: String = scala.compiletime.uninitialized

  @Column
  var branched: jl.Boolean =
    scala.compiletime.uninitialized // means this tool should pretend to copy from a branch section

  @Column
  var global: jl.Boolean = scala.compiletime.uninitialized

  @Column
  var toolId: String = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_DISABLED)
  var disabled: jl.Boolean = scala.compiletime.uninitialized

  @DataType(DATA_TYPE_ICON)
  @ManyToOne(fetch = FetchType.LAZY)
  var icon: AttachmentFinder = scala.compiletime.uninitialized

  @Column
  @DataType(DATA_TYPE_NAME)
  var name: String = scala.compiletime.uninitialized
end LtiToolFinder

object LtiToolFinder:
  final val ITEM_TYPE_LTI_TOOL               = "LtiTool"
  final val DATA_TYPE_NAME                   = "name"
  final val DATA_TYPE_ICON                   = "icon"
  final val DATA_TYPE_LTI_TOOL_ID            = "LtiTool.toolId"
  final val DATA_TYPE_LTI_TOOL_BRANCHED      = "LtiTool.branched"
  final val DATA_TYPE_DISABLED               = "disabled"
  final val DATA_TYPE_LTI_TOOL_CONFIGURATION = "LtiTool.configuration"
end LtiToolFinder

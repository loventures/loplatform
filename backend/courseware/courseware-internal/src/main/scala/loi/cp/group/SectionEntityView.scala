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

package loi.cp.group

import java.util.Date
import java.lang as jl

import com.learningobjects.cpxp.service.group.GroupFinder
import com.learningobjects.cpxp.service.item.Item
import com.learningobjects.cpxp.service.subtenant.SubtenantFinder
import com.learningobjects.cpxp.service.user.UserFinder

/** A view on the group finder table for a course section.
  *
  * @see
  *   GroupFinder
  */
case class SectionEntityView(
  id: jl.Long,
  componentId: String,
  name: String,
  groupId: String,
  creator: UserFinder,
  createTime: Date,
  startDate: Date,
  endDate: Date,
  shutdownDate: Date,
  subtenant: SubtenantFinder,
  masterCourse: GroupFinder,
  xtype: String,
  project: jl.Long,
  branch: jl.Long,
  commit: jl.Long,
  linkedAsset_id: jl.Long,
  linkedAsset: String,
  generation: jl.Long,
  archived: jl.Boolean,
  disabled: jl.Boolean,
  selfStudy: jl.Boolean,
  url: jl.String,
  path: String,
  owner: Item,
  parent: Item,
  root: Item
):

  /** Returns whether this is a preview section. Preview sections do not maintain the commit and root id values in the
    * same manner normal course sections do. Caching should not be done for preview sections.
    *
    * @return
    *   whether this is a preview section
    */
  def isPreview: Boolean =
    xtype == SectionType.PreviewSections.name

  def toGroupFinder: GroupFinder =
    val entity = new GroupFinder

    entity.setId(this.id)
    entity.componentId = this.componentId
    entity.name = this.name
    entity.groupId = this.groupId
    entity.creator = this.creator
    entity.createTime = this.createTime
    entity.startDate = this.startDate
    entity.endDate = this.endDate
    entity.shutdownDate = this.shutdownDate
    entity.subtenant = this.subtenant
    entity.masterCourse = this.masterCourse
    entity.xtype = this.xtype
    entity.project = this.project
    entity.branch = this.branch
    entity.commit = this.commit
    entity.linkedAsset_id = this.linkedAsset_id
    entity.linkedAsset = this.linkedAsset
    entity.generation = this.generation
    entity.archived = this.archived
    entity.disabled = this.disabled
    entity.selfStudy = this.selfStudy
    entity.url = this.url
    entity.setPath(this.path)
    entity.setOwner(this.owner)
    entity.setParent(this.parent)
    entity.setRoot(this.root)

    entity
  end toGroupFinder
end SectionEntityView

object SectionEntityView:
  final val itemType = "Group"

  def apply(entity: GroupFinder): SectionEntityView =
    SectionEntityView(
      entity.id(),
      entity.componentId,
      entity.name,
      entity.groupId,
      entity.creator,
      entity.createTime,
      entity.startDate,
      entity.endDate,
      entity.shutdownDate,
      entity.subtenant,
      entity.masterCourse,
      entity.xtype,
      entity.project,
      entity.branch,
      entity.commit,
      entity.linkedAsset_id,
      entity.linkedAsset,
      entity.generation,
      entity.archived,
      entity.disabled,
      entity.selfStudy,
      entity.url,
      entity.path(),
      entity.owner(),
      entity.parent(),
      entity.root()
    )
end SectionEntityView

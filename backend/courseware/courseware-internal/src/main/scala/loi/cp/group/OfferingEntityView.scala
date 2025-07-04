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
import com.learningobjects.cpxp.service.user.UserFinder

/** A view on the group finder table for a offering.
  *
  * @see
  *   GroupFinder
  */
case class OfferingEntityView(
  id: jl.Long,
  componentId: String,
  name: String,
  groupId: String,
  creator: UserFinder,
  createTime: Date,
  xtype: String,
  project: jl.Long,
  branch: jl.Long,
  commit: jl.Long,
  linkedAsset_id: jl.Long,
  linkedAsset: String,
  generation: Long,
  archived: jl.Boolean,
  disabled: jl.Boolean,
  url: String,
  path: String,
  owner: Item,
  parent: Item,
  root: Item
):
  def toGroupFinder: GroupFinder =
    val entity = new GroupFinder

    entity.setId(this.id)
    entity.componentId = this.componentId
    entity.name = this.name
    entity.groupId = this.groupId
    entity.creator = this.creator
    entity.createTime = this.createTime
    entity.xtype = this.xtype
    entity.project = this.project
    entity.branch = this.branch
    entity.commit = this.commit
    entity.linkedAsset_id = this.linkedAsset_id
    entity.linkedAsset = this.linkedAsset
    entity.generation = this.generation
    entity.archived = this.archived
    entity.disabled = this.disabled
    entity.url = this.url
    entity.setPath(this.owner.path())
    entity.setOwner(this.owner)
    entity.setParent(this.parent)
    entity.setRoot(this.root)

    entity
  end toGroupFinder
end OfferingEntityView

object OfferingEntityView:
  final val itemType = SectionEntityView.itemType

  def apply(entity: GroupFinder): OfferingEntityView =
    OfferingEntityView(
      entity.id,
      entity.componentId,
      entity.name,
      entity.groupId,
      entity.creator,
      entity.createTime,
      entity.xtype,
      entity.project,
      entity.branch,
      entity.commit,
      entity.linkedAsset_id,
      entity.linkedAsset,
      entity.generation,
      entity.archived,
      entity.disabled,
      entity.url,
      entity.path(),
      entity.owner(),
      entity.parent(),
      entity.root()
    )
end OfferingEntityView

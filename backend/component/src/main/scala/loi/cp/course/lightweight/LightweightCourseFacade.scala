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

package loi.cp.course
package lightweight

import com.learningobjects.cpxp.dto.{FacadeData, FacadeItem}
import com.learningobjects.cpxp.service.group.{GroupConstants, GroupFinder}

@FacadeItem(GroupFinder.ITEM_TYPE_GROUP)
private[lightweight] trait LightweightCourseFacade extends CourseFacade:

  @FacadeData(GroupConstants.DATA_TYPE_GROUP_LINKED_ASSET_ID)
  def getLinkedAssetId: Long
  def setLinkedAssetId(linkedAssetId: Long): Unit

  @FacadeData(GroupConstants.DATA_TYPE_GROUP_LINKED_ASSET_NAME)
  def getLinkedAssetName: String
  def setLinkedAssetName(linkedAssetName: String): Unit

  @FacadeData(GroupFinder.DATA_TYPE_GROUP_GENERATION)
  def getGeneration: Option[Long]
  def setGeneration(gen: Long): Unit

  @FacadeData(GroupFinder.DATA_TYPE_GROUP_ARCHIVED)
  def isArchived: Boolean
  def setArchived(archived: Boolean): Unit

  @FacadeData(GroupFinder.DATA_TYPE_GROUP_SELF_STUDY)
  def getSelfStudy: Option[Boolean]
  def setSelfStudy(selfStudy: Boolean): Unit
end LightweightCourseFacade

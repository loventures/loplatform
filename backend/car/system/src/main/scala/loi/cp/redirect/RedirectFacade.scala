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

package loi.cp.redirect

import com.learningobjects.cpxp.controller.upload.UploadInfo
import com.learningobjects.cpxp.dto.{Facade, FacadeData, FacadeItem}
import com.learningobjects.cpxp.service.attachment.AttachmentFacade
import com.learningobjects.cpxp.service.data.DataTypes
import com.learningobjects.cpxp.service.site.SiteConstants

@FacadeItem(SiteConstants.ITEM_TYPE_SITE)
trait RedirectFacade extends Facade:
  @FacadeData(DataTypes.DATA_TYPE_ATTACHMENT)
  def getActiveAttachment: AttachmentFacade
  def setActiveAttachment(info: UploadInfo): Unit

  @FacadeData(DataTypes.DATA_TYPE_DISABLED)
  def getDisabled: Boolean
  def setDisabled(disabled: Boolean): Unit

  @FacadeData(DataTypes.DATA_TYPE_NAME)
  def getName: String
  def setName(name: String): Unit
end RedirectFacade

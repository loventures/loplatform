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

package com.learningobjects.cpxp.service.trash;

import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.user.UserFacade;

import java.util.Date;

@FacadeItem(TrashConstants.ITEM_TYPE_TRASH_RECORD)
public interface TrashRecordFacade extends Facade {
    @FacadeData(TrashConstants.DATA_TYPE_CREATOR)
    public UserFacade getCreator();
    public void setCreator(Long creator);
    @FacadeData(TrashConstants.DATA_TYPE_CREATOR)
    public Long getCreatorId();

    @FacadeData(TrashConstants.DATA_TYPE_CREATED)
    public Date getCreated();
    public void setCreated(Date date);

    @FacadeData(TrashConstants.DATA_TYPE_TRASH_ID)
    public String getTrashId();
    public void setTrashId(String trashId);
}

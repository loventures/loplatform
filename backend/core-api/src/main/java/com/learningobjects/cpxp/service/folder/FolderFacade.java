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

package com.learningobjects.cpxp.service.folder;

import com.learningobjects.cpxp.dto.FacadeData;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.component.ComponentConstants;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.name.UrlFacade;

@FacadeItem(FolderConstants.ITEM_TYPE_FOLDER)
public interface FolderFacade extends UrlFacade {
    @FacadeData(DataTypes.DATA_TYPE_ID)
    String getIdStr();
    void setIdStr(String id);

    @FacadeData(DataTypes.DATA_TYPE_TYPE)
    String getType();
    void setType(String type);

    @FacadeData(ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER)
    String getIdentifier();
    void setIdentifier(String identifier);

    @FacadeData(FolderConstants.DATA_TYPE_FOLDER_GENERATION)
    Long getGeneration();
    void setGeneration(Long generation);

    boolean refresh(boolean lock);
}

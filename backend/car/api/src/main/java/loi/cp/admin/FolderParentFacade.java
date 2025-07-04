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

package loi.cp.admin;

import com.learningobjects.cpxp.dto.FacadeChild;
import com.learningobjects.cpxp.dto.FacadeCondition;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.folder.FolderConstants;
import com.learningobjects.cpxp.service.folder.FolderFacade;
import com.learningobjects.cpxp.service.portal.NameFacade;
import scaloi.GetOrCreate;

import java.util.List;

@FacadeItem("*")
public interface FolderParentFacade extends NameFacade {
    @FacadeChild(FolderConstants.ITEM_TYPE_FOLDER)
    public List<FolderFacade> getFolders();
    FolderFacade addFolder();
    public FolderFacade findFolderByType(
      @FacadeCondition(DataTypes.DATA_TYPE_TYPE) String type
    );
    public GetOrCreate<FolderFacade> getOrCreateFolderByType(
      @FacadeCondition(DataTypes.DATA_TYPE_TYPE) String type
    );
    public GetOrCreate<FolderFacade> getOrCreateFolderByIdString(
      @FacadeCondition(DataTypes.DATA_TYPE_ID) String idString
    );
}

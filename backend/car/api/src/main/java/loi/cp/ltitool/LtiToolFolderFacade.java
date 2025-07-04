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

package loi.cp.ltitool;

import com.learningobjects.cpxp.dto.FacadeComponent;
import com.learningobjects.cpxp.dto.FacadeItem;
import com.learningobjects.cpxp.service.folder.FolderConstants;
import com.learningobjects.cpxp.service.folder.FolderFacade;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import com.learningobjects.cpxp.service.script.ComponentFacade;

import java.util.List;

@FacadeItem(FolderConstants.ITEM_TYPE_FOLDER)
public interface LtiToolFolderFacade extends FolderFacade, ComponentFacade {
    @FacadeComponent
    List<LtiToolComponent> getLtiTools();

    LtiToolComponent addLtiTool();

    LtiToolComponent getLtiTool(Long id);

    QueryBuilder queryLtiTools();
    
    String ID = "folder-ltitools";
}

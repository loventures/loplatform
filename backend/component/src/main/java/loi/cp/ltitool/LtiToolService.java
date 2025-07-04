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

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.domain.DomainConstants;
import com.learningobjects.cpxp.service.facade.FacadeService;
import com.learningobjects.cpxp.service.folder.FolderFacade;
import com.learningobjects.cpxp.service.query.Comparison;
import com.learningobjects.cpxp.service.query.Function;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import loi.cp.admin.FolderParentFacade;

import javax.inject.Inject;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Serivce for getting and querying for LTI tools
 */
@Service
public class LtiToolService {
    public static final String ID_FOLDER_LTI_TOOLS = "folder-ltitools";
    public static final String URL_FOLDER_LTI_TOOLS = "LtiTools";
    public static final String FOLDER_TYPE_LTI_TOOL = "ltitool";

    @Inject
    private FacadeService _facadeService;

    /**
     * Gets the folder containing all the tools for the current domain
     * @return @link{LtiToolFolderFacade}
     */
    public LtiToolFolderFacade getLtiToolFolder() {
        return getLtiToolFolder(Current.getDomainDTO());
    }

    public LtiToolFolderFacade getLtiToolFolder(Id context) {
        FolderParentFacade parent = _facadeService.getFacade(context.getId(), FolderParentFacade.class);
        FolderFacade folder = parent.findFolderByType(FOLDER_TYPE_LTI_TOOL);
        if (folder == null) {
            folder = parent.addFolder();
            folder.setType(FOLDER_TYPE_LTI_TOOL);
            folder.bindUrl(URL_FOLDER_LTI_TOOLS);
            if (DomainConstants.ITEM_TYPE_DOMAIN.equals(parent.getItemType())) {
                folder.setIdStr(ID_FOLDER_LTI_TOOLS);
            }
        }
        return folder.asFacade(LtiToolFolderFacade.class);
    }

    /**
     * Finds an LTI tool by its id
     * @param id -  the id of the tool
     * @return @link{LtiToolComponent}
     */
    public LtiToolComponent getLtiTool(Long id) {
        return getLtiToolFolder().getLtiTool(id);
    }

    /**
     * Attempts to locate a tool by its key, will return first key if multiple tools exist with the same key
     *
     * @param key - the key to query for
     * @return @link{Optional<LtiToolComponent>}
     */

    public QueryBuilder queryLtiToolByName(String name) {
        return getLtiToolFolder()
                .queryLtiTools()
                .addCondition(DataTypes.DATA_TYPE_DISABLED, Comparison.eq, false)
                .addCondition(DataTypes.DATA_TYPE_NAME, Comparison.eq, name.toLowerCase(), Function.LOWER);
    }

    public Optional<LtiToolComponent> getLtiToolByKey(String key) {
        try {
            return getLtiToolFolder()
              .getLtiTools()
              .stream()
              .filter(tool -> tool.getLtiConfiguration().defaultConfiguration().key().isDefined())
              .filter(tool -> tool.getLtiConfiguration().defaultConfiguration().key().get().equals(key))
              .findFirst();
        } catch(NoSuchElementException ex) {
            // This will only occur if the tool has been misconfigured
            return Optional.empty();
        }
    }
}

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

package com.learningobjects.cpxp.service.script;

import com.learningobjects.cpxp.dto.*;
import com.learningobjects.cpxp.service.attachment.AttachmentConstants;
import com.learningobjects.cpxp.service.attachment.AttachmentFacade;

import java.util.List;

@FacadeItem(ScriptConstants.ITEM_TYPE_SCRIPT_ARCHIVE)
public interface ComponentArchiveFacade extends Facade {

    @FacadeData(ScriptConstants.DATA_TYPE_SCRIPT_ARCHIVE_NAME)
    public String getName();
    public void setName(String name);

    @FacadeData(ScriptConstants.DATA_TYPE_SCRIPT_ARCHIVE_VERSION)
    public String getVersion();
    public void setVersion(String name);

    @FacadeData(ScriptConstants.DATA_TYPE_SCRIPT_ARCHIVE_IDENTIFIER)
    public String getIdentifier();
    public void setIdentifier(String identifier);

    @FacadeData(ScriptConstants.DATA_TYPE_SCRIPT_ARCHIVE_PREFIX)
    public String getPrefix();
    public void setPrefix(String prefix);

    @FacadeData(ScriptConstants.DATA_TYPE_SCRIPT_ARCHIVE_STRIP)
    public String getStrip();
    public void setStrip(String strip);

    @FacadeData(value = ScriptConstants.DATA_TYPE_SCRIPT_ARCHIVE_FILE)
    public Long getArchiveFile();
    public void setArchiveFile(Long file);

    @FacadeData(value = ScriptConstants.DATA_TYPE_SCRIPT_ARCHIVE_GENERATION)
    public Long getGeneration();
    public void setGeneration(Long generation);

    @FacadeChild(AttachmentConstants.ITEM_TYPE_ATTACHMENT)
    public List<AttachmentFacade> getFiles();

    @FacadeParent
    public ScriptSiteFacade getParent();

    void refresh(boolean pessimistic);
}

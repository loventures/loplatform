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

package com.learningobjects.cpxp.service.group;

import com.learningobjects.cpxp.service.group.GroupConstants.GroupType;

import javax.ejb.Local;
import java.io.File;
import java.util.List;


/**
 * Group web service.
 */
@Local
public interface GroupWebService {

    // the thorough dereliction of all duty here, deferring to the caller's
    // good judgment, is wrong but someone else can fix that
    public GroupFacade addGroup(Long parentId);

    public GroupFacade getGroup(Long id);

    public GroupFacade getRawGroup(Long id);

    public void removeGroup(Long id);

    public GroupFacade getGroupByGroupId(Long parentId, String groupId);

    public List<GroupFacade> getAllGroups(Long parentId, Integer start, Integer limit, String sortDataType, Boolean ascending);

    public List<GroupFacade> getGroups(Long parentId);

    public void setImage(Long item, String fileName, Long width, Long height, File profileFile, String geometry);

    public Long getGroupFolder(Long domainId);
}

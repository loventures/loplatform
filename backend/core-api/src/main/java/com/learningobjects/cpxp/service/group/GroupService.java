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

import java.util.Collection;
import java.util.List;
import javax.ejb.Local;

import com.learningobjects.cpxp.service.data.Data;
import com.learningobjects.cpxp.service.item.Item;

/**
 * @version 1.0
 */
@Local
public interface GroupService {
    /**
     * Gets a group Item with a specified id.
     *
     * @param id    The id of the group Item
     *
     * @return      The group Item
     */
    public Item getGroup(Long id);

    public Item getGroupByGroupId(Item parent, String groupId);

    public List<Item> getAllGroups(Item parent, int start, int limit, String dataType, boolean ascending);
}

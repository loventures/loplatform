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

package com.learningobjects.cpxp.service.user;

import javax.annotation.Nullable;
import javax.ejb.Local;

import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.query.QueryBuilder;
import java.util.Collection;

import java.util.List;

@Local
public interface UserService {
    public Item get(Long id);

    @Nullable
    public Item getByUserName(Item parent, String userName);

    public Item getUserFolder();

    public Item getUserFolder(Long domainId);
}

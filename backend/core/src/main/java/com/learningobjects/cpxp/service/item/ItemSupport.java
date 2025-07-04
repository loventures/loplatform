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

package com.learningobjects.cpxp.service.item;

import com.learningobjects.cpxp.IdType;
import com.learningobjects.cpxp.service.ServiceContext;

import java.util.List;

@Deprecated // be explicit about your db access
public abstract class ItemSupport {
    public static Item get(final Long id) {
        return get(id, null);
    }

    public static Item get(final IdType item) {
        return (item == null) ? null
            : (item instanceof Item) ? ((Item) item)
            : get(item.getId(), item.getItemType());
    }

    public static Item get(Long id, String type) {
        return (id == null) ? null
            : ServiceContext.getContext().getItemService().get(id, type);
    }

    public static List<Item> get(Iterable<Long> ids, String type) {
        return ServiceContext.getContext().getItemService().get(ids, type);
    }
}

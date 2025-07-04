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

package com.learningobjects.cpxp.entity;

import com.learningobjects.cpxp.entity.annotation.ItemType;

import java.util.Optional;

public class EntityUtils {
    public static String getItemType(Class<?> clas) {
        Optional<ItemType> itemDef = Optional.ofNullable(clas.getAnnotation(ItemType.class));
        return itemDef.map(ItemType::value).orElseGet(() -> inferItemType(clas));
    }

    private static String inferItemType(Class<?> clas) {
        final String name = clas.getSimpleName();
        return (name.endsWith("Finder") || name.endsWith("Entity"))
                ? name.substring(0, name.length() - 6) : name;
    }
}

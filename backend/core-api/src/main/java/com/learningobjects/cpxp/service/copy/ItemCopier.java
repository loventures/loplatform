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

package com.learningobjects.cpxp.service.copy;

import com.learningobjects.cpxp.service.data.Data;
import com.learningobjects.cpxp.service.item.Item;

/**
 *
 * Defines several methods for copying an Item.
 *
 */
public interface ItemCopier {

    /**
     * Define a URL prefix to replace in the destination item.
     *
     * @param to
     *            the source prefix
     * @param from
     *            the destination prefix
     */
    public void setURLMapping(String to, String from);

    /**
     * Copy an item to a new child on the given item.
     *
     * @param destParent
     *            the parent
     *
     * @return the new item
     */
    public Item copyInto(Item destParent);

    /**
     * Copy over an existing item, preserving data in the existing item if there
     * is a conflict.
     *
     * @param destItem
     *            the item to copy over
     */
    public void copyOver(Item destItem);

    /**
     * Overwrite an existing item, clobbering data in the existing item if there
     * is a conflict.
     *
     * @param destItem
     *            the item to overwrite
     */
    public void overwrite(Item destItem);

    /**
     * Replace an existing item. Replaces all data with data from the source and
     * removes any data that does not exist in the source.
     *
     * @param destItem
     *            the item to replace
     */
    public void replace(Item destItem);

    /**
     *
     * @param owner
     *            the proposed owner of the copied data
     * @param data
     *            the data to filter
     * @return null if the data should not be copied. Otherwise, a filtered
     *         version of the data.
     */
    public abstract Data filter(Item owner, Data data);

    /**
     *
     * @param item
     *            the item to filter
     * @return null if the item should not be copied. Otherwise, a filtered
     *         version of the item.
     */
    public abstract Object filter(Item item);

}

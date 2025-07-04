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

package com.learningobjects.cpxp.service.data;

import java.util.Date;
import java.util.Set;

import javax.ejb.Local;
import jakarta.persistence.NonUniqueResultException;

import com.learningobjects.cpxp.service.item.Item;

/**
 * The data service.
 */
@Local
public interface DataService {
    /**
     * Create a new string data.
     *
     * @param owner the owner item
     * @param type the data type
     * @param string the string value
     *
     * @return the new data
     */
    public void createString(Item owner, String type, String string);

    /**
     * Add a new time data.
     *
     * @param owner the owner item
     * @param type the data type
     * @param time the time value
     */
    public void createTime(Item owner, String type, Date time);

    /**
     * Add a new boolean data.
     *
     * @param owner the owner item
     * @param type the data type
     * @param bool the boolean value
     */
    public void createBoolean(Item owner, String type, Boolean bool);

    /**
     * Add a new number data.
     *
     * @param owner the owner item
     * @param type the data type
     * @param number the number value
     */
    public void createNumber(Item owner, String type, Long number);

    /**
     * Create a new text data.
     *
     * @param owner the owner item
     * @param type the data type
     * @param text the text value
     *
     * @return the new data
     */
    public void createText(Item owner, String type, String text);

    /**
     * Add a new item data.
     *
     * @param owner the owner item
     * @param type the data type
     * @param item the item value
     */
    public void createItem(Item owner, String type, Item item);

    /**
     * Remove data from the database.
     *
     * @param data the data
     */
    public void remove(Data data);

    /**
     *
     * @param owner
     * @param data
     * @return
     */
    public void copy(Item owner, Data data);

    /**
     * Remove data of a particular type from an item.
     *
     * @param owner the owner item
     * @param type the data type
     */
    public void clear(Item owner, String type);

    /**
     * Remove all data from an item.
     *
     * @param owner the owner item
     */
    public void clear(Item owner);

    /**
     * Set a text value. Any existing value will be replaced.
     *
     * @param owner  the owner item
     * @param type   the data type
     * @param value the text value
     *
     * @throws NonUniqueResultException if there is more than one value already
     */
    public void setText(Item owner, String type, String value);

    /**
     * Set a string value. Any existing value will be replaced.
     *
     * @param owner  the owner item
     * @param type   the data type
     * @param value the string value
     *
     * @throws NonUniqueResultException if there is more than one value already
     */
    public void setString(Item owner, String type, String value);

    /**
     * Set a number value. Any existing value will be replaced.
     *
     * @param owner   the owner item
     * @param type    the data type
     * @param value   the number value
     *
     * @throws NonUniqueResultException if there is more than one value already
     */
    public void setNumber(Item owner, String type, Long value);

    /**
     * Set a date value. Any existing value will be replaced.
     *
     * @param owner   the owner item
     * @param type    the data type
     * @param value   the date value
     *
     * @throws NonUniqueResultException if there is more than one value already
     */
    public void setTime(Item owner, String type, Date value);

    /**
     * Set a boolean value. Any existing value will be replaced.
     *
     * @param owner   the owner item
     * @param type    the data type
     * @param value   the boolean value
     *
     * @throws NonUniqueResultException if there is more than one value already
     */
    public void setBoolean(Item owner, String type, Boolean value);

    /**
     * Set an item value. Any existing value will be replaced.
     *
     * @param owner   the owner item
     * @param type    the data type
     * @param value   the item value
     *
     * @throws NonUniqueResultException if there is more than one value already
     */
    public void setItem(Item owner, String type, Item value);

    /**
     * Set a collection of data on an item. The data can be detached from
     * the persistence context; they are copied into the item. For every
     * data type in the collection, all data of that type in the item are
     * replaced with the new values. If a datum has a null value it is not
     * written to the item, but any existing data of that type are removed.
     *
     * This method operates on the data collection of the item.
     *
     * @param item the item to update
     * @param data the data
     */
    public void setData(Item item, Iterable<Data> data);

    public void setType(Item owner, String type, Object value);

    /**
     * #INTERNAL# Normalize all newly normalized data in the system.
     */
    public void normalize(String itemType);

    public void normalize(String itemType, Set<String> dataTypes);
}

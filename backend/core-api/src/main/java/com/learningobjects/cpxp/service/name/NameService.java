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

package com.learningobjects.cpxp.service.name;

import javax.ejb.Local;

import com.learningobjects.cpxp.service.data.DataTypes;
import com.learningobjects.cpxp.service.item.Item;

/**
 * The name service. Provides facilities for binding items to paths.
 */
@Local
public interface NameService {
    /** A name service path binding. */
    public static final String DATA_TYPE_PATH = DataTypes.DATA_TYPE_URL;

    /**
     * Look up the Item at the specified absolute path.
     *
     * @param path the path
     *
     * @return the corresponding item, or null
     */
    public Item getItem(String path);
    public Item getItem(Item domain, String path);

    /**
     * Look up the Item id at the specified absolute path.
     *
     * @param path the path
     *
     * @return the corresponding item id, or null
     */
    public Long getItemId(String path);

    /**
     * Look up the item id at the specified absolute path in the specified domain.
     *
     * @param domain the domain
     * @param path the path
     *
     * @return the corresponding item id, or null
     */
    public Long getItemId(Long domain, String path);

    /**
     * Get the path binding of an item.
     *
     * @param item the item
     *
     * @return the binding, or null
     */
    public String getPath(Item item);

    /**
     * Get the path binding of an item.
     *
     * @param id the item id
     *
     * @return the binding, or null
     */
    public String getPath(Long id); // TODO: web svc

    /**
     * Set the binding of an item. Replaces any existing binding.
     *
     * @param item the item
     * @param path the path binding
     *
     * @throws Exception if the binding is already in use
     */
    public void setBinding(Item item, String path);
    public void setBinding(Long itemId, String path);

    /**
     * Set the binding of an item based on a pattern.
     * The pattern should contain a token of the
     * form $..%..$ or %. If the binding without this token is in use then the
     * token contents will be added, with the character % replaced by an
     * incrementing index. For example, "Binding$ (%)$" will evaluate as
     * "Binding", "Binding (2)", "Binding (3)" etc. On the other hand,
     * "Binding (%)" will evaluate as "Binding (1)", "Binding (2)", etc.
     *
     * @param item    the item
     * @param pattern the pattern
     *
     * @return the binding
     */
    public String setBindingPattern(Item item, String pattern);

    public String setBindingPattern(Long itemId, String pattern);

    public String getBindingPattern(Item item, String pattern);

    public String getBindingPattern(Long itemId, String pattern);
    /**
     * Link one item to another by a name binding. This creates a new link
     * child item of the parent and adds a link by that link to the
     * target.
     *
     * @param from the item from which to link
     * @param to   the item to which to link
     * @param binding the binding of the link
     *
     * @throws Exception if the binding is already in use
     *
    public void addLink(Item from, Item to, String binding);
    */

    /**
     * Unlink one item from another. Removing a missing link is silently
     * ignored.
     *
     * @param from the item from which to unlink
     * @param binding the link binding
     *
    public void removeLink(Item from, String binding);
    */

    /**
     * Get the link bindings between two items.
     *
     * @param from the item from which to look up the binding
     * @param to the item to which to look up the binding
     *
     * @return the bindings, or an empty collection
     *
    public Collection<String> getLinkBindings(Item from, Item to);
    */
}

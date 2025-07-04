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

package com.learningobjects.cpxp.service.trash;

import java.util.List;

import javax.ejb.Local;

import com.learningobjects.cpxp.service.item.Item;

@Local
public interface TrashService {

    /**
     * Begins a trash operation. Follow this up with calls to trashMore or itemService.setDeleted.
     * @param id the parent id
     * @return the trash id
     */
    public String begin(Long id);

    /**
     * Trashes item (along with its children) associated with <code>id</code>
     * @param id the item id
     */
    public String trash(Long id);
    /**
     * Trash more stuff associated with the last trash operation.
     */
    public void trashMore(Long id);

    /**
     * Untrashes item (along with its children) associated with <code>trashId</code>
     * @param trashId the trash record id
     */
    public void restore(String trashId);

    /**
     * Returns a list of trashed items. This method is purely used for testing, will improve or remove entirely later.
     * @return list of trashed items
     */
    public List<Item> getTrashRecords();

    /**
     * Cleans up trashed items that older than <code>age</code> milliseconds
     * @param age maximum
     */
    public void cleanUpTrash(long age);

    /**
     * Scheduled trash cleanup task.
     */
    public void cleanUpTrash();
}

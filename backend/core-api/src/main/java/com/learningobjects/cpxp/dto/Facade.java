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

package com.learningobjects.cpxp.dto;

import com.learningobjects.cpxp.IdType;

/**
 * Super-interface for all model facades.
 */
public interface Facade extends IdType {
    /**
     * Get the id of the underlying entity.
     *
     * @return the id of the underlying entity
     */
    public Long getId();

    /**
     * Get the parent id of the underlying entity.
     *
     * @return the parent id of the underlying entity
     */
    public Long getParentId();

    /**
     * Get the root id of the underlying entity.
     *
     * @return the root id of the underlying entity
     */
    public Long getRootId();

    /**
     * Get the type of the underlying entity.
     *
     * @return the type of the underlying entity
     */
    public String getItemType();

    /**
     * Get the path of the underlying entity.
     *
     * @return the path of the underlying entity
     */
    public String getItemPath();

    public void remove();

    public void delete();

    /**
     * Invalidate any cache entries dependent on this item across the cluster.
     */
    public void invalidate();

    /**
     * Invalidate any cache entries dependent on the parent item across the cluster.
     */
    public void invalidateParent();

    public void pollute();

    public <F extends Facade> F asFacade(Class<F> facadeClass);
}

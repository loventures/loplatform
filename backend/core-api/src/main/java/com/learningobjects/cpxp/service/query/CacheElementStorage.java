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

package com.learningobjects.cpxp.service.query;

import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.List;

/**
 * Strategy for avoiding storing attached Entity instances into the query cache.
 */
interface CacheElementStorage extends Serializable {
    /**
     * Do what is necessary to restore live objects from whatever was stored
     * into the cache.
     *
     * @param entityManager
     *            useful for loading entities based on what was stored
     * @return a collection of live, ready to be used entities
     */
    List<?> resurrect(EntityManager entityManager);
}

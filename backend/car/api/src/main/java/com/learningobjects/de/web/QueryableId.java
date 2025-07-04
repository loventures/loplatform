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

package com.learningobjects.de.web;

import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.service.data.DataTypes;

/**
 * An interface for an object that should be queryable based on its id.
 */
public interface QueryableId extends Id {
    @Queryable(dataType = DataTypes.META_DATA_TYPE_ID)
    Long getId();
}

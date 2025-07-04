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

package com.learningobjects.cpxp.service.component.misc;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SqlResultSetMapping;

/**
 * A data transfer type to carry data results from the database to an AssetGraph
 */
@MappedSuperclass // so that this is picked up in entity scanning without making a table
@SqlResultSetMapping(name = "assetPathMapping", classes = {
    @ConstructorResult(targetClass = AssetPathResult.class, columns = {
        @ColumnResult(name = "id", type = Long.class),
        @ColumnResult(name = "pathSegments", type = String.class)
    })
})
public class AssetPathResult {

    private final Long id;
    private final String pathSegments;

    public AssetPathResult(Long id, String pathSegments) {
        this.id = id;
        this.pathSegments = pathSegments;
    }

    public Long getId() {
        return id;
    }

    public String getPathSegments() {
        return pathSegments;
    }
}

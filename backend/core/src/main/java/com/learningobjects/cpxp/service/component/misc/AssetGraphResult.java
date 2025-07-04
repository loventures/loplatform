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
 * A data transfer type to carry data results from the database to AssetInfoWithPath
 * objects.
 */
@MappedSuperclass // so that this is picked up in entity scanning without making a table
@SqlResultSetMapping(name = "assetGraphMapping", classes = {
  @ConstructorResult(targetClass = AssetGraphResult.class, columns = {
    @ColumnResult(name = "id", type = Long.class),
    @ColumnResult(name = "edgeData", type = String.class),
    @ColumnResult(name = "paths", type = String.class)})})
public class AssetGraphResult {

    private final Long id;

    private final String edgeData;

    // comma separated paths
    // one path's elements are slash-delimited
    // each path element is id[:relationType:groupName:position]
    private final String paths;

    public AssetGraphResult(
            final Long id, final String edgeData, final String paths) {
        this.id = id;
        this.edgeData = edgeData;
        this.paths = paths;
    }


    public Long getId() {
        return id;
    }

    public String getEdgeData() {
        return edgeData;
    }

    public String getPaths() {
        return paths;
    }
}

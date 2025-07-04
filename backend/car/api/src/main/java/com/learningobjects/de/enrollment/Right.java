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

package com.learningobjects.de.enrollment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Similar to {@link loi.cp.right.Right}, but includes internationalized strings for its name and description. Used to
 * represent {@link loi.cp.right.Right} to clients.
 *
 * TODO figure out how to use @Schema without making this a component
 */
public class Right {

    private final String id;
    private final String name;
    private final String description;
    private final List<Right> children;

    /**
     * Constructor. It is expected that name and description are internationalized values.
     */
    @JsonCreator
    public Right(@JsonProperty("id") final String id, @JsonProperty("name") final String name,
            final @JsonProperty("description") String description, @JsonProperty("children") final List<Right>
            children) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.children = children;
    }

    /**
     * An opaque string ID unique to this right in the entire system. (Ignore for the fact that it is a FQCN of
     * a {@link loi.cp.right.Right} at the moment; that won't last).
     *
     * @return the right id
     */
    @JsonProperty
    public String getId() {
        return id;
    }

    /**
     * The internationalized name of the right
     *
     * @return the right name
     */
    @JsonProperty
    public String getName() {
        return name;
    }

    /**
     * The internationalized description of the right
     *
     * @return the right description
     */
    @JsonProperty
    public String getDescription() {
        return description;
    }

    /**
     * The children rights. An empty list when a right has no children.
     * @return the children rights.
     */
    @JsonProperty
    public List<Right> getChildren() {
        return children;
    }
}

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

package com.learningobjects.cpxp.entity.annotation;

import com.learningobjects.cpxp.entity.IndexType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a functional index to be generated on an entity field.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.FIELD })
public @interface FunctionalIndex {
    /**
     * Whether to include the parent id in this index. Include this if the
     * column is only queried relative to its parent.
     */
    boolean byParent();

    /**
     * Type of functional index desired.
     *
     * @return type of index the bootstrap should create for the table and
     *         column backing this entity and property
     */
    IndexType function() default IndexType.NORMAL;

    /**
     * Whether to check for deleted being null in this index. Include this if the
     * column is only queried for non-deleted values.
     */
    boolean nonDeleted();
}

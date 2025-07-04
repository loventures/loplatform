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

import java.lang.annotation.*;

/**
 * Annotation that declares a data type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface DataTypedef {
    /*** The data type format. */
    public DataFormat value();

    /*** Is this queried globally (without item type). */
    public boolean global() default false;

    /**
     * Optional mapped name for the property if the data type name is a
     * reserved word.
     *
     * @return mapped property name
     */
    public String mappedName() default "";

    /**
     * Optional item type specification for item data.  Required if the referenced item type
     * is standalone.
     *
     * @return item type
     */
    public String itemType() default "";

    /**
     * For string types, the max string length.
     */
    public int length() default -1;

    /**
     * Whether this column is allowed to be null. False adds a NOT NULL constraint
     *
     * @return if true, then the data in this column is allowed to be null
     */
    public boolean nullable() default true;

    /**
     * Whether this data type must be stored only as entity-mapped data.
     * Implicitly true for `DataFormat.json`.
     */
    boolean entityOnly() default false;
}

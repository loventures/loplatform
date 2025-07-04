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

package com.learningobjects.cpxp.service.item;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.learningobjects.cpxp.service.data.DataMapping;
import com.learningobjects.cpxp.service.finder.Finder;
import com.learningobjects.cpxp.service.finder.ItemRelation;
import com.learningobjects.cpxp.entity.annotation.SqlIndex;

/**
 * Annotation that declares an item type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.FIELD })
public @interface ItemTypedef {
    /**
     * Optional mapping of this type's data elements to properties on a
     * generated Item finder.
     *
     * @return array of mappings, empty by default
     */
    DataMapping[] dataMappings() default {};

    /**
     * When should an {@link Item} be created when it also has a {@link Finder},
     * if at all.
     *
     * @return policy for creating Item instances
     */
    ItemRelation itemRelation() default ItemRelation.PEER;

    /**
     * Additional indexes to create on the finder.
     */
    public SqlIndex[] sqlIndexes() default {};

    /**
     * A data type specifying a friendly name to display in the storage browser.
     */
    public String friendlyName() default "";

    /**
     * Whether to use the L2 cache.
     */
    boolean l2Cache() default true;

    /**
     * Whether to forbid data-mapped data on this item type.
     * Implicitly true for items with non-PEER itemRelations.
     */
    boolean noData() default false;
}

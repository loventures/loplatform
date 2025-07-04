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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates method on facade to identify its grouping with other methods.
 *
 * E.g., getProperty, getProperty, getProperties are all part of the same group, "Properties". In general, this is inferred by the name and return type cardinality (object vs collection). If getProperties were called something non-standard, or if the framework had difficulty inferring the group name (e.g., it can't tell if properties needs to be pluralized), this annotation can be used to specifically associate this accessor with the "Properties" group.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface FacadeGroup {
    /**
     * @return the facade method group to which this method belongs, if named
     * in a non-standard way.
     */
    public String value();
}

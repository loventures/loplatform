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

package com.learningobjects.cpxp.component.query;

import com.learningobjects.cpxp.component.annotation.ItemMapping;

import java.lang.annotation.Annotation;

/**
 * Synthetic item mapping annotation.
 */
public class ItemMappingAnnotation implements ItemMapping {
    private final String value, dataType;
    private final boolean singleton, schemaMapped;

    public ItemMappingAnnotation(final String value, final String dataType,
       final boolean singleton, final boolean schemaMapped) {
        this.value = value;
        this.dataType = dataType;
        this.singleton = singleton;
        this.schemaMapped = schemaMapped;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public String dataType() {
        return dataType;
    }

    @Override
    public boolean singleton() {
        return singleton;
    }

    @Override
    public boolean schemaMapped() {
        return schemaMapped;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return ItemMapping.class;
    }

    public static ItemMappingAnnotation DEFAULT = new ItemMappingAnnotation("", "", false, false);
}

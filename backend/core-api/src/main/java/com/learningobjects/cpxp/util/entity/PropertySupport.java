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

package com.learningobjects.cpxp.util.entity;

import java.beans.Introspector;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Support class that ensures naming is consistent regardless of where and how
 * properties are accessed.
 */
public class PropertySupport {
    private static final PropertySupport SINGLETON = new PropertySupport();

    private static final Set<String> FORBIDDEN;
    static {
        Set<String> keyWords = new HashSet<String>();

        // database reserved word that causes trouble
        keyWords.add("order");
        // properties used on static entities that should not appear on
        // generated entities
        keyWords.add("id");
        keyWords.add("root");
        keyWords.add("parent");
        keyWords.add("owner");
        keyWords.add("path");
        keyWords.add("del");
        keyWords.add("type"); // not reserved any more but breaks things to change
        keyWords.add("data"); // not reserved any more but breaks things to change
        keyWords.add("version"); // not reserved any more but breaks things to change

        FORBIDDEN = Collections.unmodifiableSet(keyWords);
    }

    /**
     * Restore a property name to its capitalized form.
     *
     * @param propertyName
     *            a name that may have a de-capitalized initial
     * @return return the re-captilized property name or do nothing if the first
     *         two characters are both capital already
     */
    public static String recapitalize(String propertyName) {
        return SINGLETON.recapitalizeProperty(propertyName);
    }

    /**
     * Tries to ensure that a generated property name doesn't run afoul of any
     * restricted words.
     *
     * @param dataType
     *            used to generate a name, parsing any dotted expressions if
     *            needed to help avoid naming collisions
     * @return a valid property name
     */
    public static String getPropertyName(String itemType, String dataType) {
        return SINGLETON.getValidPropertyName(itemType, dataType);
    }

    static void assertRestricted(String itemType, String dataType, String propertyName) {
        if (SINGLETON.isRestricted(propertyName)) {
            SINGLETON.assertRestrictedName(itemType, dataType, propertyName);
        }
    }

    private String recapitalizeProperty(String propertyName) {
        String initial = propertyName.substring(0, 1);

        // adhere to the contract of Introspector.decapitalize, leave a sequence
        // of capitalized characters alone
        if (initial.toUpperCase().equals(initial)) {
            return propertyName;
        }

        return initial.toUpperCase().concat(propertyName.substring(1));
    }

    private String getValidPropertyName(String itemType, String dataType) {
        final int lastDot = dataType.lastIndexOf('.');
        String propertyName = null;
        if (dataType.contains(".")) {
            propertyName = dataType.substring(lastDot + 1);
        } else {
            propertyName = dataType;
        }

        if(propertyName.equals("hashCode")) {
            propertyName = "_hashCode";
        }

        if (!isRestricted(propertyName)) {
            return propertyName;
        }

        if (lastDot == -1) {
            assertRestrictedName(itemType, dataType, propertyName);
        }

        String prefix = dataType.substring(0, lastDot);

        if (prefix.contains(".")) {
            final int prefixLastDot = prefix.lastIndexOf('.');
            prefix = prefix.substring(prefixLastDot + 1);
        }

        propertyName = Introspector.decapitalize(prefix).concat(
                recapitalize(propertyName));

        if (isRestricted(propertyName)) {
            assertRestrictedName(itemType, dataType, propertyName);
        }

        return propertyName;
    }

    private boolean isRestricted(String propertyName) {
        return FORBIDDEN.contains(propertyName);

    }

    private void assertRestrictedName(String itemType, String dataType, String propertyName) {
        throw new IllegalStateException(
                String
                        .format(
                                "Cannot use data type, %1$s, from item type, %2$s, that would result in property name, %3$s, which is a reserved or restricted term.",
                                dataType, itemType, propertyName));
    }
}

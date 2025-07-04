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

package com.learningobjects.cpxp.util;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableListMultimap.Builder;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Map.Entry;

/**
 * The things that Guava won't put in {@link Maps}. Most for good reason, should rename to
 * UnsafeMaps
 */
public class MoreMaps {

    public static <K, V> ImmutableListMultimap<K, V> multimapCopyOf(
            Map<? extends K, ? extends V> map) {

        if (map == null) {
            return null;
        }

        final Builder<K, V> builder = ImmutableListMultimap.builder();
        for (final Entry<? extends K, ? extends V> entry : map.entrySet()) {
            builder.put(entry);
        }

        return builder.build();
    }
}

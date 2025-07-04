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

package com.learningobjects.cpxp.component.web;

import com.google.common.base.Splitter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestMappingUtil {

    /* I feel so dirty */
    private static RequestMappingUtil INSTANCE;

    private static final Splitter SLASHER = Splitter.on('/');

    /**
     * A path variable path segment is a token in braces
     */
    private static final Pattern PATH_VAR_PATTERN = Pattern.compile("\\{(.*)}");


    public static RequestMappingUtil getInstance() {
        /* ewwwwww */
        if (INSTANCE == null) {
            INSTANCE = new RequestMappingUtil();
        }
        return INSTANCE;
    }

    /**
     * Creates an unmodifiable map of URI template variable names to their values. Assumes that {@code uriTemplate} and {@code path}
     * have the same number of path segments.
     *
     * @param uriTemplate URI template that specifies variable names
     * @param uriPathSegments        path segments to apply to the URI template
     * @return a map of URI template variable names to values.
     * @throws java.util.NoSuchElementException
     *          if {@code path} has fewer path segments than {@code uriTemplate}
     */
    public Map<String, String> applyPathToUriTemplate(final String uriTemplate, final List<UriPathSegment> uriPathSegments) {

        if (uriPathSegments.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<String, String> uriTemplateVariables = new HashMap<>();
        Iterator<UriPathSegment> pathSegments = uriPathSegments.iterator();

        for (final String uriTemplateSegment : SLASHER.split(uriTemplate)) {

            final String pathSegment = pathSegments.next().getSegment();

            Matcher m = PATH_VAR_PATTERN.matcher(uriTemplateSegment);
            if (m.matches()) {
                uriTemplateVariables.put(m.group(1), pathSegment);
            }
        }

        return Collections.unmodifiableMap(uriTemplateVariables);
    }
}

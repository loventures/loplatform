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

import com.learningobjects.cpxp.component.annotation.RequestMapping;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reserved matrix parameter names in SRS.
 */
public enum MatrixParameterName {

    /** value in request-URI is number of items to skip in collection responses */
    OFFSET("offset"),

    /** value in request-URI is max number of items to return in collection responses */
    LIMIT("limit"),

    /**
     * value in request-URI is an ordering to apply to collection responses, example
     * values:
     * <pre>
     * foo
     * foo:asc
     * foo:asc,bar:desc</pre>
     */
    ORDER("order"),

    /**
     * value in request-URI is a predicate, example values:
     * <pre>
     * name:eq(Bob)
     * name:eq(Bob),lastName:eq(Tables)
     * </pre>
     */
    FILTER("filter"),

    /**
     * value in request-URI is a predicate, applied before filtering, example values:
     * <pre>
     * name:eq(Bob)
     * name:eq(Bob),lastName:eq(Tables)
     * </pre>
     */
    PREFILTER("prefilter"),

    /**
     * value in request-URI is an embedding expression. An embedding expression is a
     * combination of {@link RequestMapping#path}s that are relative to the response
     * entity.
     * <pre>
     * schools
     * schools,teachers
     * schools/4
     * schools.courses
     * schools/4.courses
     * </pre>
     *
     * The dot starts a new embedding expression relative to the dot's left operand. The
     * slashes are not part of embedding grammar, the are part of the {@link
     * RequestMapping#path} being embedded. Commas separate paths to embed.
     */
    EMBED("embed"),

    /**
     * Not supported yet
     */
    FETCH("fetch"),

    /**
     * value in request-URI is the logical operator to join filter predicates, example
     * values:
     * <pre>
     * and
     * or</pre>
     * The operation applies to all predicates in the filter. Finer grained expressions
     * are not supported at this time.
     */
    FILTER_OP("filterOp"),

    /**
     * value in request-URI is the logical operator to join filter predicates, this is an
     * alias for {@link #FILTER_OP}, example values:
     * <pre>
     * AND
     * OR
     * and
     * or</pre>
     * The conjunction applies to all predicates in the filter. Finer grained conjunctions
     * are not supported at this time.
     * @deprecated use {@link #FILTER_OP}
     */
    @Deprecated
    MATCH_TYPE("matchType", FILTER_OP),;

    private static final Map<String, MatrixParameterName> NAME_INDEX =
      Arrays.stream(MatrixParameterName.values()).collect(Collectors.toUnmodifiableMap(MatrixParameterName::getName, Function.identity()));

    private final String name;

    /**
     * Present when a {@link MatrixParameterName} is just fronting for another one for
     * compatibility sake.
     */
    private final Optional<MatrixParameterName> normalized;

    private MatrixParameterName(final String name) {
        this(name, Optional.empty());
    }

    private MatrixParameterName(final String name, final MatrixParameterName normalized) {
        this(name, Optional.of(normalized));
    }

    private MatrixParameterName(final String name,
            final Optional<MatrixParameterName> normalized) {
        this.name = name;
        this.normalized = normalized;
    }

    public MatrixParameterName normalize() {
        return normalized.orElse(this);
    }

    public String getName() {
        return name;
    }

    public static MatrixParameterName byName(final String name) {
        return NAME_INDEX.get(name);
    }
}

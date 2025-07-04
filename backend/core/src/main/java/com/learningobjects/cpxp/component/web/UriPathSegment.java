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

import com.google.common.base.Function;
import com.learningobjects.cpxp.util.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * A parsed section of the path segment in a URI. For example, one of these will represent
 * the "foo" and a map of matrix parameters in the URI path "/foos;color=blue/bar". A
 * second {@link UriPathSegment} will represent the "bar" segment in that example.
 */
public class UriPathSegment {

    /** until Java 8 gets here */
    public static final Function<UriPathSegment, String> GET_SEGMENT =
            new Function<UriPathSegment, String>() {

        @Override
        public String apply(final UriPathSegment uriPathSegment) {
            return uriPathSegment.getSegment();
        }
    };

    private final String segment;

    /**
     * Non-reserved (non-system) matrix parameters in the segment
     */
    private final Map<String, String> matrixParameters;

    /**
     * System matrix parameters in the segment, aliases are normalized (e.g. matchType is
     * put under "conjunction")
     */
    private final Map<MatrixParameterName, String> systemMatrixParameters;

    /**
     * Construct a segment without any matrix parameters
     */
    public UriPathSegment(final String segment) {
        this(segment, Collections.<String, String>emptyMap(),
                Collections.<MatrixParameterName, String>emptyMap());
    }

    public UriPathSegment(final String segment,
            final Map<String, String> matrixParameters,
            final Map<MatrixParameterName, String> systemMatrixParameters) {
        this.segment = segment;
        this.matrixParameters = matrixParameters;
        this.systemMatrixParameters = systemMatrixParameters;
    }

    public UriPathSegment withSegment(final String segment) {
        return new UriPathSegment(segment, this.matrixParameters, this.systemMatrixParameters);
    }

    public String getSegment() {
        return segment;
    }

    /**
     * @return non-system-reserved matrix parameters
     */
    public Map<String, String> getMatrixParameters() {
        return matrixParameters;
    }

    /**
     * @return system-reserved matrix parameters (e.g. offset/limit)
     */
    public Map<MatrixParameterName, String> getSystemMatrixParameters() {
        return systemMatrixParameters;
    }

    @Override
    public boolean equals(final Object obj) {
        final boolean equals;

        if (this == obj) {
            equals = true;
        } else if (obj instanceof UriPathSegment) {
            final UriPathSegment that = (UriPathSegment) obj;
            equals = Objects.equals(this.segment, that.segment) &&
                    Objects.equals(this.matrixParameters, that.matrixParameters) &&
                    Objects.equals(this.systemMatrixParameters,
                            that.systemMatrixParameters);
        } else {
            equals = false;
        }

        return equals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(segment, matrixParameters, systemMatrixParameters);
    }

    @Override
    public String toString() {
        if (!matrixParameters.isEmpty()) {
            return segment + ";" + StringUtils.MATRIX_PARAM_JOINER.join(matrixParameters);
        } else {
            return segment;
        }
    }
}

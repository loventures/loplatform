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
import com.learningobjects.cpxp.component.query.ApiQuery;

/**
 * A more processed version of a {@link UriPathSegment}. System matrix parameters have
 * been processed into their model (e.g. pagination/ordering/filtering params are
 * processed into an {@link ApiQuery}.
 */
public class DePathSegment {

    public static final Function<DePathSegment, UriPathSegment> GET_URI_PATH_SEGMENT =
            new Function<DePathSegment, UriPathSegment>() {
                @Override
                public UriPathSegment apply(final DePathSegment dePathSegment) {
                    return dePathSegment.getUriPathSegment();
                }
            };

    private final UriPathSegment uriPathSegment;

    /**
     * When a segment doesn't have any filtering/pagination/ordering, then this is
     * effectively {@link ApiQuery#ALL}
     */
    private final ApiQuery apiQuery;

    public DePathSegment(final UriPathSegment uriPathSegment) {
        this(uriPathSegment, ApiQuery.ALL);
    }

    public DePathSegment(
            final UriPathSegment uriPathSegment, final ApiQuery apiQuery) {
        this.uriPathSegment = uriPathSegment;
        this.apiQuery = apiQuery;
    }

    public UriPathSegment getUriPathSegment() {
        return uriPathSegment;
    }

    public ApiQuery getApiQuery() {
        return apiQuery;
    }

    public String getEmbedPath() {
        return uriPathSegment.getSystemMatrixParameters().get(MatrixParameterName.EMBED);
    }

    public String toString() {

        return uriPathSegment.getSegment();
    }

}

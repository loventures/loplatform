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

import com.learningobjects.cpxp.component.annotation.MaxLimit;

public interface ApiPage {

    /**
     * @deprecated use {@link #isUnboundedLimit}
     */
    @Deprecated
    int UNBOUNDED_LIMIT = -1;

    /**
     * Return offset, 0 if not specified on request.
     */
    int getOffset();

    /**
     * Return limit, -1 means unbounded (-1 if not specified on request and none is supplied in a {@link MaxLimit}
     * annotation).
     */
    int getLimit();

    /**
     * @return limit, or defaultValue if limit is unbounded
     */
    int getLimitOr(final int defaultValue);

    /**
     * Return whether a page offset or limit is set.
     */
    boolean isSet();

    /**
     * Throws a validation error declaring this page unsupported if either offset or
     * limit is specified.
     */
    void unsupported();

    boolean isUnboundedLimit();

    /**
     * Use the limit of this page to get a new ApiPage representing the next page
     */
    ApiPage next();

    @Override
    String toString();

}

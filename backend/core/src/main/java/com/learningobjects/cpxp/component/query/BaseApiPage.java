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

import com.google.common.base.Objects;
import com.learningobjects.cpxp.component.annotation.MaxLimit;
import com.learningobjects.cpxp.service.exception.ValidationException;

public class BaseApiPage implements ApiPage {

    public static final ApiPage ALL = new BaseApiPage(0, UNBOUNDED_LIMIT);

    public static final ApiPage DEFAULT_PAGE = ALL;

    /**
     * Offset, 0 if not specified on request.
     */
    private final int offset;

    /**
     * limit, -1 means unbounded (-1 if not specified on request and none is supplied in a {@link MaxLimit}
     * annotation).
     */
    private final int limit;

    public BaseApiPage(final int offset, final int limit) {
        this.offset = offset;
        this.limit = limit;
    }

    /**
     * Return offset, 0 if not specified on request.
     */
    @Override
    public int getOffset() {
        return offset;
    }

    /**
     * Return limit, -1 means unbounded (-1 if not specified on request and none is supplied in a {@link MaxLimit}
     * annotation).
     */
    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public int getLimitOr(final int defaultValue) {
        return isUnboundedLimit() ? defaultValue : getLimit();
    }

    /**
     * Return whether a page offset or limit is set.
     */
    @Override
    public boolean isSet() {
        return (offset > 0) || (limit >= 0);
    }

    @Override
    public boolean isUnboundedLimit() {
        return limit == UNBOUNDED_LIMIT;
    }

    /**
     * Throws a validation error declaring this page unsupported if either offset or
     * limit is specified.
     */
    @Override
    public void unsupported() throws ValidationException {
        if (offset > 0) {
            throw new ValidationException("offset", String.valueOf(offset), "Unsupported offset");
        } else if (limit >= 0) {
            throw new ValidationException("limit", String.valueOf(limit), "Unsupported limit");
        }
    }

    @Override
    public ApiPage next() {
        return new BaseApiPage(offset + limit, limit);
    }

    @Override
    public String toString() {
        return "offset=" + offset + ", limit=" + limit;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.limit, this.offset);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof BaseApiPage)) {
            return false;
        }
        BaseApiPage page = (BaseApiPage) obj;
        return this.limit == page.limit && this.offset == page.offset;
    }
}

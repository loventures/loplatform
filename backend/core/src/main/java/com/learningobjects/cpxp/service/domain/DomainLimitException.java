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

package com.learningobjects.cpxp.service.domain;

import com.learningobjects.cpxp.service.ServiceException;

public class DomainLimitException extends ServiceException {
    private Limit _limit;
    private long _value;
    private long _max;

    public DomainLimitException(Limit limit, long value, long max) {
        super("Domain limit " + limit + " exceeded: " + value + " / " + max, limit.toString(), new Object[] { value, max });
        _limit = limit;
        _value = value;
        _max = max;
    }

    public Limit getLimit() {
        return _limit;
    }

    public long getvalue() {
        return _value;
    }

    public long getMax() {
        return _max;
    }

    public static enum Limit {
        /** Max users in the domain. */
        userLimit,
        /** Max groups in the domain. */
        groupLimit,
        /** Max enrollments in a group. */
        memberLimit,
        /** Max enrollments in the domain. */
        enrollmentLimit,
        /** Max sessions. */
        sessionLimit;
    }
}

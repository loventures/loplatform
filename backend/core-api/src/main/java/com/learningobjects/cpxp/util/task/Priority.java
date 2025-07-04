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

package com.learningobjects.cpxp.util.task;

import com.learningobjects.cpxp.util.DateUtils;

public enum Priority {
    /** System priority level is for system-level tasks. */
    System(1f, DateUtils.Unit.week.getValue(1)),
    /** Interative priority level means that a human is blocked on the completion of
     * this task; e.g. network login. */
    Interactive(1f, DateUtils.Unit.second.getValue(5)),
    /** High priority level means that a human is aware of this task; e.g. profile image scaling,
     * roster sync. */
    High(.75f, DateUtils.Unit.second.getValue(15)),
    /** Low priority level is for all bulk tasks. */
    Low(.5f, DateUtils.Unit.minute.getValue(10)),
    /** Index priority is for search index ops. */
    Index(.375f, DateUtils.Unit.hour.getValue(1));

    private final float _utilization;
    private final long _timeout;

    Priority(float utilization, long timeout) {
        _utilization = utilization;
        _timeout = timeout;
    }

    /** Get the previous (higher) priority, or null. */
    public Priority higher() {
        int index = ordinal() - 1;
        return (index < 0) ? null : values()[index];
    }

    /** Get the next (lower) priority, or null. */
    public Priority lower() {
        int index = ordinal() + 1;
        return (index >= values().length) ? null : values()[index];
    }

    /** The maximum thread utilizitaion of this priority level and lower. */
    public float getUtilization() {
        return _utilization;
    }

    /** Maximum expected duration of a task at this prioriy, for diagnostic purposes. */
    public long getTimeout() {
        return _timeout;
    }
}

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

package com.learningobjects.de.task;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.EnumSet;

public enum TaskReportStatus {

    WAITING, IN_PROGRESS, SUCCESS, FAILURE;

    private static final EnumSet<TaskReportStatus> COMPLETE_STATUSES = EnumSet.of(SUCCESS, FAILURE);

    @JsonIgnore
    public boolean isComplete() {
        return COMPLETE_STATUSES.contains(this);
    }

}

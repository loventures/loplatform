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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.learningobjects.de.BasicReport;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class FiniteTaskReport extends BaseTaskReport {

    private final int totalUnits;

    @JsonCreator
    public FiniteTaskReport(@JsonProperty("id") final String id,
      @JsonProperty("name") final String name,
      @JsonProperty("report") final BasicReport report,
      @JsonProperty("startTime") final Date startTime,
      @JsonProperty("endTime") final Date endTime,
      @JsonProperty("status") final TaskReportStatus status,
      @JsonProperty("complete") final boolean complete,
      @JsonProperty("completedUnits") final int completedUnits,
      @JsonProperty("children") final List<TaskReport> children,
      @JsonProperty("totalUnits") final int totalUnits) {
        super(id, name, report, startTime, endTime, status, complete, completedUnits,
          children);
        this.totalUnits = totalUnits;
    }

    public FiniteTaskReport(final String name, final int totalUnits) {
        this(null, name, null, null, null, null, false, 0, null, totalUnits);
    }

    public int getTotalUnits() {
        return totalUnits;
    }

    @Override
    public TaskReport copy() {

        final FiniteTaskReport copy =
          new FiniteTaskReport(id, name, report, startTime, endTime, status, complete,
            completedUnits, null, totalUnits);

        for (final TaskReport child : children) {
            copy.addChild(child.copy());
        }

        return copy;
    }

    @Override
    public boolean equals(final Object obj) {
        final boolean equals;
        if (this == obj) {
            equals = true;
        } else if (obj instanceof FiniteTaskReport) {
            final FiniteTaskReport that = (FiniteTaskReport) obj;
            equals = super.equals(obj) && Objects.equals(totalUnits, that.totalUnits);
        } else {
            equals = false;
        }
        return equals;
    }

    // be wary... these objects have mutable properties
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), totalUnits);
    }
}

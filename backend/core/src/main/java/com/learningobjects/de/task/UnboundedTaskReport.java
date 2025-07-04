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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.learningobjects.de.BasicReport;

import java.util.Date;
import java.util.List;

public class UnboundedTaskReport extends BaseTaskReport {

    public UnboundedTaskReport(@JsonProperty("id") final String id,
      @JsonProperty("name") final String name,
      @JsonProperty("report") final BasicReport report,
      @JsonProperty("startTime") final Date startTime,
      @JsonProperty("endTime") final Date endTime,
      @JsonProperty("status") final TaskReportStatus status,
      @JsonProperty("complete") final boolean complete,
      @JsonProperty("completedUnits") final int completedUnits,
      @JsonProperty("children") final List<TaskReport> children) {
        super(id, name, report, startTime, endTime, status, complete, completedUnits,
          children);
    }

    public UnboundedTaskReport(final String name) {
        this(null, name, null, null, null, null, false, 0, null);
    }

    @Override
    public TaskReport copy() {

        final UnboundedTaskReport copy =
          new UnboundedTaskReport(id, name, report, startTime, endTime, status, complete,
            completedUnits, null);

        for (final TaskReport child : children) {
            copy.addChild(child.copy());
        }

        return copy;
    }

    @Override
    public boolean equals(final Object obj) {
        return super.equals(obj) && obj instanceof UnboundedTaskReport;
    }

}

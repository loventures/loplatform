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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.learningobjects.de.BasicReport;
import loi.cp.i18n.BundleMessage;

import java.util.Date;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(name = "unbounded", value = UnboundedTaskReport.class),
  @JsonSubTypes.Type(name = "finite", value = FiniteTaskReport.class)})
public interface TaskReport {

    String getId();

    String getName();

    BasicReport getReport();

    Date getStartTime();

    Date getEndTime();

    /**
     * Returns the status of this report. Folds the status of this report and all of its
     * children (if any child failed then this failed). This is the only field that
     * folds with the corresponding data of its children reports. This was done for
     * backward compatibility with users of the old TaskReport.
     *
     * @return the status of this report
     */
    TaskReportStatus getStatus();

    boolean isComplete();

    int getCompletedUnits();

    List<TaskReport> getChildren();

    /**
     * Adds a child report. Marks the child as started. This is to maintain
     * backward compatibility with users of the old TaskReport. Also every use
     * was starting it immediately anyway.
     */
    void addChild(final TaskReport child);

    void markStart(final Date startTime);

    void markProgress(int progressUnits);

    void markComplete(final Date endTime);

    /**
     * Adds an unbounded child report. Marks the child as started. This is to maintain
     * backward compatibility with users of the old TaskReport. Also every use
     * was starting it immediately anyway.
     */
    UnboundedTaskReport addChild(final String name);

    /**
     * Adds a finite child report. Marks the child as started. This is to maintain
     * backward compatibility with users of the old TaskReport. Also every use
     * was starting it immediately anyway.
     */
    FiniteTaskReport addChild(final String name, final int totalUnits);

    void addInfo(final BundleMessage info);

    void addWarning(final BundleMessage warning);

    void addError(final BundleMessage error); //XXX: Probably should not be a BundleMessage

    void addAll(BasicReport report);

    void markStart();

    void markProgress();

    void markComplete();

    @JsonIgnore
    boolean isSuccess();

    boolean hasErrors();

    TaskReport copy();

}

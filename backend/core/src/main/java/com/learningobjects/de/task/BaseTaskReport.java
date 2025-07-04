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

import com.learningobjects.cpxp.component.web.util.JacksonUtils;
import com.learningobjects.de.BasicReport;
import loi.cp.i18n.BundleMessage;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class BaseTaskReport implements TaskReport {

    protected final String id;

    protected final String name;

    protected final BasicReport report;

    protected Date startTime;

    protected Date endTime;

    protected TaskReportStatus status;

    protected boolean complete;

    protected int completedUnits;

    protected final List<TaskReport> children;

    public BaseTaskReport(final String id, final String name, final BasicReport report,
      final Date startTime, final Date endTime, final TaskReportStatus status,
      final boolean complete, final int completedUnits, final List<TaskReport> children) {

        this.id = Optional.ofNullable(id).orElse(UUID.randomUUID().toString());
        this.name = name;
        this.report = Optional.ofNullable(report).orElse(BasicReport.empty());
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = Optional.ofNullable(status).orElse(TaskReportStatus.WAITING);
        this.complete = complete;
        this.completedUnits = completedUnits;

        // these objects are used concurrently. Jackson may be serializing for one HTTP
        // client while a deferred operation adds a new child. Traversals are much more
        // common that adds. Nobody removes children.
        this.children =
          new CopyOnWriteArrayList<>(Optional.ofNullable(children).orElse(new ArrayList<TaskReport>()));
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public BasicReport getReport() {
        return report;
    }

    @Override
    public Date getStartTime() {
        return startTime;
    }

    @Override
    public Date getEndTime() {
        return endTime;
    }

    @Override
    public TaskReportStatus getStatus() {
        return status;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public int getCompletedUnits() {
        return completedUnits;
    }

    @Override
    public List<TaskReport> getChildren() {
        return children;
    }

    @Override
    public void addChild(final TaskReport child) {
        children.add(child);
    }

    @Override
    public UnboundedTaskReport addChild(final String name) {
        final UnboundedTaskReport child = new UnboundedTaskReport(name);
        child.markStart();
        addChild(child);
        return child;
    }

    @Override
    public FiniteTaskReport addChild(final String name, int totalUnits) {
        final FiniteTaskReport child = new FiniteTaskReport(name, totalUnits);
        child.markStart();
        addChild(child);
        return child;
    }

    @Override
    public void addInfo(final BundleMessage info) {
        getReport().addInfo(info);
    }

    @Override
    public void addWarning(final BundleMessage warning) {
        getReport().addWarning(warning);
    }

    @Override
    public void addError(final BundleMessage error) {
        getReport().addError(error);
    }

    @Override
    public void addAll(BasicReport report) {
        getReport().addAll(report);
    }

    @Override
    public boolean isSuccess() {
        return getStatus() == TaskReportStatus.SUCCESS;
    }

    @Override
    public boolean hasErrors() {
        return getReport().hasErrors() || getChildren().stream()
                                                       .anyMatch(TaskReport::hasErrors);
    }

    @Override
    public void markStart() {
        markStart(new Date());
    }

    @Override
    public void markStart(final Date startTime) {
        this.status = TaskReportStatus.IN_PROGRESS;
        this.startTime = startTime;
    }

    @Override
    public void markProgress() {
        markProgress(1);
    }

    @Override
    public void markProgress(int progressUnits) {
        completedUnits += progressUnits;
    }

    @Override
    public void markComplete() {
        markComplete(new Date());
    }

    @Override
    public void markComplete(final Date endTime) {
        this.endTime = endTime;
        complete = true;
        status = hasErrors() ? TaskReportStatus.FAILURE : TaskReportStatus.SUCCESS;
    }

    @Override
    public boolean equals(final Object obj) {
        final boolean equals;
        if (this == obj) {
            equals = true;
        } else if (obj instanceof BaseTaskReport) {
            final BaseTaskReport that = (BaseTaskReport) obj;
            equals = Objects.equals(complete, that.complete) &&
              Objects.equals(completedUnits, that.completedUnits) &&
              Objects.equals(id, that.id) &&
              Objects.equals(name, that.name) &&
              Objects.equals(report, that.report) &&
              Objects.equals(startTime, that.startTime) &&
              Objects.equals(endTime, that.endTime) &&
              Objects.equals(status, that.status) &&
              Objects.equals(children, that.children);
        } else {
            equals = false;
        }
        return equals;
    }

    // be wary... these objects have mutable properties
    @Override
    public int hashCode() {
        return Objects
          .hash(id, name, report, startTime, endTime, status, complete, completedUnits,
            children);
    }

    @Override
    public String toString() {
        return JacksonUtils.getMapper().valueToTree(this).toString();
    }
}

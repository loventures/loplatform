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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class TaskReportService {

    private static final Cache<String, TaskReport> taskReports =
            CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).build();

    public static void track(TaskReport taskReport) {
        taskReports.put(taskReport.getId(), taskReport);
    }

    public static Optional<TaskReport> get(String identifier) {
        return Optional.ofNullable(taskReports.getIfPresent(identifier));
    }
}

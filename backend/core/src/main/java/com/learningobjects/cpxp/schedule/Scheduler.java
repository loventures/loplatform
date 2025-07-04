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

package com.learningobjects.cpxp.schedule;

import com.learningobjects.cpxp.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class Scheduler extends ManagedObject {
    private static Scheduler __scheduler;

    public static synchronized void startup() {
        getScheduler().start();
    }

    public static synchronized void shutdown() {
        if (__scheduler == null) {
            return;
        }
        __scheduler.stop();
        __scheduler = null;
    }

    public static synchronized Scheduler getScheduler() {
        if (__scheduler == null) {
            __scheduler = new Scheduler();
        }
        return __scheduler;
    }

    public static synchronized void configure(SchedulerConfig conf) {
        getScheduler().conf = conf;
    }

    public synchronized ScheduledTask schedule(Runnable runnable, String name, String defaultWhen) {
        String when = __scheduler.conf.when.getOrDefault(name,defaultWhen);
        long schedule = -1;
        int hour = -1, minute = -1;
        if (when.contains(":")) {
            hour = Integer.parseInt(StringUtils.substringBefore(when, ":"));
            minute = Integer.parseInt(StringUtils.substringAfter(when, ":"));
        } else if (!"Never".equalsIgnoreCase(when)) {
            schedule = DateUtils.parseDuration(when);
        }
        ScheduledTask task = new ScheduledTask(runnable, name, schedule, hour, minute);
        if ((schedule >= 0) || (hour >= 0)) {
            if (_executor != null) {
                task.setExecutor(_executor);
                task.schedule(true);
            } else {
                _pendingTasks.add(task);
            }
        }
        return task;
    }

    private static final int DEFAULT_POOL_SIZE = 2;

    SchedulerConfig conf;

    private List<ScheduledTask> _pendingTasks = new ArrayList<>();

    private ScheduledExecutorService _executor;

    private synchronized void start() {
        ThreadFactory threads = new ThreadFactoryBuilder()
            .threadGroup(new ThreadGroup("Scheduler"))
            .finishConfig();
        int poolSize = NumberUtils.intValue(conf.poolSize, DEFAULT_POOL_SIZE);
        _executor = Executors.newScheduledThreadPool(poolSize, threads);
        for (ScheduledTask task : _pendingTasks) {
            task.setExecutor(_executor);
            task.schedule(true);
        }
        _pendingTasks.clear();
    }

    private synchronized void stop() {
        if (_executor == null) {
            return;
        }
        _executor.shutdownNow();
        _executor = null;
    }

    public static class SchedulerConfig {
        public Integer poolSize;
        public Map<String, String> when;
    }
}

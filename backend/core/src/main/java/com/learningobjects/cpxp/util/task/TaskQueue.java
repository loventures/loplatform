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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskQueue<T extends Task>  {
    private static final Logger logger = Logger.getLogger(TaskQueue.class.getName());
    private final TreeSet<ScheduledTask<T>> _tasks = new TreeSet<>();
    private final EnumMap<Priority, RunQueue<T>> _running = new EnumMap<Priority, RunQueue<T>>(Priority.class);
    private final Map<String, Progress> _progress = new HashMap<>();
    private final Cache<String, Progress> _errors = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();
    private int _threads;

    public TaskQueue() {
        for (Priority priority : Priority.values()) {
            _running.put(priority, new RunQueue<T>(priority));
        }
    }

    public void setThreads(int threads) {
        _threads = threads;
    }

    public synchronized boolean offer(T task) {
        if (_progress.containsKey(task.getIdentifier())) {
            // This is not a cluster-safe lock for preventing concurrent task executions
            // but is somewhat functional at the appserver level.
            logger.log(Level.WARNING, "Ignoring duplicate task, {0}", task.getIdentifier());
            return false;
        }
        _tasks.add(new ScheduledTask<T>(task));
        _progress.put(task.getIdentifier(), new Progress());
        _errors.invalidate(task.getIdentifier());
        int count = 0;
        for (RunQueue<T> running : _running.values()) {
            count += running.size();
        }
        logger.log(Level.FINE, "Task queue size, {0}", _tasks.size());
        if ((_threads > 0) && (count >= _threads)) {
            logger.log(Level.WARNING, "Task queue at utilization, {0}, {1}", new Object[]{count, _tasks.size()});
        }
        notify();
        return true;
    }

    public synchronized T take() throws InterruptedException {
        do {
            Priority priority = topPriority();
            if (priority != null) {
                if (isRunnable(priority)) {
                    return takeFairly(priority);
                }
                logger.log(Level.WARNING, "Task priority utilization limit reached, {0}", priority);
            }
            wait();
        } while (true);
    }

    public synchronized boolean remove(T task) {
        Iterator<ScheduledTask<T>> tasks = _tasks.iterator();
        while (tasks.hasNext()) {
            if (tasks.next().task == task) {
                tasks.remove();
                _progress.remove(task.getIdentifier());
                return true;
            }
        }
        return false;
    }

    public synchronized int size() {
        return _tasks.size();
    }

    public synchronized StringBuilder stringify() {
        StringBuilder sb = new StringBuilder("Tasks");
        for (ScheduledTask<T> task : _tasks) {
            sb.append("\nTask: ").append(task.task.getIdentifier()).append(" (").append(task.task.getPriority()).append(")");
        }
        return sb;
    }

    private Priority topPriority() {
        return _tasks.isEmpty() ? null : _tasks.first().task.getPriority();
    }

    // Is it valid to run a task of this priority
    private boolean isRunnable(Priority priority) {
        int count = 0;
        for (Priority p = priority; p != null; p = p.lower()) {
            count += _running.get(priority).size();
        }
        return count < _threads * priority.getUtilization();
    }

    // Fairly take a task
    private T takeFairly(Priority priority) {
        ScheduledTask<T> task = null;
        int min = Integer.MAX_VALUE;
        RunQueue<T> running = _running.get(priority);
        for (ScheduledTask<T> t : _tasks) {
            if (priority.equals(t.task.getPriority())) {
                int count = running.get(t.task.getGroup()).size();
                if (count < min) {
                    min = count;
                    task = t;
                }
            }
        }
        assert task != null : "Priority level cannot be empty.";
        _tasks.remove(task);
        running.put(task.task.getGroup(), task);
        Progress progress = _progress.get(task.task.getIdentifier());
        if (progress != null) {
            progress.start();
        }
        return task.task;
    }

    public synchronized Progress getTaskProgress(String identifier) {
        Progress progress = _errors.getIfPresent(identifier);
        if (progress != null) {
            _errors.invalidate(identifier);
        } else {
            progress =  _progress.get(identifier);
        }
        return progress;
    }

    public void taskFailed(T task, Throwable th) {
        Progress progress = _progress.get(task.getIdentifier());
        if (progress != null) {
            progress.setError(th);
            _errors.put(task.getIdentifier(), progress);
        }
    }

    public synchronized void complete(T task) {
        RunQueue<T> running = _running.get(task.getPriority());
        running.remove(task);
        _progress.remove(task.getIdentifier());
        notify();
    }

    public synchronized void resetStatistics() {
        for (RunQueue<T> running : _running.values()) {
            running.resetStatistics();
        }
    }

    public synchronized void logStatistics() {
        for (RunQueue<T> running : _running.values()) {
            running.logStatistics();
        }
    }

    public synchronized void getStatus(String prefix, Map<String, Object> status) {
        status.put(prefix + "QueueSize", _tasks.size());
        for (Map.Entry<Priority, RunQueue<T>> entry : _running.entrySet()) {
            entry.getValue().getStatus(prefix + entry.getKey() + ".", status);
        }
    }
}

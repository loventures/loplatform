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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.learningobjects.cpxp.util.DateUtils;
import com.learningobjects.cpxp.util.KahanSummation;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

class RunQueue<T extends Task>  {
    private static final Logger logger = Logger.getLogger(RunQueue.class.getName());
    private final Priority _priority;
    private int _count, _peak;
    private final KahanSummation _latency = new KahanSummation();
    private final KahanSummation _runtime = new KahanSummation();
    private final Multimap<Object, ScheduledTask<T>> _tasks = ArrayListMultimap.create();

    public RunQueue(Priority priority) {
        _priority = priority;
    }

    public boolean put(Object key, ScheduledTask<T> task) {
        boolean value = _tasks.put(key, task);
        long now = System.currentTimeMillis();
        task.started = now;
        ++ _count;
        _latency.add(now - task.created);
        int size = _tasks.size();
        if (size > _peak) {
            _peak = size;
        }
        return value;
    }

    public Collection<ScheduledTask<T>> get(Object key) {
        return _tasks.get(key);
    }

    public void remove(T task) {
        Iterator<ScheduledTask<T>> running = _tasks.get(task.getGroup()).iterator();
        while (running.hasNext()) {
            ScheduledTask<T> t = running.next();
            if (task == t.task) {
                long runtime = System.currentTimeMillis() - t.started;
                _runtime.add(runtime);
                running.remove();
                if (runtime > _priority.getTimeout()) {
                    logger.log(Level.WARNING, "Task exceeded priority runtime, {0}, {1}, {2}", new Object[]{task.getIdentifier(), _priority, DateUtils.formatDuration(runtime)});
                }
            }
        }
    }

    public int size() {
        return _tasks.size();
    }

    public void resetStatistics() {
        _count = _peak = 0;
        _latency.reset();
        _runtime.reset();
    }

    public void logStatistics() {
        // return _priority + ": " +
        logger.log(Level.INFO, "{0} : {1} tasks (peak {2}), average latency {3}, average runtime {4}", new Object[]{_priority, _count, _peak, DateUtils.formatDuration((long) (_latency.value() / _count)), DateUtils.formatDuration((long) (_runtime.value() / _count))});
    }

    public synchronized void getStatus(String prefix, Map<String, Object> status) {
        status.put(prefix + "Running", _tasks.size());
        status.put(prefix + "Count", _count);
        status.put(prefix + "Peak", _peak);
        if (_count > 0) {
            status.put(prefix + "Latency", _latency.value() / _count);
            status.put(prefix + "Duration", _runtime.value() / _count);
        }
    }
}

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

import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.util.LogUtils;
import com.learningobjects.cpxp.util.ThreadLog;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledTask {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTask.class.getName());

    /** Jitter to add to timers to avoid thundering herds. */
    private static final long JITTER = 10000; // 10s

    /** Minimum timer delay. */
    private static final long MINIMUM = 2000; // 2s

    private final Runnable _runnable;
    private final String _name;
    private final long _schedule;
    private final int _hour, _minute;
    private ScheduledExecutorService _executor;
    private boolean _cancelled;
    private Future<?> _future;

    ScheduledTask(Runnable runnable, String name, long schedule, int hour, int minute) {
        _runnable = runnable;
        _name = name;
        _schedule = schedule;
        _hour = hour;
        _minute = minute;
    }

    public String getName() {
        return _name;
    }

    void setExecutor(ScheduledExecutorService executor) {
        _executor = executor;
    }

    synchronized void schedule(boolean initial) {
        if (_cancelled) {
            return;
        }
        _future = _executor.schedule(new Task(), getDelay(initial), TimeUnit.MILLISECONDS);
    }

    public synchronized void cancel() {
        _cancelled = true;
        if (_future != null) {
            _future.cancel(true);
        }
    }

    private long getDelay(boolean initial) {
        long delay;
        if (_schedule < 0) {
            Calendar calendar = Calendar.getInstance();
            long now = calendar.getTime().getTime();
            if ((calendar.get(Calendar.HOUR_OF_DAY) > _hour) || ((calendar.get(Calendar.HOUR_OF_DAY) == _hour) && (calendar.get(Calendar.MINUTE) >= _minute))) {
                calendar.add(Calendar.DATE, 1);
            }
            calendar.set(Calendar.HOUR_OF_DAY, _hour);
            calendar.set(Calendar.MINUTE, _minute);
            delay = calendar.getTime().getTime() - now;
        } else if (initial) {
            // Schedule the task randomly within its scheduling period
            delay = RandomUtils.nextInt(0, (int) _schedule);
        } else {
            delay = _schedule + RandomUtils.nextInt(0, (int) JITTER) - JITTER / 2;
        }
        if (delay < MINIMUM) {
            delay = MINIMUM / 2 + RandomUtils.nextInt(0, (int) MINIMUM);
        }
        return delay;
    }

    private class Task implements Runnable {
        public void run() {
            if ((_schedule < 0) || (_schedule > 45000)) {
                LogUtils.prod(logger, "Executing scheduled task: " + _name);
            }
            ThreadLog.begin("Scheduler/" + _name);
            try {
                Current.clear();
                _runnable.run();
            } catch (Throwable th) {
                // todo: if there's an interruptedexception in the trace ignore it
                logger.warn("Scheduled task error", th);
            } finally {
                ThreadLog.end();
            }
            schedule(false);
        }
    }
}

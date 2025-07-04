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

package com.learningobjects.cpxp.util;

import com.google.common.collect.MapMaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThreadLog {
    private static final Logger logger = Logger.getLogger(ThreadLog.class.getName());

    private Map<Thread, ThreadInfo> _map = new MapMaker().weakKeys().makeMap();

    private static final ThreadLog INSTANCE = new ThreadLog();

    private ThreadLog() {
    }

    public static void begin(String name) {
        INSTANCE._map.put(Thread.currentThread(), new ThreadInfo(Thread.currentThread(), name));
    }

    public static void end() {
        INSTANCE._map.remove(Thread.currentThread());
    }

    public static void log() {
        INSTANCE.doLog();
    }

    public static List<ThreadInfo> threads() {
        return new ArrayList<ThreadInfo>(INSTANCE._map.values());
    }

    private static final long SLOW_THREAD_LOG_MS = 60000L; // 1 minute

    private StringBuilder sbLog() {
        StringBuilder sb2 = new StringBuilder();
        StringBuilder sb = new StringBuilder("Thread log:");
        for (ThreadInfo info : _map.values()) {
            if (info.getThread() == Thread.currentThread()) {
                continue;
            }
            long elapsed = info.getElapsed();
            sb.append("\n  ").append(info.getThreadName()).append(": ").append(info.getName()).append(" (").append(DateUtils.formatDuration(elapsed)).append(")");
            if (elapsed > SLOW_THREAD_LOG_MS) {
                sb2.append("\n  ").append(info.getThreadName()).append(": ").append(info.getName());
                for (StackTraceElement element : info.getThread().getStackTrace()) {
                    sb2.append(" at\n    ").append(element.getClassName()).append('.').append(element.getMethodName()).append('(')
                            .append(element.getFileName()).append(':').append(element.getLineNumber()).append(')');
                }
            }
        }
        if (sb2.length() > 0) {
            sb.append("\nSlow stack traces:").append(sb2);
        }
        return sb;
    }

    private void doLog() {
        String str = sbLog().toString();
        logger.log(str.contains("\n") ? Level.INFO : Level.FINE, str);
    }

    public static class ThreadInfo {
        private final Thread _thread;
        private final String _name;
        private final long _started;

        public ThreadInfo(Thread thread, String name) {
            _thread = thread;
            _name = name;
            _started = System.currentTimeMillis();
        }

        public Thread getThread() {
            return _thread;
        }

        public String getThreadName() {
            return "Thread<" + _thread.getId() + "> \"" + _thread.getName() + "\"" + (_thread.isAlive() ? "" : " (defunct)");
        }

        public String getName() {
            return _name;
        }

        public long getElapsed() {
            return System.currentTimeMillis() - _started;
        }
    }
}

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.util.control.ControlThrowable;

import java.util.HashSet;
import java.util.Set;

public class ThreadTerminator {
    private static final Logger logger = LoggerFactory.getLogger(ThreadTerminator.class);

    private static long __timeout = 60000;

    private static boolean __death; // flag for quick check.. has anything been scheduled for death
    private static final Set<Long> __ids = new HashSet<>();
    private static final ThreadLocal<Long> __started = new ThreadLocal<>();

    public static void configure(long timeout) {
        __timeout = timeout;
    }

    public static void register() {
        __started.set(System.currentTimeMillis());
    }

    public static void unregister() {
        __started.set(null);
    }

    public static void check() {
        if (__death) {
            synchronized (__ids) {
                boolean die = __ids.remove(Thread.currentThread().getId());
                __death = !__ids.isEmpty();
                if (die) {
                    logger.warn("Thread killed 'cos we hate it");
                    throw new Terminated();
                }
            }
        }

        Long started = __started.get();
        if (__timeout > 0 && started != null && System.currentTimeMillis() - started > __timeout) {
            logger.warn("Thread killed due to timeout after {}", System.currentTimeMillis() - started);
            throw new Terminated();
        }
    }

    public static void kill(Long id) {
        synchronized (__ids) {
            __ids.add(id);
            __death = true;
        }
    }

    public static final class Terminated extends ThreadDeath {}
}

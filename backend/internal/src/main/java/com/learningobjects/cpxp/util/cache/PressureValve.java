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

package com.learningobjects.cpxp.util.cache;

import com.learningobjects.cpxp.locache.LoCache;
import com.learningobjects.cpxp.util.DateUtils;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PressureValve  implements Runnable {
    private static final Logger logger = Logger.getLogger(PressureValve.class.getName());
    private static PressureValve __instance;

    public static synchronized void startup() {
        if (__instance != null) {
            return;
        }
        __instance = new PressureValve();
        __instance.start();
    }

    public static synchronized void shutdown() {
        if (__instance == null) {
            return;
        }
        __instance.stop();
        __instance = null;
    }

    // I maintain multiple soft references because the garbage collector
    // evicts old soft references when memory is below an amount proportional
    // to their age; if i never touch one reference it will eventually
    // just be gced, but if i touch one periodically then there'll be a
    // time when it is very new and less likely to be gced.
    private static final int N = 3;

    private final ReferenceQueue<Object> _queue = new ReferenceQueue<Object>();
    private final SoftReference<?>[] _references = new SoftReference<?>[N];
    private Thread _thread;
    private int _index = 0;

    private PressureValve() {
    }

    public void start() {
        _thread = new Thread(this, "PressureValve");
        _thread.start();
    }

    public void stop() {
        _thread.interrupt();
        _thread = null;
    }

    public void run() {
        logger.log(Level.INFO, "Pressure valve started");
        for (int i = 0; i < N; ++ i) {
            _references[i] = new SoftReference<Object>(new Object(), _queue);
        }
        try {
            do {
                Reference<?> reference = _queue.remove(DateUtils.Unit.hour.getValue(1));
                if (reference != null) {
                    depressurize();
                    while (_queue.remove(0) != null) {
                        // clear the reference queue
                    }
                    for (int i = 0; i < N; ++ i) { // reset any cleared references
                        if (_references[i].get() == null) {
                            _references[i] = new SoftReference<Object>(new Object(), _queue);
                        }
                    }

                } else { // otherwise ping the next reference
                    _references[_index ++ % N].get();
                }
            } while (_thread != null);
        } catch (InterruptedException ex) {
        }
        logger.log(Level.INFO, "Pressure valve stopped");
    }

    private void depressurize() {
        logger.log(Level.WARNING, "Depressurizing");
        LoCache.forEach(LoCache::evictAll);
    }
}

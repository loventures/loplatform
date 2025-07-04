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

package com.learningobjects.cpxp.operation;

import com.learningobjects.cpxp.util.*;
import com.learningobjects.cpxp.util.task.Priority;
import com.learningobjects.cpxp.util.task.Progress;
import com.learningobjects.cpxp.util.task.TaskQueue;
import com.learningobjects.cpxp.util.tx.TransactionCompletion;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.*;

// TODO: Just merge this into Scheduler
public class Executor extends ManagedObject {
    private static final Logger logger = LoggerFactory.getLogger(Executor.class.getName());

    private static Executor __executor;

    public static synchronized void startup() {
        getExecutor().start();
    }

    public static synchronized void shutdown() {
        if (__executor == null) {
            return;
        }
        __executor.stop();
        __executor = null;
    }

    public static synchronized Executor getExecutor() {
        if (__executor == null) {
            __executor = new Executor();
        }
        return __executor;
    }

    public static void getStatus(Map<String, Object> status) {
        getExecutor()._taskQueue.getStatus("cp.Executor.", status);
    }

    public static Progress getOperationProgress(String identifier) {
        return getExecutor()._taskQueue.getTaskProgress(identifier);
    }

    /**
     * Defer the processing of an operation. The operation will be
     * performed asynchronously in a separate context at some point
     * in the future; usually within a few seconds.
     *
     * @param operation the operation
     * @param join the number of milliseconds to synchronously wait for the
     * operation to complete, or -1
     */
    public <T> Future<T> submit(Operation<T> operation, final Priority priority, final String identifier, final long join) {
        final FutureOperation<T> future = new FutureOperation<T>(operation, priority, identifier);
        logger.debug("Deferring operation, {}, {}", future, _taskQueue.size());

        // The semantics of canceling before the tx synchronizer are bogus
        // because cancel will return false even though the future is cancelable
        // but hey.
        TransactionCompletion offerer = new TransactionCompletion() {
                public void onCommit() {
                    logger.debug("Offering, {}", future);
                    future.offered(_taskQueue);
                    if (_taskQueue.offer(future) && (join >= 0)) {
                        logger.debug("Joining operation, {}", join);
                        try {
                            future.get(join, TimeUnit.MILLISECONDS);
                        } catch (TimeoutException ignored) {
                        } catch (Exception ex) {
                            // hrm
                        }
                    }
                }
            };
        if (!EntityContext.onCompletion(offerer)) {
            offerer.onCommit();
        }
        return future;
    }


    private final TaskQueue<FutureOperation<?>> _taskQueue = new TaskQueue<FutureOperation<?>>();

    @Inject
    private Config _config;

    private ThreadPoolExecutor _executor;

    private synchronized void start() {
        int poolSize = _config.getInt("com.learningobjects.cpxp.executor.poolSize");
        _taskQueue.setThreads(poolSize);
        ThreadFactory threads = new ThreadFactoryBuilder()
            .threadGroup(new ThreadGroup("Executor"))
            .finishConfig();
        _executor = new ThreadPoolExecutor(0, 256, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), threads);
        for (int i = 0; i < poolSize; ++ i) {
            _executor.submit(new ExecutorTask(this));
        }
        logger.info("Executor pool size: "+ poolSize);
    }

    TaskQueue<FutureOperation<?>> getQueue() {
        return _taskQueue;
    }

    synchronized boolean isStopped() {
        return _executor == null;
    }

    private synchronized void stop() {
        if (_executor == null) {
            return;
        }
        // I should join the threads, but some async operations may be very slow.
        _executor.shutdownNow();
        _executor = null;
    }

    public void logStuff() {
        ThreadLog.log();
        StringBuilder sb = new StringBuilder();
        LogUtils.prod(logger, ""+ _taskQueue.stringify());
    }

}

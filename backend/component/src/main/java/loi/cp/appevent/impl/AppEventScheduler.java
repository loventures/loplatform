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

package loi.cp.appevent.impl;

import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.component.annotation.PostLoad;
import com.learningobjects.cpxp.component.annotation.PreUnload;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.operation.AbstractOperation;
import com.learningobjects.cpxp.operation.Operations;
import com.learningobjects.cpxp.operation.VoidOperation;
import com.learningobjects.cpxp.util.DateUtils;
import com.learningobjects.cpxp.util.EntityContext;
import com.learningobjects.cpxp.util.ManagedUtils;
import com.learningobjects.cpxp.util.tx.TransactionCompletion;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import org.hibernate.LockMode;
import org.hibernate.jpa.HibernateHints;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// This is all static.. It might be an idea to natively support the idea of
// singleton service objects in the system.
@Service // A service for pre/post (un)load support. Todo... hooks.
public class AppEventScheduler {
    // How often I should keep events alive in the database by poking their fired value
    public static final long KEEPALIVE_INTERVAL = DateUtils.Unit.minute.getValue(1);
    // At what point do I considered a fired event dead and in need of rescheduling
    public static final long RESURRECT_INTERVAL = DateUtils.Unit.minute.getValue(5);
    // Maximum time I'll sleep before polling the database for a scheduled appevent
    public static final long SCHEDULER_SLEEP_TIME = KEEPALIVE_INTERVAL;

    // Length of time to wait before restarting a dead thread
    public static final long THREAD_DEATH_SLEEP_TIME = DateUtils.Unit.second.getValue(15);

    private static Thread __scheduler, __executor;
    private static boolean __complete = false;
    private static final Queue<Long> __queue = new LinkedList<>();

    @PostLoad
    private static void startScheduler() {
        __scheduler = new Thread(new AppEventSchedulerTask(), "AppEventScheduler");
        __scheduler.start();
        __executor = new Thread(new AppEventExecutorTask(), "AppEventExecutor");
        __executor.start();
    }

    @PreUnload
    private static void stopScheduler() {
        __complete = true;
        if (__scheduler != null) {
            __scheduler.interrupt();
        }
        if (__executor != null) {
            __executor.interrupt();
        }
    }

    public static void eventFired(final Long id) {
        EntityContext.onCompletion(new TransactionCompletion() {
            @Override
            public void onCommit() {
                synchronized (__queue) {
                    __queue.add(id);
                    __queue.notify();
                }
            }
        });
    }

    public static void eventScheduled(Date when) {
        if (when.before(DateUtils.delta(SCHEDULER_SLEEP_TIME))) {
            EntityContext.onCompletion(new TransactionCompletion() {
                @Override
                public void onCommit() {
                    synchronized (__scheduler) {
                        __scheduler.notify(); // notify the calendar to look for a new deadline if it's schedule within our sleep time
                    }
                }
            });
        }
    }

    private static class AppEventSchedulerTask implements Runnable {
        private static final Logger logger = Logger.getLogger(AppEventSchedulerTask.class.getName());

        @Override
        public void run() {
            // This thread is responsible for pulling events out of the database
            // and for keeping events being processed by this node alive in the
            // database.
            logger.log(Level.INFO, "App event scheduler started");
            do {
                try {
                    logger.log(Level.FINE, "Loop start");
                    acquireAppEvents();
                    awaitAppEvent();
                    ManagedUtils.perform(new UpdateAppEventsOperation());
                } catch (Exception ex) {
                    if (!__complete) {
                        logger.log(Level.WARNING, "App event scheduler error", ex);
                        try {
                            Thread.sleep(THREAD_DEATH_SLEEP_TIME);
                        } catch (InterruptedException ie) {
                            // can't wait that long...
                        }
                    }
                }
            } while (!__complete);
            logger.log(Level.INFO, "App event scheduler stopped");
        }

        private void acquireAppEvents() {
            List<Long> events = ManagedUtils.perform(new AcquireAppEventsOperation());
            logger.log(Level.INFO, "Found {0} app events in database: {0}", new Object[]{events.size(), events});
            if (!events.isEmpty()) {
                synchronized (__queue) {
                    __queue.addAll(events);
                    __queue.notify();
                }
            }
        }

        private void awaitAppEvent() throws Exception {
            // find the wait time before the next database scheduled event, maximum
            // one minute, minimum zero.. Then wait that time.
            Long nextTime = ManagedUtils.perform(new GetNextAppEventTimeOperation());
            long eventWaitTime = (nextTime == null) ? Long.MAX_VALUE : nextTime - System.currentTimeMillis();
            // The min wait time is somewhat arbitrary. It should be
            // keepalive interval if I have any executing tasks. It
            // could be larger if there were no executing tasks but I'd
            // then have to explicitly wake this thread if a local
            // event was fired.. I also have to be able to pick up tasks
            // if a foreign node dies.
            long waitTime = Math.min(Math.max(1, eventWaitTime), SCHEDULER_SLEEP_TIME);
            logger.log(Level.INFO, "Awaiting app events, {0} ms", waitTime);
            synchronized (__scheduler) {
                __scheduler.wait(waitTime);
            }
        }

    }

    private static class AppEventExecutorTask implements Runnable {
        private static final Logger logger = Logger.getLogger(AppEventExecutorTask.class.getName());

        @Override
        public void run() {
            logger.log(Level.INFO, "App event executor started");
            do {
                try {
                    logger.log(Level.INFO, "Acquiring app event");
                    Long id;
                    do {
                        synchronized (__queue) {
                            if (__queue.isEmpty()) {
                                __queue.wait();
                            }
                            id = __queue.peek();
                        }
                        if (__complete) {
                            throw new InterruptedException();
                        }
                    } while (id == null);
                    logger.log(Level.INFO, "Executing app event " + id);
                    ManagedUtils.perform(Operations.asNRTransaction(new AppEventExecution(id)));
                    synchronized (__queue) {
                        __queue.remove();
                    }
                } catch (Exception ex) {
                    if (!__complete) {
                        logger.log(Level.WARNING, "App event executor error", ex);
                        // move to the back of the queue to avoid blocking other entries
                        synchronized (__queue) {
                            if (!__queue.isEmpty()) {
                                Long failingId = __queue.remove();
                                __queue.add(failingId);
                            }
                        }
                        try {
                            Thread.sleep(THREAD_DEATH_SLEEP_TIME);
                        } catch (InterruptedException ie) {
                            // meh
                        }
                    }
                } finally {
                    de.tomcat.juli.LogMeta.clear();
                }
            } while (!__complete);
            logger.log(Level.INFO, "App event executor stopped");
        }
    }

    private static final int MAX_APPEVENTS = 256;

    private static class AcquireAppEventsOperation extends AbstractOperation<List<Long>> {
        private static final Logger logger = Logger.getLogger(AcquireAppEventsOperation.class.getName());

        @Override
        @SuppressWarnings("unchecked")
        public List<Long> perform() {
            logger.log(Level.FINE, "Acquire app events");
            try {
                // Grab a chunk of app events that are not complete, that are due
                // to happen and that aren't currently considered fired (in
                // flight). Update the host to myself and the fired time to
                // now. Global order is not guaranteed across a cluster.
                List<Number> ids = ManagedUtils.getEntityContext().getEntityManager().createQuery(
                    "SELECT id FROM com.learningobjects.cpxp.service.appevent.AppEventFinder" +
                      " WHERE del IS NULL" +
                      " AND state IS NULL" +
                      " AND deadline <= :now" +
                      " AND fired < :resurrect" +
                      " ORDER BY deadline ASC, id ASC")
                  .setHint(HibernateHints.HINT_NATIVE_LOCK_MODE, LockMode.UPGRADE_SKIPLOCKED)
                  .setParameter("now", new Date())
                  .setParameter("resurrect", DateUtils.delta(new Date(), -RESURRECT_INTERVAL))
                  .setMaxResults(MAX_APPEVENTS)
                  .getResultList();

                if (ids.size() > 0) {
                    ManagedUtils.getEntityContext().getEntityManager().createQuery(
                        "UPDATE com.learningobjects.cpxp.service.appevent.AppEventFinder" +
                          " SET host = :host, fired = :now" +
                          " WHERE id IN :ids")
                      .setParameter("host", BaseServiceMeta.getServiceMeta().getLocalHost())
                      .setParameter("now", new Date())
                      .setParameter("ids", ids)
                      .executeUpdate();
                }

                logger.log(ids.isEmpty() ? Level.FINE : Level.INFO, "Acquired app events, {0}", ids);
                return ids.stream().map(Number::longValue).collect(Collectors.toList());
            } catch (org.hibernate.PessimisticLockException | jakarta.persistence.PessimisticLockException ple) {
                logger.info("Could not obtain AppEventFinder lock, no app events acquired");
                return Collections.emptyList();
            }
        }
    }

    private static class UpdateAppEventsOperation extends VoidOperation {
        private static final Logger logger = Logger.getLogger(UpdateAppEventsOperation.class.getName());

        @Override
        public void execute() {
            List<Long> ids;
            synchronized (__queue) {
                ids = new ArrayList<>(__queue);
            }
            logger.log(Level.FINE, "Update app events, {0}", ids);
            if (!ids.isEmpty()) {
                // Update the fired time of all app events that that are
                // not complete, that I own that have a fired time longer
                // ago than the keepalive interval, so that no one else
                // will steal them.
                Query query = ManagedUtils.getEntityContext().getEntityManager().createQuery(
                  "UPDATE com.learningobjects.cpxp.service.appevent.AppEventFinder" +
                    " SET fired = :now" +
                    " WHERE del IS NULL" +
                    " AND state IS NULL" +
                    " AND host = :host" +
                    " AND fired < :keepalive" +
                    " AND id IN :ids");
                query.setParameter("host", BaseServiceMeta.getServiceMeta().getLocalHost());
                Date now = new Date();
                query.setParameter("now", now);
                query.setParameter("keepalive", DateUtils.delta(now, -KEEPALIVE_INTERVAL));
                query.setParameter("ids", ids);
                int n = query.executeUpdate();
                logger.log(Level.INFO, "Updated app events, {0}", n);
            }
        }
    }

    private static class GetNextAppEventTimeOperation extends AbstractOperation<Long> {
        private static final Logger logger = Logger.getLogger(GetNextAppEventTimeOperation.class.getName());

        @Override
        public Long perform() {
            logger.log(Level.FINE, "Get next app event time");
            // Find the minimum deadline of a non complete event whose fired
            // time is far enough in the past that either the owner node is
            // dead or there was no owner.
            Query query = ManagedUtils.getEntityContext().getEntityManager().createQuery(
              "SELECT MIN(deadline) FROM com.learningobjects.cpxp.service.appevent.AppEventFinder" +
                " WHERE del IS NULL" +
                " AND state IS NULL" +
                " AND fired < :resurrect");
            Date now = new Date();
            query.setParameter("resurrect", DateUtils.delta(now, -RESURRECT_INTERVAL));
            Date deadline = (Date) query.getSingleResult();
            logger.log(Level.INFO, "Next app event time, {0}", deadline);
            return (deadline == null) ? null : deadline.getTime();
        }
    }
}

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

package com.learningobjects.cpxp.service.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.learningobjects.cpxp.service.BasicServiceBean;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionImpl;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This dispatcher is designed for use with the bulk operations of
 * {@link ItemService} and will try to defer eviction to the next commit if in a
 * transaction, otherwise evicting immediately if called outside of a
 * transaction.
 *
 * IMPROVE consider how to combine and simplify with the dispatcher used for
 * {@link BasicServiceBean} evict methods.
 */
class BulkCacheDispatcher  {
    private static final Logger logger = Logger.getLogger(BulkCacheDispatcher.class.getName());
    private static final BulkCacheDispatcher SINGLETON = new BulkCacheDispatcher();

    private static ThreadLocal<Multimap<String, Long>> __bulkEvictions = new ThreadLocal<Multimap<String, Long>>() {
        @Override
        protected Multimap<String, Long> initialValue() {
            return HashMultimap.create();
        }
    };

    private static ThreadLocal<Boolean> __registeredSynch = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    private BulkCacheDispatcher() {
        // enforce Singleton pattern
    }

    /**
     * Queue an item for eviction from the L2 cache on commit.
     *
     * @param entityManager
     *            needed to capture into the transaction callback to drive the
     *            evictions
     * @param cacheName
     *            cache from which entries should be evicted
     * @param entityIds
     *            primary keys to evict
     */
    static void enqueueItemEviction(EntityManager entityManager,
            String cacheName, Collection<Long> entityIds) {
        SessionFactory sessionFactory = getSessionFactory(entityManager);
        for (Long entityId : entityIds) {
            SINGLETON.enqueueEntityForEviction(entityManager, sessionFactory,
                    cacheName, entityId);
        }
    }

    static SessionFactory getSessionFactory(EntityManager entityManager) {
        // N.B. this is Hibernate specific but should be reasonably safe
        SessionImpl session = (SessionImpl) entityManager.getDelegate();
        if (null == session || !session.isOpen()) {
            return null;
        }
        return session.getSessionFactory();
    }

    /**
     * Fetch the current bulk evictions keyed by cache name for processing.
     *
     * @return the collection of sets of keys to be evicted from the respective
     *         L2 caches
     */
    static Multimap<String, Long> getToEvict() {
        return __bulkEvictions.get();
    }

    /**
     * Clear out the collection of entities to evict and reset the registration
     * flag, preparing thread local storage for re-use if the thread is recycled
     * and re-used later.
     */
    static void reset() {
        __bulkEvictions.get().clear();
        __registeredSynch.set(Boolean.FALSE);
    }

    private void enqueueEntityForEviction(EntityManager entityManager,
            SessionFactory sessionFactory, String name, Long entityId) {
        if (registerSynch(entityManager)) {
            __bulkEvictions.get().put(name, entityId);
            return;
        }

        if (null == sessionFactory) {
            return;
        }

        // if outside of a TX, then evict immediately
        sessionFactory.getCache().evictEntityData(name, entityId);
    }

    private boolean registerSynch(EntityManager entityManager) {
        if (__registeredSynch.get()) {
            return true;
        }
        try {
            Context context = new InitialContext();
            TransactionManager transactionManager = (TransactionManager) context
                    .lookup("java:appserver/TransactionManager");

            Transaction t = transactionManager.getTransaction();
            if (null == t || Status.STATUS_ACTIVE != t.getStatus()) {
                return false;
            }
            t.registerSynchronization(new BulkDispatchSynchronization(
                    entityManager));
            __registeredSynch.set(Boolean.TRUE);
            return true;
        } catch (Exception e) {
            logger.log(Level.FINE, "Could not register synchronization callback with transaction manager.");
            return false;
        }
    }
}

/**
 * Call back implementation to be registered as needed with the transaction
 * manager to actually process the queued L2 eviction requests.
 */
class BulkDispatchSynchronization  implements Synchronization {
    private static final Logger logger = Logger.getLogger(BulkDispatchSynchronization.class.getName());
    private final EntityManager _entityManager;

    BulkDispatchSynchronization(EntityManager entityManager) {
        _entityManager = entityManager;
    }

    /**
     * {@inheritDoc}
     *
     * Process the queued entities for L2 cache eviction on commit, clean up on
     * either commit or rollback.
     */
    @Override
    public void afterCompletion(int status) {
        if (Status.STATUS_COMMITTED == status) {
            SessionFactory sessionFactory = BulkCacheDispatcher.getSessionFactory(_entityManager);
            if (null != sessionFactory) {
                long start = System.currentTimeMillis();
                Multimap<String, Long> byCacheName = BulkCacheDispatcher.getToEvict();

                for (Entry<String, Long> entry : byCacheName.entries()) {
                    sessionFactory.getCache().evictEntityData(entry.getKey(), entry.getValue());
                }
                logger.log(Level.FINE, "Took {0} MS to evict {1} entries from L2 cache.", new Object[]{(System.currentTimeMillis() - start), byCacheName
                  .size()});
            }
        }

        if (Status.STATUS_COMMITTED == status
                || Status.STATUS_ROLLEDBACK == status) {
            BulkCacheDispatcher.reset();
        }
    }

    /**
     * {@inheritDoc}
     *
     * Nothing to do in this implementation.
     */
    @Override
    public void beforeCompletion() {
        // do nothing
    }
}

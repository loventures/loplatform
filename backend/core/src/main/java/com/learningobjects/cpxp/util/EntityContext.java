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

import com.google.common.annotations.VisibleForTesting;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.data.Data;
import com.learningobjects.cpxp.util.tx.AfterTransactionCompletionListener;
import com.learningobjects.cpxp.util.tx.BeforeTransactionCompletionListener;
import com.learningobjects.cpxp.util.tx.TransactionCompletionListener;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.proxy.HibernateProxy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EntityContext {

    private static final Logger logger = Logger.getLogger(EntityContext.class.getName());

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    // TODO: pull the thread local out of ManagedUtils and in here and ...

    public static boolean inTransaction() {
        EntityContext ec = ManagedUtils.getEntityContext();
        return (ec != null) && ec.getEntityTransaction().isActive();
    }

    public static boolean onCompletion(TransactionCompletionListener completion) {
        EntityContext ec = ManagedUtils.getEntityContext();
        if ((ec == null) || !ec.getEntityTransaction().isActive()) {
            return false;
        }
        ec.pushCompletion(completion);
        return true;
    }

    /**
     * Get the PK from a potentially lazy hibernate entity without triggering a database fetch. This is needed
     * to resolve performance issues with hibernate and field-level access.
     */
    public static Long getId(Id entity) {
        return (entity instanceof HibernateProxy) ? (Long) ((HibernateProxy) entity).getHibernateLazyInitializer().getIdentifier() : entity.getId();
    }

    /**
     * Flushes and clears the Hibernate entity cache, clears thread-local
     * caches.
     */
    public static void flushAndClearCaches() {
        flush(FlushAction.FLUSH_AND_CLEAR_ENTITIES_AND_CACHE);
    }

    public static void flush() {
        flush(FlushAction.FLUSH_ENTITIES_ONLY);
    }

    public static void flush(boolean clear) {
        flush(clear ? FlushAction.FLUSH_AND_CLEAR_ENTITIES : FlushAction.FLUSH_ENTITIES_ONLY);
    }

    public static void flushClearAndCommit() {
        flush(FlushAction.FLUSH_CLEAR_AND_COMMIT);
    }

    public enum FlushAction {
        NOTHING(false, false, false, false), //use for testing, to avoid systemic dependencies
        FLUSH_ENTITIES_ONLY(true, false, false, false),
        FLUSH_AND_CLEAR_ENTITIES(true, true, false, false),
        FLUSH_AND_CLEAR_ENTITIES_AND_CACHE(true, true, true, false),
        FLUSH_CLEAR_AND_COMMIT(true, true, true, true);

        final private boolean flushEntities;
        final private boolean clearEntities;
        final private boolean clearCache;
        final private boolean commit;

        FlushAction(boolean flushEntities, boolean clearEntities, boolean clearCache, boolean commit) {
            this.flushEntities = flushEntities;
            this.clearEntities = clearEntities;
            this.clearCache = clearCache;
            this.commit = commit;
        }
    }

    public static void flush(FlushAction action) {
        EntityManager entityManager = ManagedUtils.getEntityContext().getEntityManager();
        if (action.flushEntities) {
            entityManager.flush();
        }
        if (action.clearEntities) {
            entityManager.clear();
        }
        if (action.clearCache) {
            Current.clearCache();
        }
        if (action.commit) {
            ManagedUtils.commit();
        }
    }

    /**
     * Generate a PK for an entity using the configured entity sequence generator.
     *
     * @return the PK
     */
    public static Long generateId() {
        SessionImplementor implementor = ManagedUtils.getEntityContext().getEntityManager().unwrap(SessionImplementor.class);
        return (Long) implementor.getFactory().getMappingMetamodel()
          .getEntityDescriptor(Data.class)
          .getEntityMetamodel()
          .getIdentifierProperty()
          .getIdentifierGenerator()
          .generate(implementor, null);
    }

    @Deprecated
    public static void splitTransaction(boolean clear) {// TODO: KILL ME IN FAVOUR OF MANAGEDUTILS.COMMIT I THINK..
        EntityContext ec = ManagedUtils.getEntityContext();
        flush(clear);
        ec.completeTransaction();
        ec.beginTransaction();
    }

    public static boolean inSession() {
        EntityContext ec = ManagedUtils.getEntityContext();
        return (ec != null) && ec._entityManager.isOpen();
    }

    /**
     * Flag the transaction as failed and to rollback.
     */
    public static void markTransactionFailed() {
        EntityContext ec = ManagedUtils.getEntityContext();
        ec.setRollbackOnly();
    }

    private EntityManager _entityManager;
    private final List<TransactionCompletionListener> _completions = new ArrayList<>();
    private Duration _timeout;
    private boolean _timeoutOverridden;

    /* protected because one of these is only worthwhile if you
     * have it created and initialized by ManagedUtils.perform... */
    @VisibleForTesting
    protected EntityContext() {
    }

    public void init(EntityManager entityManager) {
        _entityManager = entityManager;
        _completions.clear();
        _timeout = DEFAULT_TIMEOUT;
        _timeoutOverridden = false;
    }

    public EntityManager getEntityManager() {
        return _entityManager;
    }

    private EntityTransaction getEntityTransaction() {
        return _entityManager.getTransaction();
    }

    public void setStatementTimeout(Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Not setting timeout to negative duration " + duration);
        }
        _timeoutOverridden = true;
        _timeout = duration;
        setStatementTimeout1(duration.toMillis());
    }

    private void setStatementTimeout1(long millis) {
        try {
            _entityManager.unwrap(Session.class).doWork(connection -> {
                try (java.sql.Statement statement = connection.createStatement()) {
                    logger.fine("Statement timeout " + millis + " ms");
                    statement.execute("SET statement_timeout = '" + millis + " ms'");
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException("Statement timeout error", ex);
        }
    }

    public Duration getStatementTimeout() {
        return _timeout;
    }

    public boolean isStatementTimeoutSet() {
        return _timeoutOverridden;
    }

    public void setCacheModePutOnly() {
        _entityManager.unwrap(Session.class).setCacheMode(CacheMode.PUT);
    }

    public void setRollbackOnly() {
        EntityTransaction tx = getEntityTransaction();
        if (!tx.isActive()) {
            return;
        }
        tx.setRollbackOnly();
    }

    public void pushCompletion(TransactionCompletionListener completion) {
        _completions.add(completion);
    }

    public <T extends TransactionCompletionListener> T getOrAddCompletionListener(Class<T> listenerClass, Supplier<T> ctor) {
        return _completions.stream().filter(listenerClass::isInstance).map(listenerClass::cast).findFirst()
          .orElseGet(() -> {
              var listener = ctor.get();
              pushCompletion(listener);
              return listener;
          });
    }

    void beginTransaction() {
        getEntityTransaction().begin();
        if (isStatementTimeoutSet()) {
            setStatementTimeout1(_timeout.toMillis());
        }
    }

    void completeTransaction() {
        EntityTransaction tx = getEntityTransaction();
        if (!tx.isActive()) {
            return;
        }
        boolean commit = !tx.getRollbackOnly();
        try {
            transactionCompleting(commit);
            if (commit) {
                tx.commit(); // if this fails, rollback happens automatically.
            } else {
                tx.rollback();
            }
        } catch (Exception ex) {
            commit = false;
            throw new RuntimeException("Transaction failure", ex);
        } finally {
            transactionCompleted(commit);
        }
    }

    public void transactionCompleting(boolean commit) {
        for (TransactionCompletionListener completion : _completions) {
            if (completion instanceof BeforeTransactionCompletionListener) {
                BeforeTransactionCompletionListener before = (BeforeTransactionCompletionListener) completion;
                if (commit) {
                    before.beforeCommit();
                } else {
                    before.beforeRollback();
                }
            }
        }
    }

    public void transactionCompleted(boolean commit) {
        Current.clearPolluted();
        for (TransactionCompletionListener completion : _completions) {
            try {
                if (completion instanceof AfterTransactionCompletionListener) {
                    AfterTransactionCompletionListener after = (AfterTransactionCompletionListener) completion;
                    if (commit) {
                        after.onCommit();
                    } else {
                        after.onRollback();
                    }
                }
            } catch (Throwable th) {
                logger.log(Level.WARNING, "Completion error: " + completion, th);
            }
        }
        _completions.clear();
    }
}

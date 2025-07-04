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

import com.google.common.base.Throwables;
import com.learningobjects.cpxp.component.eval.InferEvaluator;
import com.learningobjects.cpxp.component.eval.InjectEvaluator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FlushModeType;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ManagedUtils {
    private static final Logger logger = Logger.getLogger(ManagedUtils.class.getName());

    public static <T> T newInstance(Class<T> cls) {
        if (cls.getConstructors().length != 1)
            throw new IllegalStateException("Multiple constructors: " + cls.getName());
        try {
            Constructor<T> ctor = (Constructor<T>) cls.getConstructors()[0];
            Object[] params = Arrays.stream(ctor.getParameters())
              .map(p -> {
                  InferEvaluator infer = new InferEvaluator();
                  infer.init(null, p.getName(), p.getParameterizedType(), p.getAnnotations());
                  return infer.getValue(null, null, null);
              }).toArray();
            T t = ctor.newInstance(params);
            di(t, cls, true);
            return t;
        } catch (Throwable th) {
            throw new IllegalStateException("DI error: " + cls.getName(), th);
        }
    }

    public static void di(Object o, boolean selfManaged) {
        try {
            di(o, o.getClass(), selfManaged);
        } catch (Throwable th) {
            logger.log(Level.WARNING, "DI error", th);
            throw Throwables.propagate(th);
        }
    }

    /**
     * @Param selfManaged implies that this is entirely self-managed so the
     * external container will do postconstruct and @Resource injection.
     */
    private static void di(final Object o, final Class<?> clas, boolean selfManaged) throws Throwable {
        Class<?> superClass = clas.getSuperclass();
        if (superClass != null) {
            di(o, superClass, selfManaged);
        }
        for (Field field : clas.getDeclaredFields()) {
            field.setAccessible(true);
            //Support JSR 330 Inject Annotation.
            Optional<Inject> inject = Optional.ofNullable(field.getAnnotation(Inject.class));
            if(inject.isPresent()) {
                field.set(o, InjectEvaluator.lookupField(field));
            }
        }
        if (selfManaged) {
            // For tomcat managed objects, it will call PostConstruct after its DI
            for (Method method : clas.getDeclaredMethods()) {
                if (method.getAnnotation(PostConstruct.class) != null) {
                    method.setAccessible(true);
                    method.invoke(o);
                }
            }
        }
    }



    /** Begin a database transaction. */
    public static void begin() {
        EntityContext existingContext = __entityStorage.get();
        if(existingContext == null) {
            existingContext = new EntityContext();
        }
        __entityStorage.set(existingContext);
        EntityManager em = __entityManagerFactory.createEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        existingContext.init(em);
        try {
            existingContext.beginTransaction();
        } catch (Throwable th) {
            throw new RuntimeException("Failed to begin Transaction new:",th);
        }
    }

    public static <T> T perform(Operation<T> operation) {
        T result;

        // We would like to prohibit nested performance; however, cats in its infinite wisdom will happily
        // reuse a thread to perform nested IO and cause this unhappy sadness situation.
        final var prior = __entityStorage.get();
        final EntityContext context = new EntityContext();
        __entityStorage.set(context);
        try {
            EntityManager em = __entityManagerFactory.createEntityManager();
            em.setFlushMode(FlushModeType.AUTO); // TODO: any measurable performance impact vs COMMIT?

            context.init(em);
            try {
                context.completeTransaction(); /// hmmm
                context.beginTransaction();
                boolean completed = false;
                try {
                    result = operation.perform();
                    completed = true;
                    context.completeTransaction();
                } catch (Exception th) {
                    if (!completed) {
                        try {
                            context.setRollbackOnly();
                            context.completeTransaction();
                        } catch (Throwable th2) {
                            logger.log(Level.WARNING, "Suppressed rollback error", th2);
                        }
                    }
                    throw th;
                }
            } finally {
                if (em.isOpen()) {
                    /* do I need this? */
                    //if (context.isStatementTimeoutSet()) {
                    //    context.setStatementTimeout(1);
                    //}
                    em.close();
                }
            }
        } finally {
            __entityStorage.set(prior);
        }

        return result;
    }

    public static void commit() { // commit this tx and open a new one; used between update and render
        commit(true);
    }

    /**
     * Rollback the current transaction and open a new one.
     */
    public static void rollback() { // used between update and render
        commit(false);
    }

    /** Commit or rollback depending on the success flag, then start a new transaction. */
    public static void commit(boolean success) {
        EntityContext ec = __entityStorage.get();
        if ((ec == null) || !ec.getEntityManager().isOpen()) {
            return;
        }
        if (!success) {
            ec.setRollbackOnly();
        }
        ec.completeTransaction();
        ec.beginTransaction();
    }

    public static void setRollbackOnly() {
        EntityContext ec = __entityStorage.get();
        if ((ec == null) || !ec.getEntityManager().isOpen()) {
            return;
        }
        ec.setRollbackOnly();
    }

    /** Commit or rollback depending on the success flag, then close the entity context. */
    public static void end() {
        EntityContext ec = __entityStorage.get();
        if ((ec == null) || !ec.getEntityManager().isOpen()) {
            return;
        }
        try {
            ec.completeTransaction();
            if (ec.isStatementTimeoutSet()) {
                ec.setStatementTimeout(EntityContext.DEFAULT_TIMEOUT);
            }
        } finally {
            ec.getEntityManager().close();
        }
    }

    /** Commit or rollback depending on the success flag, but do not close the entity context.
     * Do this if you need to run requests explicitly outside of a transaction; for example,
     * concurrent DDL updates. */
    public static void leaveTransaction() {
        EntityContext ec = __entityStorage.get();
        if ((ec == null) || !ec.getEntityManager().isOpen()) {
            return;
        }
        ec.completeTransaction();
    }

    public static void JTACompliantBegin() {
        if (__entityStorage.get() != null) {
            throw new RuntimeException("Cannot begin while a transaction is running");
        }
        begin();
    }

    public static void JTACompliantEnd() {
        end();
        __entityStorage.remove();
    }

    public static void timeout(Duration duration) {
        getEntityContext().setStatementTimeout(duration);
    }

    public static EntityContext getEntityContext() {
        return __entityStorage.get();
    }

    private static final ConcurrentHashMap<Class<?>, Object> __services = new ConcurrentHashMap<>();

    public static <T> T getService(final Class<T> type) {
        if (!type.isAnnotationPresent(Local.class)) {
            throw new IllegalArgumentException("Not an EJB: " + type.getName());
        }
        Object o = __services.get(type);
        if (o == null) {
            // I can't just use a loading cache because of dependency loops so
            // I have to put the uninitialized service into the map before I
            // do the dependency injection phase. Lazy all would solve.
            try {
                // sync so other threads can never see a non-DI instance
                synchronized (__services) {
                    o = __services.get(type);
                    if (o == null) {
                        Constructor<T> ctor = getImplCtor(type);
                        if (ctor.getParameterCount() == 0) {
                            o = ctor.newInstance();
                            __services.put(type, o);
                            di(o, true);
                        } else {
                            Object[] args = new Object[ctor.getParameterCount()];
                            for (int i = 0; i < args.length; i++) {
                                Type argTpe = ctor.getGenericParameterTypes()[i];
                                args[i] = getCtorArg(argTpe);
                            }
                            o = ctor.newInstance(args);
                            __services.put(type, o);
                        }
                    }
                }
            } catch (Throwable th) {
                throw new RuntimeException("Service error: " + type.getName(), th);
            }
        }
        return type.cast(o);
    }

    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> getImplCtor(Class<T> type) throws Exception {
        final String name = type.getName() + "Bean";
        Class<T> impl = (Class<T>) Class.forName(name);
        Constructor<T>[] ctors = (Constructor<T>[]) impl.getDeclaredConstructors();
        if (ctors.length == 1) return ctors[0];
        else throw new IllegalArgumentException("multiple constructors for " + impl);
    }

    private static Object getCtorArg(Type type) {
        if (type instanceof Class<?>) {
            if (((Class<?>) type).isAnnotationPresent(Local.class)) {
                return getService((Class<?>) type);
            }
            // fallthrough
        } else if ((type instanceof ParameterizedType)) {
            // shouldn't use InjectEvaluator here as it is too knowledgeable
            ParameterizedType tr = (ParameterizedType) type;
            if (tr.getRawType() == Provider.class) {
                assert tr.getActualTypeArguments().length == 1 : tr;
                Type targ = tr.getActualTypeArguments()[0];
                return (Provider<Object>) () -> getCtorArg(targ);
            }
            // fallthrough
        }
        // very limitful mockery of a DI mechanism
        InjectEvaluator dele = new InjectEvaluator();
        dele.init(null, null, type, new Annotation[0]);
        return dele.getValue(null, null, Collections.emptyMap());
    }

    static final ThreadLocal<EntityContext> __entityStorage = new ThreadLocal<>();

    static EntityManagerFactory __entityManagerFactory;

    public static EntityManagerFactory getEntityManagerFactory() {
        return __entityManagerFactory;
    }

    public static void init(EntityManagerFactory entityManagerFactory) {
        __entityManagerFactory = entityManagerFactory;
    }

    /* Suppresses DI of resources from the JNDI during test execution.
     * TODO: KILLME: Replace this with a simplejndi. */
    static boolean __noJndi;

}

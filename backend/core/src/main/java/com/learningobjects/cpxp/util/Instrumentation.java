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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import loi.apm.Apm;
import loi.apm.Segment;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Instrumentation {
    private static final Logger logger = Logger.getLogger(Instrumentation.class.getName());

    private static TracerFactory __debugTracerFactory = new NopTracerFactory();
    private static TracerFactory __tracerFactory = new NopTracerFactory();

    private static final ThreadLocal<LinkedList<Tracer>> __tracerStack =
      ThreadLocal.withInitial(LinkedList::new);

    public static void configure(Boolean enabled, Boolean trace, Boolean println) {
        TracerFactory debugFactory = new NopTracerFactory(), actualFactory = debugFactory;
        try {
            if (Boolean.TRUE.equals(enabled)) {
                actualFactory = new ApmTracerFactory();
            } else if (Boolean.TRUE.equals(println)) {
                actualFactory = PrintlnTracerFactory.INSTANCE;
            }

            if (Boolean.TRUE.equals(trace)) {
                debugFactory = actualFactory;
            }
        } catch (Throwable th) {
            logger.log(Level.WARNING, "Error initializing instrumentation. Method tracing disabled.", th);
        }
        __debugTracerFactory = debugFactory;
        __tracerFactory = actualFactory;
    }

    /**
     * Get an execution tracer. By default only top-level methods are traced; lower-level methods
     * are only traced if `apm.trace` is set to `true` in config because of performance
     * costs.
     *
     * @param method   the method being traced
     * @param instance the instance being traced
     * @param topLevel whether to always acquire a real tracer or only if trace-level instrumentation is enabled
     * @return an execution tracer
     */
    public static Tracer getTracer(Method method, Object instance, boolean topLevel) {
        return (topLevel ? __tracerFactory : __debugTracerFactory).getTracer(method, instance);
    }

    /**
     * Begin a debug-level trace.
     */
    public static void begin(String className, String methodName, String signature, Object instance) {
        __tracerStack.get().push(__debugTracerFactory.getTracer(className, methodName, signature, instance));
    }

    /**
     * Succeed a debug-level trace.
     */
    public static <T> T succeedA(T t) {
        return __tracerStack.get().pop().success(t);
    }

    /**
     * Fail a debug-level trace.
     */
    public static Throwable fail(Throwable exn) {
        return __tracerStack.get().pop().failure(exn);
    }

    /* this is silly but it makes my life easier */
    public static boolean succeed(boolean result) {
        return succeedA(result);
    }

    public static byte succeed(byte result) {
        return succeedA(result);
    }

    public static char succeed(char result) {
        return succeedA(result);
    }

    public static short succeed(short result) {
        return succeedA(result);
    }

    public static int succeed(int result) {
        return succeedA(result);
    }

    public static long succeed(long result) {
        return succeedA(result);
    }

    public static float succeed(float result) {
        return succeedA(result);
    }

    public static double succeed(double result) {
        return succeedA(result);
    }

    public static void succeed() {
        succeedA(null);
    }

    public static interface Tracer {
        <T> T success(T result);

        <T extends Throwable> T failure(T exception);
    }

    static interface TracerFactory {
        public Tracer getTracer(Method method, Object instance);

        public Tracer getTracer(String className, String methodName, String signature, Object instance);
    }

    static class NopTracer implements Tracer {
        static final NopTracer INSTANCE = new NopTracer();

        private NopTracer() {
        }

        @Override
        public <T> T success(T result) {
            return result;
        }

        @Override
        public <T extends Throwable> T failure(T exception) {
            return exception;
        }
    }

    static class PrintlnTracerFactory implements TracerFactory {
        static final PrintlnTracerFactory INSTANCE = new PrintlnTracerFactory();

        private PrintlnTracerFactory() {
        }

        @Override
        public Tracer getTracer(Method method, Object instance) {
            return new PrintlnTracer(method.toString());
        }

        @Override
        public Tracer getTracer(String className, String methodName, String signature, Object instance) {
            return new PrintlnTracer(className + "." + methodName + ":" + signature);
        }
    }

    static class PrintlnTracer implements Tracer {
        private final String slug;
        private final long start;

        PrintlnTracer(String slug) {
            this.slug = slug;
            this.start = System.currentTimeMillis();
        }

        @Override
        public <T> T success(T result) {
            log();
            return result;
        }

        @Override
        public <T extends Throwable> T failure(T exception) {
            log();
            return exception;
        }

        private void log() {
            long now = System.currentTimeMillis();
            long delta = now - start;
            if (delta >= 50) {
                System.out.println("end: " + slug + " took " + delta);
            }
        }
    }

    static class NopTracerFactory implements TracerFactory {
        @Override
        public Tracer getTracer(Method method, Object instance) {
            return NopTracer.INSTANCE;
        }

        @Override
        public Tracer getTracer(String className, String methodName, String signature, Object instance) {
            return NopTracer.INSTANCE;
        }
    }

    static class ApmTracerFactory implements TracerFactory {
        private final LoadingCache<Pair<Class<?>, Method>, String> _signatureCache =
          CacheBuilder.newBuilder()
            .weakKeys()
            .build(new CacheLoader<Pair<Class<?>, Method>, String>() {
                @Override
                public String load(Pair<Class<?>, Method> pair) {
                    final Class<?> cls = pair.getLeft();
                    final Method method = pair.getRight();
                    StringBuilder sb = new StringBuilder();
                    sb.append(cls.getSimpleName());
                    sb.append('.');
                    sb.append(method.getName());
                    sb.append('(');
                    boolean first = true;
                    for (Class<?> prm : method.getParameterTypes()) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(',');
                        }
                        sb.append(prm.getSimpleName());
                    }
                    sb.append(')');
                    return sb.toString();
                }
            });

        @Override
        public Tracer getTracer(Method method, Object instance) {
            try {
                return getTracer(_signatureCache.get(Pair.of(instance.getClass(), method)), instance);
            } catch (Exception ex) {
                // I could return a NopTracer but performance would be invisibly diabolical
                // so it is just safer to fail fail fail and let someone fix.
                ex.printStackTrace();
                throw new RuntimeException("Tracer error", ex);
            }
        }

        @Override
        public Tracer getTracer(String className, String methodName, String signature, Object instance) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(className.substring(1 + className.lastIndexOf('.')));
                sb.append('.');
                sb.append(methodName);
                sb.append(signature); // maybe
                return getTracer(sb.toString(), instance);
            } catch (Exception ex) {
                // I could return a NopTracer but performance would be invisibly diabolical
                // so it is just safer to fail fail fail and let someone fix.
                ex.printStackTrace();
                throw new RuntimeException("Tracer error", ex);
            }
        }

        private Tracer getTracer(String signature, Object instance) {
            try {
                Segment segment = Apm.startSegment("Java", signature);
                return new ApmTracer(segment);
            } catch (ClassCastException ex) {
                ex.printStackTrace();
                return NopTracer.INSTANCE;
            }
        }
    }

    static class ApmTracer implements Tracer {
        private final Segment _segment;

        ApmTracer(Segment segment) {
            _segment = segment;
        }

        @Override
        public <T> T success(T result) {
            _segment.end();
            return result;
        }

        @Override
        public <T extends Throwable> T failure(T exception) {
            _segment.end();
            return exception;
        }
    }
}

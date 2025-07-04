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

import java.lang.Thread.UncaughtExceptionHandler;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/* http://stackoverflow.com/questions/3179733/threadfactory-usage-in-java */

public class ThreadFactoryBuilder implements ThreadFactory {
    //thread properties
    private long _stackSize;
    private String _pattern;
    private ClassLoader _classLoader;
    private ThreadGroup _group;
    private int _priority;
    private UncaughtExceptionHandler _handler;
    private boolean _daemon;

    private boolean _configured;

    private boolean _wrapRunnable;//if acc is present wrap or keep it
    protected final AccessControlContext _acc;

    //thread creation counter
    protected final AtomicLong _counter = new AtomicLong();

    public ThreadFactoryBuilder() {
        final Thread t = Thread.currentThread();
        ClassLoader loader;
        AccessControlContext acc = null;
        try {
            loader =  t.getContextClassLoader();
            if (System.getSecurityManager()!=null) {
                acc = AccessController.getContext();//keep current permissions
                acc.checkPermission(new RuntimePermission("setContextClassLoader"));
            }
        } catch (SecurityException _skip) {
            //no permission
            loader =null;
            acc = null;
        }

        _classLoader = loader;
        _acc = acc;
        _priority = t.getPriority();
        _daemon = false;
        _wrapRunnable = true;
        StackTraceElement[] stack =  Thread.currentThread().getStackTrace();
        pattern((stack.length > 2) ? ClassUtils.getOuterName(stack[2].getClassName())
                : "ThreadFactoryBuilder", true);
    }

    public ThreadFactory finishConfig() {
        _configured = true;
        _counter.addAndGet(0);//write fence "w/o" volatile
        return this;
    }

    public long getCreatedThreadsCount() {
        return _counter.get();
    }

    protected void assertConfigurable() {
        if (_configured)
            throw new IllegalStateException("already configured");
    }

    private static String getOuterClassName(String className) {
        int idx = className.lastIndexOf('.')+1;
        className = className.substring(idx);//remove package
        idx = className.indexOf('$');
        if (idx<=0) {
            return className;//handle classes starting w/ $
        }
        return className.substring(0,idx);//assume inner class

    }

    @Override
    public Thread newThread(Runnable r) {
        _configured = true;
        final Thread t = new Thread(_group, wrapRunnable(r), composeName(r), _stackSize);
        t.setPriority(_priority);
        t.setDaemon(_daemon);
        t.setUncaughtExceptionHandler(_handler);//securityException only if in the main group, shall be safe here
        //funny moment Thread.getUncaughtExceptionHandler() has a race.. badz (can throw NPE)

        applyCCL(t);
        return t;
    }

    private void applyCCL(final Thread t) {
        if (_classLoader!=null) {//use factory creator ACC for setContextClassLoader
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                        public Object run() {
                        t.setContextClassLoader(_classLoader);
                        return null;
                    }
                }, _acc);
        }
    }

    private Runnable wrapRunnable(final Runnable r) {
        if ((_acc == null) || !_wrapRunnable) {
            return r;
        }
        Runnable result = new Runnable() {
                public void run() {
                    AccessController.doPrivileged(new PrivilegedAction<Object>() {
                            @Override
                                public Object run() {
                                r.run();
                                return null;
                            }
                        }, _acc);
                }
            };
        return result;
    }

    protected String composeName(Runnable r) {
        return String.format(_pattern, _counter.incrementAndGet(), System.currentTimeMillis());
    }

    //standard setters allowing chaining, feel free to add normal setXXX
    public ThreadFactoryBuilder pattern(String pattern, boolean appendFormat) {
        assertConfigurable();
        if (appendFormat) {
            pattern += "-%d"; // : %d @ %tF %<tT";//counter + creation time
        }
        _pattern = pattern;
        return this;
    }


    public ThreadFactoryBuilder daemon(boolean daemon) {
        assertConfigurable();
        _daemon = daemon;
        return this;
    }

    public ThreadFactoryBuilder priority(int priority) {
        assertConfigurable();
        if (priority<Thread.MIN_PRIORITY || priority>Thread.MAX_PRIORITY) {//check before actual creation
            throw new IllegalArgumentException("priority: "+priority);
        }
        _priority = priority;
        return this;
    }

    public ThreadFactoryBuilder stackSize(long stackSize) {
        assertConfigurable();
        _stackSize = stackSize;
        return this;
    }

    public ThreadFactoryBuilder threadGroup(ThreadGroup group) {
        assertConfigurable();
        _group = group;
        return this;
    }

    public ThreadFactoryBuilder exceptionHandler(UncaughtExceptionHandler exceptionHandler) {
        assertConfigurable();
        _handler = exceptionHandler;
        return this;
    }

    public ThreadFactoryBuilder wrapRunnable(boolean wrapRunnable) {
        assertConfigurable();
        _wrapRunnable = wrapRunnable;
        return this;
    }

    public ThreadFactoryBuilder contextClassLoader(ClassLoader ccl) {
        assertConfigurable();
        _classLoader = ccl;
        return this;
    }
}

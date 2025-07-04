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

package com.learningobjects.cpxp.component;

import com.google.common.base.Throwables;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.util.Instrumentation;
import com.learningobjects.cpxp.util.ThreadTerminator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ServiceInvocationHandler implements InvocationHandler {

    private final DelegateDescriptor _delegate;
    private final ComponentInstance _instance;

    private Object _object;

    public ServiceInvocationHandler(DelegateDescriptor delegate, ComponentInstance instance) {
        _delegate = delegate;
        _instance = instance;
    }

    public ComponentInstance getInstance() {
        return _instance;
    }

    public DelegateDescriptor getDelegate() {
        return _delegate;
    }

    public Object getObject() {
        return _object;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        initialize();
        Class<?> declarer = method.getDeclaringClass();
        if (Object.class.equals(declarer) || ComponentInterface.class.equals(declarer)) {
            // short circuit uninteresting methods
            return method.invoke(_object, args);
        }
        ThreadTerminator.check();
        try {
            Instrumentation.Tracer tracer = Instrumentation.getTracer(method, _object, _delegate.instrument());
            try {
                return tracer.success(method.invoke(_object, args));
            } catch (Exception ex) {
                throw tracer.failure(ex);
            }
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    protected boolean isSingleton() {
        return true;
    }

    private synchronized void initialize() {
        if (_object != null) {
            return;
        }

        if (isSingleton()) {
            _object = _instance.getEnvironment().getSingletonCache().getRealService(_delegate);
        } else {
            _object = _instance.getInstance(_delegate);
        }
    }
}

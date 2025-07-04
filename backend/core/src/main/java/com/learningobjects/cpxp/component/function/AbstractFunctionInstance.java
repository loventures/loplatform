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

package com.learningobjects.cpxp.component.function;

import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.util.Instrumentation;
import com.learningobjects.cpxp.util.ObjectUtils;

import java.lang.reflect.Method;

/**
 * The base function type. Subtypes can control how the {@link Function} is invoked.
 */
public class AbstractFunctionInstance implements FunctionInstance {

    protected ComponentInstance _instance;
    protected FunctionDescriptor _function;

    @Override
    public void init(ComponentInstance instance, FunctionDescriptor function) {
        _instance = instance;
        _function = function;
    }

    @Override
    public ComponentInstance getComponentInstance() {
        return _instance;
    }

    @Override
    public FunctionDescriptor getFunction() {
        return _function;
    }

    @Override
    public Object getObject() {
        return _function.isStatic() ? null : _instance.getInstance(_function.getDelegate());
    }

    protected Object invoke(Object object, Object[] parameters) {
        try {
            final Method method = _function.getMethod();
            final Instrumentation.Tracer tracer = Instrumentation.getTracer(method, object, _function.instrument());
            try {
                return tracer.success(method.invoke(object, parameters));
            } catch (Exception ex) {
                throw tracer.failure(ex);
            }
        } catch (Exception e) {
            String objStr = ObjectUtils.safeToString(object, () -> "<error> (" + _instance + ")");
            throw new RuntimeException("Error invoking: " + _function.getMethod() + " on " + objStr, e);
        }
    }

    public String toString() {
        return _function.getMethod().toString();
    }
}

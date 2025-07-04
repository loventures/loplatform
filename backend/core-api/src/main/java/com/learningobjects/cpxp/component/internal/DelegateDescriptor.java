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

package com.learningobjects.cpxp.component.internal;

import com.learningobjects.cpxp.component.ComponentDescriptor;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.ComponentInterface;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public interface DelegateDescriptor {
    ComponentDescriptor getComponent();

    Class<?> getDelegateClass();

    boolean isService();

    boolean instrument();

    Map<Class<? extends ComponentInterface>, Annotation> getBindings();

    Set<Class<?>> getServiceInterfaces();

    <T extends Annotation> T getBinding(
            Class<? extends ComponentInterface> iface);

    Object newInstance(ComponentInstance instance);

    Object newInstance(ComponentInstance instance, Consumer<Object> cache);

    Object invokeRef(String name, ComponentInstance instance, Object object);

    Object invokeRef(String name, ComponentInstance instance, Object object, Object[] args);

    void lifecycle(Class<? extends Annotation> lifecycle) throws Exception;

    void addMethodRef(Method method) throws Exception;

    void checkAccess(ComponentInstance instance);
}

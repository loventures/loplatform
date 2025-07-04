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

import com.google.common.collect.MapMaker;
import com.learningobjects.cpxp.component.ComponentInstance;
import com.learningobjects.cpxp.component.acl.AccessControl;
import com.learningobjects.cpxp.component.acl.AccessEnforcer;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.ServiceException;
import com.learningobjects.cpxp.service.accesscontrol.AccessControlException;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractDescriptor {
    private final List<EnforceChecker> _enforcers = new ArrayList<>();

    protected void introspectAccess(DelegateDescriptor parent, AnnotatedElement described) {
        for (Annotation annotation : described.getDeclaredAnnotations()) {
            introspectAccess(parent, annotation);
        }
    }

    protected void introspectAccess(DelegateDescriptor parent, Annotation annotation) {
        // annotation is a proxy implementation of the interface
        AccessControl access = annotation.annotationType().getAnnotation(AccessControl.class);
        if (access != null) {
            _enforcers.add(new EnforceChecker(parent, access.value(), annotation));
        }
    }

    public void checkAccess(ComponentInstance instance) {
        for (EnforceChecker checker : _enforcers) {
            checker.checkAccess(instance);
        }
    }

    private static class EnforceChecker {
        private DelegateDescriptor _descriptor;
        private Annotation _annotation;
        private Class<? extends Annotation> _annotationClass;

        public EnforceChecker(DelegateDescriptor parent, Class<? extends AccessEnforcer> clas, Annotation annotation){
            _descriptor = getEnforcerDescriptor(parent, clas);
            _annotation = annotation;
            _annotationClass = annotation.annotationType();
        }

        public void checkAccess(ComponentInstance instance) {
            try {
                Current.put(_annotationClass, _annotation);
                AccessEnforcer enforcer = (AccessEnforcer) _descriptor.newInstance(instance);
                if (!enforcer.checkAccess()) {
                    throw new AccessControlException("Access denied: " + _annotation);
                }
            } catch (ServiceException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new AccessControlException("Access denied: " + _annotation, ex);
            } finally {
                Current.remove(_annotationClass);
            }
        }
    }

    private static final ConcurrentMap<Class<?>, DelegateDescriptor> __enforcers = new MapMaker().weakKeys().makeMap();

    // I reuse the DI support from DelegateDescriptor for instantiating access enforcers.
    public static synchronized DelegateDescriptor getEnforcerDescriptor(DelegateDescriptor parent, Class<? extends AccessEnforcer> clas) {
        DelegateDescriptor delegateDescriptor = __enforcers.get(clas);
        if (delegateDescriptor == null) {
            delegateDescriptor = new BaseDelegateDescriptor(parent, clas);
            __enforcers.put(clas, delegateDescriptor);
        }
        return delegateDescriptor;
    }
}

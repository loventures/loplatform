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

package com.learningobjects.cpxp.service.facade;

import com.learningobjects.cpxp.dto.EntityDescriptor;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.service.finder.Finder;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.util.EntityContext;
import com.learningobjects.cpxp.util.ManagedUtils;
import com.learningobjects.cpxp.util.ThreadTerminator;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Invocation handler for a facade proxy.
 */
class FacadeInvocationHandler implements InvocationHandler {
    private final FacadeDescriptor _descriptor;
    private final ServiceContext _context;
    private Item _item;
    private Map<String, Object> _values;
    private boolean _standalone;

    public FacadeInvocationHandler(FacadeDescriptor descriptor, Item item, ServiceContext context) {
        _descriptor = descriptor;
        _item = item;
        _context = context;
        if (item == null) {
            _values = new HashMap<>();
        }
        EntityDescriptor entity = descriptor.getEntityDescriptor();
        _standalone = (entity != null) && !entity.getItemRelation().isPeered();
    }

    boolean isDummy() {
        return _item == null;
    }

    Item getItem() {
        return _item;
    }

    boolean isStandalone() {
        return _standalone;
    }

    ServiceContext getContext() {
        return _context;
    }

    boolean hasValue(String methodName) {
        return (_values != null) && _values.containsKey(methodName);
    }

    Object getValue(String methodName) {
        return (_values == null) ? null : _values.get(methodName);
    }

    void setValue(String methodName, Object value, Object[] args) {
        if ((args == null) || (args.length == 0)) {
            setValue(methodName, value);
        }
    }

    void setValue(String methodName, Object value) {
        if (_values != null) {
            _values.put(methodName, value);
        }
    }

    void removeValue(String methodName) {
        if (_values != null) {
            _values.remove(methodName);
        }
    }

    public void removeAllValuesInHandlerGroup(Method method) {
        String groupName = FacadeUtils.getNameForHandlerGroupWithThisMethod(method);
        for (FacadeMethodHandler methodHandler : _descriptor.getMethodHandlersByGroupName(groupName)) {
            removeValue(methodHandler.getMethodName());
        }
    }

    void attach() { // is this sufficiently efficient
        if (_item == null) {
            // not a real entity
            return;
        }
        EntityContext ec = ManagedUtils.getEntityContext();
        if ((ec == null) || !ec.getEntityManager().isOpen()) {
            // outside of transaction context
            return;
        }
        Finder finder = _item.getFinder();
        if (_standalone ? finder.isNew() : _item.isNew()) {
            // entity created and not yet persisted
            return;
        }
        if (ec.getEntityManager().contains((finder == null) ? _item : finder)) {
            // entity in the entity context
            return;
        }
        // Silently re-attach any detached entities.. Hmm...
        if (finder != null) {
            final Class<?> finderClass = _descriptor.getEntityDescriptor().getEntityType();
            finder = (Finder) ec.getEntityManager().find(finderClass, finder.getId());
            _item = finder.getOwner();
        } else {
            _item = ec.getEntityManager().find(Item.class, _item.getId());
        }
    }

    public Long getId() { // See also ItemService#getId
        if (_item == null) {
            return null;
        } else if (!isStandalone()) {
            return EntityContext.getId(_item);
        } else if (_item.getFinder() != null) {
            return EntityContext.getId(_item.getFinder());
        } else {
            return null;
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        this.attach();
        ThreadTerminator.check();
        if (method.isDefault()) {
            return invokeDefaultMethod(proxy, method, args);
        }

        FacadeMethodHandler methodHandler = _descriptor.getHandler(method);
        if (methodHandler == null) {
            throw new RuntimeException("Unsupported facade method: " + method.getName());
        }
        return methodHandler.invoke(this, args);
    }

    private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
        // Attempt private access to proxy's implemented interface (JDK9+)
        try {
            return MethodHandles.lookup()
                .findSpecial(method.getDeclaringClass(),
                             method.getName(),
                             MethodType.methodType(method.getReturnType(),
                                                   method.getParameterTypes()),
                             method.getDeclaringClass())
                .bindTo(proxy)
                .invokeWithArguments(args);
          // Fallback on JDK8 private access hack
        } catch (IllegalAccessException e) {
              final Class<?> declaringClass = method.getDeclaringClass();
              Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class); //BAD using a private constructor. Not part of the API
              constructor.setAccessible(true); //Fairly bad, we have to first hack the constructor to allow us to call it despite it being private
              return
                  constructor.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE) //once we have come this far abusing the constructor is easy
                  .unreflectSpecial(method, declaringClass)
                  .bindTo(proxy)
                  .invokeWithArguments(args);
        }
    }
}

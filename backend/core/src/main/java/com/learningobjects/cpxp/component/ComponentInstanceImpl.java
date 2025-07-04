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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.learningobjects.cpxp.BaseServiceMeta;
import com.learningobjects.cpxp.IdType;
import com.learningobjects.cpxp.component.function.FunctionInstance;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.registry.Bound;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.service.ServiceContext;
import com.learningobjects.cpxp.util.StringUtils;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class ComponentInstanceImpl implements ComponentInstance {
    private final ComponentEnvironment _environment;

    private final ComponentDescriptor _component;

    private final IdType _item;

    private final Long _context;

    private final LoadingCache<DelegateDescriptor, Object> _instances;

    private final Object[] _args;

    ComponentInstanceImpl(ComponentEnvironment env, ComponentDescriptor component, IdType item, Long context, Object... args) {
        _environment = env;
        _component = component;
        _item = item;
        _context = context;
        _instances = CacheBuilder.newBuilder()
            .build(new CacheLoader<DelegateDescriptor, Object>() {
                    @Override
                    public Object load(DelegateDescriptor delegate) {
                        return delegate.newInstance(ComponentInstanceImpl.this);
                    }
                });
        _args = args;
    }

    @Override
    public Object[] getArgs() {
        return _args;
    }

    @Override
    public ComponentDescriptor getComponent() {
        return _component;
    }

    @Override
    public ComponentEnvironment getEnvironment() {
        return _environment;
    }

    @Override
    public Long getId() {
        return (_item == null) ? null : _item.getId();
    }

    @Override
    public String getItemType() {
        return (_item == null) ? null : _item.getItemType();
    }

    @Override
    public boolean getAcl() {
        // only enforce ACL is this was created against an acl'ed facade
        return false;
    }

    @Override
    public IdType getItem() {
        return _item;
    }

    @Override
    public Long getContext() {
        return _context;
    }

    @Override
    public String getIdentifier() {
        return _component.getIdentifier();
    }

    @Override
    public String getVersion() {
        return StringUtils.defaultIfEmpty(_component.getVersion(), BaseServiceMeta.getServiceMeta().getVersion());
    }

    @Override
    public String getName() {
        String name = evalI18n(_component.getComponentAnnotation().name());
        if (name == null) {
            Class<?> clas = _component.getComponentClass();
            name = evalI18n(StringUtils.toSeparateWords(StringUtils.removeEnd(clas.getSimpleName(), "Impl")));
        }
        return name;
    }

    @Override
    public String getDescription() {
        return evalI18n(_component.getComponentAnnotation().description());
    }

    @Override
    public String getIcon() {
        String icon = (String) eval(_component.getComponentAnnotation().icon());
        if (StringUtils.isEmpty(icon)) {
            icon = _component.getComponentClass().getSimpleName() + ".png";
        }
        return ComponentUtils.resourceUrl(icon, _component);
    }

    // TODO: The context should be a Facade, not an Id.
    @Override
    public boolean checkContext() {
        Object result = eval(_component.getComponentAnnotation().context());
        if (Boolean.FALSE.equals(result) || (result == null)) {
            return false;
        } else if ((_context == null) || "*".equals(result) || Boolean.TRUE.equals(result)) {
            return true;
        } else {
            String contextType = ServiceContext.getContext().getItemService().get(_context).getType();
            return ((String) result).contains(contextType); // sufficient albeit inaccurate
        }
    }

    // supported interfaces and methods

    @Override
    public boolean isSupported(Class<? extends ComponentInterface> iface) {
        return _component.isSupported(iface);
    }

    @Override
    public <T extends ComponentInterface> T getInstance(Class<T> iface, Object... args) {
        // TODO: Either I should kill this args concept, or I should
        // support it in self instance evaluation, or else throw if
        // args supplied inappropriately. The same effect could be
        // achieved with stateful interfaces that have an init(args)
        // method...
        if (iface.isInterface()) {
            if (!isSupported(iface)) {
                if (ComponentDecorator.class.isAssignableFrom(iface)) {
                    ComponentDescriptor decorator;
                    if(iface.isAnnotationPresent(Bound.class)){
                        decorator = Optional.ofNullable(ComponentSupport.lookupComponent(iface, getComponent().getComponentClass())).orElseGet(() -> ComponentSupport.lookupComponent(iface, iface));
                    } else {
                        decorator = ComponentSupport.getComponentDescriptor(iface);
                    }
                    if(decorator == null){
                        throw new IllegalArgumentException("Unsupported interface: " + getIdentifier() + " (" + getItemType() + ":" + getId() + ") / " + iface.getSimpleName());
                    }
                    ComponentInstance instance = decorator.getInstance(_environment, _item, null, args);
                    return instance.getInstance(iface);
                } else {
                    throw new IllegalArgumentException("Unsupported interface: " + getIdentifier() + " (" + getItemType() + ":" + getId() + ") / " + iface.getSimpleName());
                }
            }
            return getInstance(iface, _component.getDelegate());
        } else {
            if (iface.isAssignableFrom(_component.getDelegate().getDelegateClass())) {
                return iface.cast(getInstance());
            } else {
                DelegateDescriptor delegate = _component.getDelegate();
                if (iface.isAssignableFrom(delegate.getDelegateClass())) {
                    return iface.cast(delegate.newInstance(this));
                } else {
                    // assume it is a decorator implementation
                    return ComponentSupport
                      .getComponentDescriptor(iface)
                      .getInstance(_environment, _item, null, args)
                      .getInstance(iface);
                }
            }
        }
    }

    @Override
    public <T extends ComponentInterface> T getInstance(Class<T> iface, DelegateDescriptor delegate) { // TODO: Cache these
        return ComponentSupport.getInstance(iface, this, delegate);
    }

    @Override
    public Object newServiceInstance(DelegateDescriptor delegate, boolean immediate, Consumer<Object> cache) {
        if (_item != null || _context != null || _args.length != 0) {
            throw new RuntimeException(String.format("not constructing %s as a service", this));
        }

        Set<Class<?>> serviceInterfaces = delegate.getServiceInterfaces();
        if (serviceInterfaces.isEmpty() || immediate) {
            return delegate.newInstance(this, cache);
        } else {
            // interface-based services get lazily initialized upon first access
            final Class<?>[] interfaces = serviceInterfaces.toArray(new Class<?>[serviceInterfaces.size()]);
            final ServiceInvocationHandler h = new ServiceInvocationHandler(delegate, this);
            return Proxy.newProxyInstance(delegate.getDelegateClass().getClassLoader(), interfaces, h);
        }
    }

    @Override
    public <T extends FunctionInstance> Optional<T> getOptionalFunctionInstance(Class<T> type, Object... keys) {

        final FunctionDescriptor function =
          _environment.getRegistry().getFunction(_component, type, keys);

        final Optional<T> functionInstance;
        if (function == null) {
            functionInstance = Optional.empty();
        } else {
            functionInstance = Optional.of(getFunctionInstance(type, function));
        }

        return functionInstance;
    }

    @Override
    public <T extends FunctionInstance> T getFunctionInstance(Class<T> type, Object... keys) {
        FunctionDescriptor function = _environment.getRegistry().getFunction(_component, type, keys);
        if (function == null) {
            // throw new RuntimeException("Unknown function: " + type + "; keys: " + Arrays.toString(keys));
            return null;
        }
        return getFunctionInstance(type, function);
    }

    @Override
    public <T extends FunctionInstance> T getFunctionInstance(Class<T> type, FunctionDescriptor function) {
        try {
            T instance = type.newInstance();
            instance.init(this, function);
            return instance;
        } catch (InstantiationException | IllegalAccessException ex) {
            throw Throwables.propagate(ex);
        }
    }

    // configuration

    @Override
    public Object getInstance(DelegateDescriptor delegate) {
        return _instances.getUnchecked(delegate);
    }

    @Override
    public Object getInstance() {
        return _instances.getUnchecked(_component.getDelegate());
    }

    // evaluation

    @Override
    public Object eval(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        } else if (value.startsWith("$$")) {
            return ComponentUtils.expandMessage(value, _component);
        } else if (value.startsWith("#")) {
            String name = value.substring(1);
            return _component.getDelegate().invokeRef(name, this, getInstance());
        } else {
            return value;
        }
    }

    @Override
    public String evalI18n(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        } else if (value.startsWith("$$")) {
            return ComponentUtils.expandMessage(value, _component);
        } else {
            return ComponentUtils.i18n(value, _component);
        }
    }

    @Override
    public int hashCode() {
        return (getId() == null) ? 0 : getId().intValue();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof ComponentInstance) && (getId() != null) && getId().equals(((ComponentInstance) o).getId());
    }

    @Override
    public String toString() {
        if (_item == null) {
            return "ComponentInstance[" + _component.getIdentifier() + "]";
        } else {
            return "ComponentInstance[" + _component.getIdentifier() + "/" + _item.getId() + "/" + _item.getItemType() + "]";
        }
    }
}

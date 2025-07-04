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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.learningobjects.cpxp.BaseWebContext;
import com.learningobjects.cpxp.Id;
import com.learningobjects.cpxp.IdType;
import com.learningobjects.cpxp.component.annotation.*;
import com.learningobjects.cpxp.component.function.AbstractFunctionInstance;
import com.learningobjects.cpxp.component.internal.ComponentLifecycle;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.component.internal.LifecycleInstance;
import com.learningobjects.cpxp.component.registry.Bound;
import com.learningobjects.cpxp.component.registry.ResourceRegistry;
import com.learningobjects.cpxp.component.registry.SchemaRegistry;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.component.web.util.JacksonUtils;
import com.learningobjects.cpxp.dto.DataTransfer;
import com.learningobjects.cpxp.dto.Facade;
import com.learningobjects.cpxp.service.Current;
import com.learningobjects.cpxp.service.component.ComponentConstants;
import com.learningobjects.cpxp.service.item.Item;
import com.learningobjects.cpxp.service.item.ItemSupport;
import com.learningobjects.cpxp.service.script.ComponentFacade;
import com.learningobjects.cpxp.util.*;
import com.learningobjects.cpxp.util.collection.BreadthFirstSupertypeIterable;
import com.learningobjects.de.web.Queryable;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Local;
import javax.validation.groups.Default;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Collections.singleton;

public class ComponentSupport {
    private static final Logger logger = Logger.getLogger(ComponentSupport.class.getName());

    @Deprecated
    public static ComponentEnvironment getEnvironment() {
        return BaseWebContext.getContext().getComponentEnvironment();
    }

    /** Returns whether the specified component is enabled in this environment
     * (if a class), else if any implementation is available (if an interface). */
    public static boolean hasComponent(Class<? extends ComponentInterface> cls) {
        return cls.isInterface() ? !getEnvironment().getComponents(cls).isEmpty()
          : hasComponent(cls.getName());
    }

    public static boolean hasComponent(String identifier) {
        ComponentEnvironment env = getEnvironment();
        return env.hasComponent(identifier);
    }

    public static ComponentDescriptor getComponentDescriptor(String identifier) {
        return getComponentDescriptor(null, identifier);
    }

    public static ComponentDescriptor getComponentDescriptor(Class<? extends ComponentInterface> iface) {
        return getComponentDescriptor(iface, (String) null);
    }

    @SuppressWarnings("unchecked")
    private static ComponentDescriptor getComponentDescriptor(Class<? extends ComponentInterface> iface, String identifier) {
        ComponentEnvironment env = getEnvironment();
        if (env == null || (identifier == null && iface == null)) {
            return null;
        } else if (identifier != null) {
            ComponentDescriptor desc = env.getComponent(identifier);
            if (desc == null) {
                SchemaRegistry.Registration registration = lookupResource(Schema.class, SchemaRegistry.Registration.class, identifier, Default.class);
                desc = (registration == null) ? null : getComponentDescriptor((Class<? extends ComponentInterface>) registration.getSchemaClass());
            }
            return desc;
        } else {
            if (iface.isInterface()) {
                // If we are given an interface, pick the first descriptor that implements the given interface
                /* this is atrocious in the case of polymorphic components... could it just throw? */
                return ObjectUtils.getFirstNonNullIn(env.getComponents(iface));
            } else {
                // Else the given class is an actual component implementation
                return env.getComponent(iface.getName());
            }
        }
    }

    private static ComponentDescriptor getComponentDescriptor(Class<? extends ComponentInterface> iface, IdType item) {
        if (item != null) {
            // In case of asking for shared interface (e.g. Subscribable) on a singleton item type
            // we need to look up the singleton component and not an arbitrary subscribable
            ComponentEnvironment env = getEnvironment();
            Class<? extends ComponentInterface> itemComponent = env.getRegistry().getItemComponent(item.getItemType());
            if (itemComponent != null) {
                iface = itemComponent;
            }
        }
        if (iface != null) {
            ItemMapping mapping = iface.getAnnotation(ItemMapping.class);
            if (mapping != null && mapping.singleton()) {
                /* If it's a singleton there's no point to checking the db for a component id */
                return getComponentDescriptor(iface);
            }
        }
        String identifier = DataTransfer.getStringData(ItemSupport.get(item), ComponentConstants.DATA_TYPE_COMPONENT_IDENTIFIER);
        return getComponentDescriptor(iface, identifier);
    }

    public static boolean isSupported(Class<? extends ComponentInterface> iface, IdType item) {
        ComponentDescriptor component = getComponentDescriptor(iface, item);
        return (component != null) && component.isSupported(iface);
    }

    public static ComponentInstance getComponent(String identifier, Long context) {
        ComponentDescriptor component = getComponentDescriptor(null, identifier);
        return (component == null) ? null : component.getInstance(getEnvironment(), IdType.NULL, context);
    }

    public static ComponentInstance getComponent(IdType item, Long context) {
        if (item == null) return null;
        ComponentEnvironment env = getEnvironment();
        // See also FacadeComponentHandler.. Can this be done more efficiently?
        Class<? extends ComponentInterface> iface = env.getRegistry().getItemComponent(item.getItemType());
        return getComponent(item, context, iface);
    }

    private static ComponentInstance getComponent(IdType item, Long context, Class<? extends ComponentInterface> iface) {
        if (item == null) return null;
        ComponentDescriptor component = getComponentDescriptor(iface, item);
        if (component == null) {
            if (iface == null)
                return null;
            component = getUnknownImplementation(getEnvironment(), iface).orElseThrow(() ->
              new RuntimeException("Unknown component: " + item.getId() + "/" + item.getItemType()));
        }
        return component.getInstance(item, context);
    }

    @Deprecated
    public static <T extends ComponentInterface> T decorate(IdType info, Class<T> iface) {
        if (info == null) return null;
        ComponentDescriptor componentDescriptor = ComponentSupport.getComponentDescriptor(iface.getName()); // that's not an interface
        ComponentInstance componentInstance = componentDescriptor.getInstance(info, null);
        return componentInstance.getInstance(iface);
    }

    public static <T extends ComponentInterface> T decorate(IdType info, Class<T> iface, Class<? extends T> impl) {
        if (info == null) return null;
        ComponentDescriptor componentDescriptor = ComponentSupport.getComponentDescriptor(impl.getName());
        ComponentInstance componentInstance = componentDescriptor.getInstance(info, null);
        return componentInstance.getInstance(iface);
    }

    /**
     * Return all implementation objects of the given interface class
     *
     * @param iface interface class
     * @param <T> type of interface class
     * @return all implementation objects of the given interface class
     */
    public static <T extends ComponentInterface> List<T> getComponents(Class<T> iface) {
        List<T> components = new ArrayList<>();
        for (ComponentInstance instance : getComponents(iface, null)) {
            components.add((T) instance.getInstance());
        }
        return components;
    }

    public static Iterable<ComponentDescriptor> getComponentDescriptors(Class<? extends ComponentInterface> iface) {
        ComponentEnvironment env = getEnvironment();
        return env.getComponents(iface);
    }

    public static List<ComponentInstance> getComponents(Class<? extends ComponentInterface> iface, Long context) {
        ComponentEnvironment env = getEnvironment();
        List<ComponentInstance> components = new ArrayList<>();
        for (ComponentDescriptor component : getComponentDescriptors(iface)) {
            try {
                if (component.isStateless()) {
                    components.add(env.getSingletonCache().getComponent(component));
                } else {
                    ComponentInstance instance = component.getInstance(null, context);
                    if (instance.checkContext()) {
                        components.add(instance);
                    }
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Component error: " + component.getIdentifier(), ex);
            }
        }
        Collections.sort(components, new PropertyComparator<ComponentInstance>("name"));
        return components;
    }

    public static <T extends ComponentInterface> T get(Class<T> iface) {
        ComponentDescriptor cd = getComponentDescriptor(iface);
        ComponentInstance ci;
        if (cd.isStateless()) {
            ci = getEnvironment().getSingletonCache().getComponent(cd);
        } else {
            ci = cd.getInstance(null, null);
        }
        return ci.getInstance(iface);
    }

    public static <T extends ComponentInterface> T get(IdType idType, Class<T> iface) {
        return getInstance(iface, idType, null);
    }

    public static <S extends IdType, T extends ComponentInterface> java.util.function.Function<S, T> idToComponent(Class<T> iface){
        return idType -> get(idType, iface);
    }

    public static <T extends ComponentInterface> T get(Long id, Class<T> iface) {
        return get(id, null, iface);
    }

    public static <T extends ComponentInterface> T get(Long id, String itemType, Class<T> iface) {
        if (itemType == null) {
            itemType = getItemType(iface);
        }
        return getInstance(iface, ItemSupport.get(id, itemType), null);
    }

    public static <T extends ComponentInterface> List<T> getById(Iterable<Long> ids, Class<T> iface) {
        return get(ids, null, iface);
    }

    public static <T extends ComponentInterface> List<T> get(Iterable<Long> ids, String itemType, Class<T> iface) {
        if (itemType == null) {
            itemType = getItemType(iface);
        }
        return get(ItemSupport.get(ids, itemType), iface);
    }

    public static String getItemType(Class<? extends ComponentInterface> iface) {
        ItemMapping mapping = iface.getAnnotation(ItemMapping.class);
        return (mapping != null) ? mapping.value() : null;
    }

    public static boolean isSingleton(Class<? extends ComponentInterface> iface) {
        ItemMapping mapping = iface.getAnnotation(ItemMapping.class);
        return (mapping != null) ? mapping.singleton() : false;
    }

    public static <T extends ComponentInterface> List<T> get(Iterable<? extends IdType> items, Class<T> iface) {
        List<T> list = new ArrayList<>();
        for (IdType item : items) {
            T t = getInstance(iface, item, null);
            if (t != null) {
                list.add(t);
            }
        }
        return list;
    }

    public static <T extends ComponentInterface> T getInstance(Class<T> iface, IdType info, Long context) {
        Item item = ItemSupport.get(info); // check not deleted
        if (item == null) {
            return null;
        } else if (ComponentDecorator.class.isAssignableFrom(iface)) {
            ComponentDescriptor decorator;
            if (iface.isAnnotationPresent(Bound.class)) { // polymorphic decorator bound by the target component type
                ComponentDescriptor actual = getComponent(item, context).getComponent();
                Class<?> lookupKey = actual.getComponentClass(); // lookup key is the implementation class
                decorator = Optional.ofNullable(lookupComponent(iface, lookupKey)).orElseGet(() -> lookupComponent(iface, iface)); // if there's no decorator, fallback to the base interface
                if (decorator == null) {
                    decorator = getUnknownImplementation(getEnvironment(), iface).orElseThrow(() ->
                        new RuntimeException("No " + iface.getName() + " implementation bound for component " + lookupKey.getName()));
                }
            } else {
                // look up sole implementation
                decorator = getComponentDescriptor(iface);
            }
            ComponentInstance instance = decorator.getInstance(item, context);
            return instance.getInstance(iface);
        } else {
            return getComponent(item, context, iface).getInstance(iface);
        }
    }

    private static Optional<ComponentDescriptor> getUnknownImplementation(
            ComponentEnvironment env, Class<? extends ComponentInterface> category) {
        return env.getComponents(category)
          .stream()
          .filter(c -> c.getAnnotation(DefaultImplementation.class) != null)
          .findFirst();
    }

    /**
     * A Component is basically a class that has a registered ComponentDescriptor. Contrary to popular
     * belief, in does not require a corresponding row in the database. But nor can you just "new it up"
     * by directly calling the constructor. Use this method as a "Component-aware constructor" in cases
     * where you need to roll your own class, not drawn directly from the DB, which plays nicely with
     * the Component framework, by specifying a (non-inner) class which implements ComponentInterface.
     * @param implType - the implementation class which will be instantiated, and to whose thusly created instance
     *                 the proxy will forward most method calls
     * @param <IMPL> - the type of the implementation class
     * @return this returns a proxy object which extends implType
     */
    public static <IFACE extends ComponentInterface, IMPL extends ComponentInterface>
            IFACE createComponent(Class<IFACE> ifaceType, Class<IMPL> implType) {
        return createComponent(ifaceType, implType, null);
    }

    public static <IFACE extends ComponentInterface>
            IFACE createSingletonComponent(Class<IFACE> ifaceType, Object arg) {
        return createComponent(ifaceType, null, arg);
    }

    public static <IFACE extends ComponentInterface, IMPL extends ComponentInterface>
            IFACE createComponent(Class<IFACE> ifaceType, Class<IMPL> implType, Object arg) {
        ComponentDescriptor descriptor = getComponentDescriptor(
                ifaceType, (implType == null) ? null : implType.getName());
        ComponentInstance instance = descriptor.getInstance(null, null);
        try {
            Current.put(Infer.class, arg);
            ComponentSupport.lifecycle(instance, PostCreate.class);
            IFACE i = instance.getInstance(ifaceType);
            i.hashCode(); // force immediate eval so the inferred values get populated.
            // yes, inferring via Current is wrong; inferred should be explicit parameters
            // of the component instance and extracted directly from there. sigh.
            return i;
        } catch (Exception e) {
            throw new RuntimeException("Exception creating new component", e);
        } finally {
            Current.remove(Infer.class);
        }
    }

    public static <IMPL extends ComponentInterface> IMPL newComponent(Class<IMPL> implType) {
        return implType.cast(getComponent(implType.getName(), null).getInstance());
    }

    public static <T extends ComponentInterface> T getInstance(Class<T> iface, String identifier, Long context) {
        final ComponentInstance component = getComponent(identifier, context);
        if (component == null) {
            throw new IllegalArgumentException("No component class could be found for identifier '" + identifier + "' in context " + context);
        }
        return component.getInstance(iface);
    }

    public static ComponentDescriptor lookupComponent(Class<? extends ComponentInterface> iface, Object... keys) {
        ComponentEnvironment env = getEnvironment();
        DelegateDescriptor delegate = env.getRegistry().lookup(iface, keys);
        return (delegate == null) ? null : delegate.getComponent();
    }

    public static <T extends ComponentInterface> T lookup(Class<T> iface, Object key, Object... rest) {
        return lookup(getEnvironment(), iface, key, rest);
    }

    public static <T extends ComponentInterface> T lookup(ComponentEnvironment env, Class<T> iface, Object key, Object... rest) {
        return lookupWithContext(env, iface, key, null, rest);
    }

    public static <T extends ComponentInterface> T lookupWithContext(ComponentEnvironment env, Class<T> iface, Object key, Long context, Object... rest) {
        Object[] keys = ArrayUtils.add(rest, 0, key);
        DelegateDescriptor delegate = env.getRegistry().lookup(iface, keys);
        return (delegate == null) ? null : getInstance(delegate, null, context).getInstance(iface, delegate);
    }

    public static <T extends ComponentInterface> T lookup(Class<T> iface) {
        ComponentEnvironment env = getEnvironment();
        DelegateDescriptor delegate = ObjectUtils.getFirstNonNullIn(env.getRegistry().lookupAll(iface));
        return (delegate == null) ? null : getInstance(delegate, null, null).getInstance(iface, delegate);
    }

    public static <T> T lookupService(final String key) {
        ComponentEnvironment env = getEnvironment();
        return (T) createService(null, env.getRegistry().lookup(Service.class, key), env);
    }

    /**
     * Lookup a service, swallowing any potential exceptions or null results as an Optional.empty().
     *
     * @param clas  The service to look up.
     * @return      Optional(service) if the service exists, Optional.empty if the service can't be found.
     */
    public static <T> java.util.Optional<T> optionalLookupService(final Class<T> clas) {
        // Ugh, ComponentSupport#lookupService throws when the service can't be found, and it's awful.
        try {
            T result = lookupService(clas);
            return java.util.Optional.ofNullable(result);
        } catch (Exception e) {
            logger.warning(String.format("Cannot find service for %s", clas.toString()));
            return java.util.Optional.empty();
        }
    }

    public static <T> T lookupService(final Class<T> clas) {
        return lookupService(getEnvironment(), clas);
    }

    public static <T> T lookupService(ComponentEnvironment env, final Class<T> clas) {
        if (clas.isAnnotationPresent(Local.class)) {
            logger.finer("Since " + clas.getName() + " has annotation " + Local.class.getName() + " we fetched " + "service" + " from ManagedUtils");
            return ManagedUtils.getService(clas);
        }
        if (!clas.isAnnotationPresent(Service.class)) {
            throw new IllegalArgumentException("Not a service: " + clas.getName());
        }
        logger.finer("Since " + clas.getName() + " doesn't have annotation " + Local.class.getName() + " we fetched " + "service" + " from the component registry");
        final ComponentEnvironment env0 = clas.getAnnotation(Service.class).unique()
          ? ComponentManager.getComponentEnvironment() : env;
        return (env0 == null) ? null
          : createService(clas, ObjectUtils.getFirstNonNullIn(env0.getRegistry().lookupAll(clas)), env0);
    }

    private static <T> T createService(@Nullable Class<T> clas, DelegateDescriptor delegate, ComponentEnvironment env) {
        return (delegate == null) ? null : env.getSingletonCache().getService(delegate, clas);
    }

    /**
     * Looks up all implementations of a Service interface
     */
    public static <T> List<T> lookupAllServices(ComponentEnvironment env, final Class<T> iface) {
        return StreamSupport
          .stream(env.getRegistry().lookupAll(iface).spliterator(), false)
          .map(delegate -> createService(iface, delegate, env))
          .collect(Collectors.toList());
    }

    public static <T> List<T> lookupAllServices(Class<T> iface) {
        return lookupAllServices(getEnvironment(), iface);
    }

    public static <T extends ComponentInterface> List<T> lookupAll(Class<T> iface) {
        return lookupAll(getEnvironment(), iface);
    }

    public static <T extends ComponentInterface> List<T> lookupAll(ComponentEnvironment env, Class<T> iface) {
        List<T> list = new ArrayList<>();
        for (DelegateDescriptor delegate : env.getRegistry().lookupAll(iface)) {
            list.add(getInstance(delegate, null, null).getInstance(iface, delegate));
        }
        return list;
    }

    public static <T extends ComponentInterface> List<T> lookupAll(Class<T> iface, Long context) {
        ComponentEnvironment env = getEnvironment();
        List<T> list = new ArrayList<>();
        for (DelegateDescriptor delegate : env.getRegistry().lookupAll(iface)) {
            ComponentInstance instance = getInstance(delegate, null, context);
            try {
                if (instance.checkContext()) {
                    list.add(instance.getInstance(iface, delegate));
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Component error: " + delegate.getComponent().getIdentifier(), ex);
            }
        }
        return list;
    }

    public static <T> Class<? extends T> lookupClass(Class<T> clas, Object... keys) {
        return getEnvironment().getRegistry().lookupClass(clas, keys);
    }

    public static <T> Collection<Class<? extends T>> lookupAllClasses(Class<T> clas) {
        return getEnvironment().getRegistry().lookupAllClasses(clas);
    }

    /**
     * Creates a new instance of the specified clas, DIing fields and constructor parameters as necessary.
     * The specified args, if any, will be passed as the initial args to the constructor; the
     * remainder must be available via DI.
     */
    public static <T> T newInstance(Class<T> clas, Object... args) {
        return newInstance(getEnvironment(), clas, args);
    }

    public static <T> T newInstance(ComponentEnvironment env, Class<T> clas, Object... args) {
        // TODO: use a more-lightweight mechanism for DI than ComponentDescriptor...
        return clas.cast(env.getRegistry().getInstanceDescriptor(clas).getInstance(env, null, null, args).getInstance());
    }

    public static ObjectMapper getObjectMapper() {
        return java.util.Optional.ofNullable(getEnvironment()).map(ComponentEnvironment::getObjectMapper).orElse(JacksonUtils.getMapper());
    }

    /**
     * Load a class by name using the current environment class loader.
     *
     * @param fullyQualifiedClassName
     */
    public static Class<?> loadClass(String fullyQualifiedClassName) {
        try {
            return Class.forName(fullyQualifiedClassName, true, getEnvironment().getClassLoader());
        } catch (Throwable t) {
            throw Throwables.propagate(t);
        }
    }

    public static boolean hasClass(String fullyQualifiedClassName) {
        try {
            loadClass(fullyQualifiedClassName);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Lookup a resource expressed by one of the components in the current {@link BaseComponentEnvironment}. The resource is keyed by the annotation
     * that describes the resource, which may or may not be based on the component hosting the annotation (it depends on the
     * {@link ResourceRegistry} implementation).
     *
     * @param annotationType the annotation {@link Class} that describes the desired resource.
     * @param resourceType   the non-type-erased {@code T} class to which the {@link ResourceRegistry#lookup} is casted.
     * @param keys           the keys to give to the {@link ResourceRegistry} keyed by {@code annotationType}.
     * @param <T>            the type that the {@link ResourceRegistry} keyed by {@code annotationType} processes the
     *                       resource into.
     * @return a resource expressed by one of the components in this registry
     */
    public static <T> T lookupResource(final Class<? extends Annotation> annotationType, final Class<T> resourceType, final Object... keys) {
        return getEnvironment().getRegistry().lookupResource(annotationType, resourceType, keys);
    }

    public static <T> Iterable<T> lookupAllResources(final Class<? extends Annotation> annotationType, final Class<T> resourceType) {
        return getEnvironment().getRegistry().lookupAllResources(annotationType, resourceType);
    }

    private static ComponentInstance getInstance(DelegateDescriptor delegate, IdType info, Long context) {
        ComponentDescriptor component = delegate.getComponent();
        if (component.isStateless()) {
            return getEnvironment().getSingletonCache().getComponent(component);
        } else {
            return component.getInstance(info, context);
        }
    }

    public static ComponentInstance getComponent(ComponentInterface proxy) {
        return proxy.getComponentInstance();
        // ComponentInvocationHandler handler = (ComponentInvocationHandler) Proxy.getInvocationHandler(proxy);
        // return handler.getInstance();
    }

    /**
     * @return the id of the component proxy by reading it off the proxy's {@link ComponentInvocationHandler}.
     */
    @Nullable
    public static Long getId(@Nullable final ComponentInterface proxy) {
        if (proxy == null) {
            return null;
        } else {
            final ComponentInstance component = getComponent(proxy);
            return component.getId();
        }
    }

    public static DelegateDescriptor getDelegate(ComponentInterface proxy) {
        ComponentInvocationHandler handler = (ComponentInvocationHandler) Proxy.getInvocationHandler(proxy);
        return handler.getDelegate();
    }

    public static <T extends ComponentInterface> T getInstance(Class<T> iface, ComponentInstance instance,
                                                               DelegateDescriptor delegate) {

        final ComponentInvocationHandler h = new ComponentInvocationHandler(delegate, instance);

        final Set<Class<?>> delegateInterfaces = BreadthFirstSupertypeIterable
          .from(delegate.getDelegateClass())
          .filter(clazz -> clazz != null && clazz.isInterface())
          .collect(Collectors.toSet());

        final Set<Class<?>> ifaces = SetUtils.union(delegateInterfaces, singleton(iface));
        final Class<?>[] interfaces = ifaces.toArray(new Class<?>[0]);

        return iface.cast(Proxy.newProxyInstance(delegate.getDelegateClass().getClassLoader(), interfaces, h));
    }

    /**
     * Returns the narrowest (most specific, or "leafiest") {@link ComponentInterface} type of the given component.
     * This method is most useful for {@link ComponentInvocationHandler}-based proxies,
     * because this method may downcast all the way to the first {@link ComponentInterface} declared by the proxy's
     * {@link DelegateDescriptor} class.
     *
     * @param component the component for which the narrowest type is returned
     * @return the narrowest {@link ComponentInterface} type of the given component
     * @see BreadthFirstSupertypeIterable
     * @see ComponentUtils#findAnnotation(ComponentInterface, Class)
     */
    public static Class<? extends ComponentInterface> getNarrowestComponentInterface(@Nonnull final ComponentInterface component) {
        final Class<?> componentClass;
        if (isComponentInvocationHandlerBasedProxy(component)) {
            componentClass = ComponentSupport.getDelegate(component).getDelegateClass();
        } else {
            componentClass = component.getClass();
        }
        return getNarrowestComponentInterface(componentClass);
    }

    public static Class<? extends ComponentInterface> getNarrowestComponentInterface(@Nonnull final Class<?> componentClass) {
        // get the first interface narrowestType declares that is a ComponentInterface
        return BreadthFirstSupertypeIterable.from(componentClass)
          .filter(type -> type.isInterface() && ComponentInterface.class.isAssignableFrom(type) && type != ComponentImplementation.class)
          .reduce((a, b) -> a.isAssignableFrom(b) ? b : a)
          .orElse(ComponentInterface.class)
          .asSubclass(ComponentInterface.class);
    }

    private static boolean isComponentInvocationHandlerBasedProxy(final Object object) {
        return Proxy.isProxyClass(object.getClass()) && Proxy.getInvocationHandler(object) instanceof
                ComponentInvocationHandler;
    }

    public static <T extends Annotation> T getBinding(ComponentInterface proxy, Class<? extends ComponentInterface> iface) {
        return (T) getDelegate(proxy).getBinding(iface);
    }

    public static TagInstance getTag(String identifier, String name) {
        ComponentInstance instance = getComponent(identifier, null);
        return getFunction(TagInstance.class, instance, name);
    }

    public static RpcInstance lookupRpc(String binding) {
        ComponentEnvironment env = getEnvironment();
        FunctionDescriptor function = env.getRegistry().lookupFunction(RpcInstance.class, binding);
        if (function == null) { // TODO: FIXME: Crude support for binding /path/*
            int index = binding.lastIndexOf('/');
            String binding2 = binding.substring(0, 1 + index) + '*';
            function = env.getRegistry().lookupFunction(RpcInstance.class, binding2);
        }
        if (function == null) {
            return null;
        }
        ComponentInstance instance = getInstance(function.getDelegate(), null, null);
        return instance.getFunctionInstance(RpcInstance.class, function);
    }

    public static FnInstance getFn(String identifier, String name) {
        return getFunction(FnInstance.class, getComponent(identifier, null), name);
    }

    public static RpcInstance getRpc(String identifier, String method, String name) {
        return getFunction(RpcInstance.class, getComponent(identifier, null), method, name);
    }

    public static RpcInstance getRpc(IdType info, String method, String name) {
        return getFunction(RpcInstance.class, getComponent(info, null), method, name);
    }

    public static <T extends AbstractFunctionInstance> T getFunction(Class<T> type, ComponentInstance instance, Object... keys) {
        return instance.getFunctionInstance(type, keys);
    }

    public static boolean isComponentType(String type) {
        ComponentEnvironment env = getEnvironment();
        return env.getRegistry().getItemTypes().contains(type);
    }

    public static <T> void lifecycle(final ComponentInstance instance, final Class<? extends Annotation> phase, final Class<T> key, final T value) {

        try {
            for (Class<?> k = key; (k != null) && (k != Object.class); k = k.getSuperclass()) {
                Current.put(k, value);
            }
            ComponentSupport.lifecycle(instance, phase);
        } catch (final Exception ex) {
            throw Throwables.propagate(ex);
        } finally {
            for (Class<?> k = key; (k != null) && (k != Object.class); k = k.getSuperclass()) {
                Current.remove(k);
            }
        }
    }

    public static void lifecycle(ComponentInstance instance, Class<? extends Annotation> phase) throws Exception {
        if (phase.isAnnotationPresent(ComponentLifecycle.class)) {
            instance.getComponent().getDelegate().lifecycle(phase);
        }
        List<FunctionDescriptor> functions = new ArrayList<>(instance.getEnvironment().getRegistry().getFunctions(instance.getComponent(), LifecycleInstance.class));
        Collections.reverse(functions);
        for (FunctionDescriptor function : functions) {
            if (function.getAnnotationClass() == phase) {
                LifecycleInstance lifecycle = instance.getFunctionInstance(LifecycleInstance.class, function);
                lifecycle.invoke();
            }
        }
    }

    /**
     * Return the data mappings (the Constants strings) for the given
     * plain-old-java-object type. Returns an empty Map if the given class has no {@link
     * Queryable}s.
     *
     * @return the data mappings (the Constants strings) for the given
     * plain-old-java-object type.
     * @see Queryable
     */
    @Nonnull
    public static Map<String, Queryable> getPojoDataMappings(
            @Nonnull final Class<?> clazz) {
        ComponentEnvironment env = getEnvironment();
        return env.getRegistry().getPojoDataMappings(clazz);
    }

    @Nullable
    public static String getSchemaName(@Nonnull ComponentInterface component) {
        final java.util.Optional<Schema> schemaAnnotation = ComponentUtils.findAnnotation(component, Schema.class);

        //TODO: Use functional combinators with Optional instead, orElseGet instead of isPresent/value
        if (schemaAnnotation.isPresent()) {
            return schemaAnnotation.get().value();
        } else {
            final ComponentInstance componentInstance = component.getComponentInstance();
            if (componentInstance == null) {
                return null; // synthetic instances.. hmm..
            }
            return componentInstance.getIdentifier();
        }
    }

    @Nullable
    public static String getSchemaName(@Nonnull ComponentDescriptor component) {
        return ClassUtils.findAnnotation(component.getCategory(), Schema.class).map(Schema::value).orElse(component.getIdentifier());
    }

    /**
     * Uses the given facade's component identifier to convert the given facade to a
     * component.
     */
    @Nonnull
    public static <C extends ComponentInterface> C facadeToComponent(
            @Nonnull final Facade facade, @Nonnull final Class<C> componentInterfaceClass) {
        // Guava's insists that all Functions return Nullables, well this one doesn't
        //noinspection ConstantConditions
        if (facade == null) {
            return null;
        } else {
            final ComponentFacade componentFacade = facade.asFacade(ComponentFacade.class);
            //noinspection ConstantConditions
            return ComponentSupport.getInstance(componentInterfaceClass, componentFacade, null);
        }
    }

    public static <S extends Id, T extends ComponentInterface> Function<S, T> toComponent(final Class<T> iface) {
        return new Function<S, T>() {
            @Override
            public T apply(S id) {
                return get(Ids.get(id), iface);
            }
        };
    }

    public static <S extends ComponentInterface, T extends ComponentInterface> Function<S, T> asComponent(final Class<T> iface, final Object... args) {
        return new Function<S, T>() {
            @Override
            public T apply(S c) {
                return c.asComponent(iface, args);
            }
        };
    }


}


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

package com.learningobjects.cpxp.component.registry;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.learningobjects.cpxp.CpxpClasspath;
import com.learningobjects.cpxp.component.BaseComponentDescriptor;
import com.learningobjects.cpxp.component.ComponentDescriptor;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.annotation.Component;
import com.learningobjects.cpxp.component.annotation.ItemMapping;
import com.learningobjects.cpxp.component.annotation.Service;
import com.learningobjects.cpxp.component.function.*;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.cpxp.util.MultiKey;
import com.learningobjects.cpxp.util.StringUtils;
import com.learningobjects.de.web.Queryable;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * This registry allows component implementations of component interfaces to be bound under particular names and paths;
 * allowing lookup, for example, of components that support a particular quiz type or request URL.
 */
public class BaseComponentRegistry implements ComponentRegistry,ComponentRegistryMaps {
    private final Map<String, Class<? extends ComponentInterface>> _types = new HashMap<>();
    private final Map<Class<?>, Registry<Annotation, DelegateDescriptor>> _registries = new HashMap<>();
    private final Map<Class<?>, Registry<Annotation, Class<?>>> _classRegistries = new HashMap<>();
    private final Map<MultiKey, FunctionDescriptor> _functions = new HashMap<>();
    private final Map<ComponentDescriptor, Map<Class<?>, FunctionRegistry>> _functionRegistriess = new HashMap<>();

    /**
     * A lazily instantiated index of data mappings by POJO type. Usually the POJO type is
     * a {@link Component} type.
     *
     * @see Queryable
     */
    private final LoadingCache<Class<?>, Map<String, Queryable>> _pojoDataMappings =
            CacheBuilder.newBuilder().weakKeys().build(new QueryableIntrospector());

    /** A lazily instantiated cache of component descriptors for non-component
     * classes. This is used to enable DI during instance creation.
     */
    private final LoadingCache<Class<?>, ComponentDescriptor> _instanceDescriptors =
            CacheBuilder.newBuilder().weakKeys().build(new InstanceIntrospector());

    /**
     * The union of all the {@link ResourceRegistry}-ies of all the components in this registry.
     */
    private final ResourceRegistryContainer _resourceRegistryContainer = new BaseResourceRegistryContainer();

    public BaseComponentRegistry() {
        initFunctionRegistry();
    }

    /**
     * Bind all function registries now so BaseComponentDescriptor#linkComponent works.
     */
    private void initFunctionRegistry() {
        CpxpClasspath.classGraph().getClassesWithAnnotation(FunctionBinding.class)
          .loadClasses()
          .forEach(fn -> {
              FunctionBinding fb = fn.getAnnotation(FunctionBinding.class);
              registerClass(FunctionInstance.class, fb, fn);
          });
    }

    @Override
    public FunctionDescriptor getFunction(ComponentDescriptor component, Class<? extends FunctionInstance> type, Object... keys) {
        Map<Class<?>, FunctionRegistry> registries = _functionRegistriess.get(component);
        if (registries != null) {
            return (registries.get(type) == null) ? null : registries.get(type).lookup(keys);
        } else {
            return null;
        }
    }

    @Override
    public Collection<FunctionDescriptor> getFunctions(ComponentDescriptor component, Class<? extends FunctionInstance> type) {
        Map<Class<?>, FunctionRegistry> registries = _functionRegistriess.get(component);
        if (registries != null) {
            return (registries.get(type) == null) ? Collections.emptySet() : registries.get(type).lookupAll();
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public void register(ComponentDescriptor component) {
        Map<Class<?>, FunctionRegistry> functionRegistries =
          _functionRegistriess.computeIfAbsent(component, k -> new HashMap<>());
        for (FunctionDescriptor function : component.getFunctionDescriptors()) {
            Class<? extends Annotation> annoClass = function.getAnnotationClass();
            Function functionAnno = annoClass.getAnnotation(Function.class);
            Class<? extends FunctionInstance> functionType = functionAnno.value();
            if (functionType == FunctionInstance.class) {
                /* NOTE WELL: For this value to be omitable, the FunctionInstance must be declared
                 * in component land, or else the instance type will need to be manually bound into
                 * the component environment registry in order for ComponentDescriptor#linkComponent
                 * to be able to function. */
                functionType = lookupClass(FunctionInstance.class, annoClass);
                if (functionType == null) {
                    throw new RuntimeException("Unable to find function instance for annotation class: " + annoClass.getName());
                }
            }
            try {
                FunctionRegistry registry = functionRegistries.get(functionType);
                if (registry == null) {
                    FunctionBinding binding = functionType.getAnnotation(FunctionBinding.class);
                    if (binding == null) {
                        registry = new DefaultFunctionRegistry();
                    } else {
                        registry = binding.registry().newInstance();
                    }
                    functionRegistries.put(functionType, registry);
                }
                registry.register(function);
            } catch (Exception e) {
                throw new RuntimeException("Error adding function: " + function, e);
            }
        }
        // This is an inefficient way to enumerate the item mappings but it
        // seems like the easiest. I suspect that the item mappings should
        // be listed in the actual @ItemTypedef instead.
        for (Class<? extends ComponentInterface> iface : component.getInterfaces()) {
            ItemMapping mapping = iface.getAnnotation(ItemMapping.class);
            if (mapping != null) {
                _types.put(mapping.value(), iface);
            }
        }
        DelegateDescriptor delegate = component.getDelegate();
        for (final Map.Entry<Class<? extends ComponentInterface>,
                Annotation> entry : delegate.getBindings().entrySet()) {

            final Class<? extends ComponentInterface> binder = entry.getKey();
            final Annotation annotation = entry.getValue();

            register(binder, annotation, delegate);
        }
        if (delegate.isService() || !delegate.getServiceInterfaces().isEmpty()) {
            Registry<Annotation, DelegateDescriptor> registry = new SingletonRegistry<>();
            registry.register(null, delegate);
            _registries.put(delegate.getDelegateClass(), registry);
            ServiceBinding binding = delegate.getDelegateClass().getAnnotation(ServiceBinding.class);
            if (binding != null) {
                register(Service.class, binding, delegate);
            }
            for (Class<?> i : delegate.getServiceInterfaces()) {
                _registries.put(i, registry);
            }
        }
        for (Map.Entry<Class<?>, FunctionRegistry> entry : functionRegistries.entrySet()) {
            Class<?> type = entry.getKey();
            for (FunctionDescriptor function : entry.getValue().lookupAll()) {
                if (StringUtils.isNotEmpty(function.getBinding())) {
                    _functions.put(new MultiKey(type, function.getBinding()), function);
                }
            }
        }

        // merge component descriptor's resource registry into the environment's
        BaseResourceRegistryContainer thisBase = _resourceRegistryContainer instanceof BaseResourceRegistryContainer ?
                    (BaseResourceRegistryContainer) _resourceRegistryContainer :
                    null;
        BaseResourceRegistryContainer otherBase = component.getResourceRegistryContainer() instanceof  BaseResourceRegistryContainer ?
                    (BaseResourceRegistryContainer) component.getResourceRegistryContainer() :
                    null;
        if(thisBase != null && otherBase != null) {
            thisBase.merge(otherBase);
        } else {
            throw new RuntimeException("Unable to merge RegistryContainers");
        }
    }

    @SuppressWarnings("unchecked")
    private void register(Class<?> iface, Annotation annotation, DelegateDescriptor delegate) {
        try {
            Registry<Annotation, DelegateDescriptor> registry = _registries.get(iface);
            if (registry == null) {
                registry = initRegistry(iface, annotation, _registries);
            }
            registry.register(annotation, delegate);
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    private <T> Registry<Annotation, T> initRegistry(Class<?> iface, Annotation annotation, Map<Class<?>, Registry<Annotation, T>> target) throws InstantiationException, IllegalAccessException {
        Binding binding = annotation.annotationType().getAnnotation(Binding.class);
        Registry<Annotation, T> registry = binding.registry().newInstance();
        registry.init(this);
        target.put(iface, registry);
        return registry;
    }

    @Override
    public Map<String, Queryable> getPojoDataMappings(final Class<?> clazz) {
        return _pojoDataMappings.getUnchecked(clazz);
    }

    @Override
    public Set<String> getItemTypes() {
        return _types.keySet();
    }

    @Override
    public Class<? extends ComponentInterface> getItemComponent(String type) {
        return _types.get(type);
    }

    @Override
    public DelegateDescriptor lookup(Class<?> clas, Object... keys) {
        Registry<Annotation, DelegateDescriptor> registry = _registries.get(clas);
        return (registry == null) ? null : registry.lookup(keys);
    }

    @Override
    public Iterable<DelegateDescriptor> lookupAll(Class<?> clas) {
        Registry<Annotation, DelegateDescriptor> registry = _registries.get(clas);
        if (registry == null) {
            return Collections.emptySet();
        }
        return registry.lookupAll();
    }

    @Override
    public FunctionDescriptor lookupFunction(Class<? extends FunctionInstance> type, String name) {
        return _functions.get(new MultiKey(type, name));
    }

    @Override
    public void registerClass(Class<?> clas) {
        for (Annotation a : clas.getDeclaredAnnotations()) {
            Binding binding = a.annotationType().getAnnotation(Binding.class);
            if ((binding != null) && !binding.component()) {
                Class<?> aClass = a.annotationType();
                Class<?> base = findBase(clas, aClass);
                registerClass(base, a, clas);
            }
        }
    }

    private Class<?> findBase(Class<?> clas, Class<?> bindingClass) {
        LinkedList<Class<?>> list = new LinkedList<Class<?>>();
        list.add(clas);
        do {
            Class<?> base = list.removeFirst();
            Bound bound = base.getAnnotation(Bound.class);
            if ((bound != null) && bindingClass.equals(bound.value())) {
                return base;
            }
            for (Class<?> iface : base.getInterfaces()) {
                list.add(iface);
            }
            base = base.getSuperclass();
            if (base != null) {
                list.add(base);
            }
        } while (!list.isEmpty());
        throw new RuntimeException("No binding base found: " + clas + " / " + bindingClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerClass(Class<?> base, Annotation annotation, Class<?> impl) {
        try {
            Registry<Annotation, Class<?>> registry = _classRegistries.get(base);
            if (registry == null) {
                registry = initRegistry(base, annotation, _classRegistries);
            }
            registry.register(annotation, impl);
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    @Override
    public <T> Class<? extends T> lookupClass(Class<T> clas, Object... keys) {
        Registry<Annotation, Class<?>> registry = _classRegistries.get(clas);
        return (registry == null) ? null : (Class<T>) registry.lookup(keys);
    }

    @Override
    public <T> Collection<Class<? extends T>> lookupAllClasses(Class<T> clas) {
        Registry<Annotation, Class<?>> registry = _classRegistries.get(clas);
        if (registry == null) {
            return Collections.emptyList();
        }
        return (Collection<Class<? extends T>>) (Collection) registry.lookupAll(); // this is immoral, i could iterate through and add to a new list... but this is more efficient
    }

    @Override
    public void registerResource(final Annotation a, final Class<?> clas) {
        _resourceRegistryContainer.register(a, clas);
    }

    /**
     * Lookup a resource expressed by one of the components in this registry. The resource is keyed by the annotation
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
    @Override
    public <T> T lookupResource(final Class<? extends Annotation> annotationType, final Class<T> resourceType,
                                final Object... keys) {
        return resourceType.cast(_resourceRegistryContainer.getRegistry(annotationType).lookup(keys));
    }

    @Override
    public <T> Iterable<T> lookupAllResources(final Class<? extends Annotation> annotationType, final Class<T> resourceType) {

        // ResourceRegistryContainer is all f'ed up, casting for now
        final ResourceRegistry<? extends Annotation> registry = _resourceRegistryContainer.getRegistry(annotationType);
        return (Iterable<T>) registry.lookupAll();
    }

    @Override
    public ComponentDescriptor getInstanceDescriptor(Class<?> clas) {
        return _instanceDescriptors.getUnchecked(clas);
    }

    private static class InstanceIntrospector extends CacheLoader<Class<?>, ComponentDescriptor> {
        @Override
        public ComponentDescriptor load(Class<?> aClass) {
            return new BaseComponentDescriptor(aClass, null);
        }
    }

    @Override
    public Map<Class<?>, Registry<Annotation, Class<?>>> getClassRegistry() {
        return Collections.unmodifiableMap(_classRegistries);
    }

    @Override
    public Map<MultiKey, FunctionDescriptor> getFunctionRegistry() {
        return Collections.unmodifiableMap(_functions);
    }

    @Override
    public Map<Class<?>, Registry<Annotation, DelegateDescriptor>> getRegistryRegistry() {
        return Collections.unmodifiableMap(_registries);
    }

    @Override
    public Map<String, Class<? extends ComponentInterface>> getTypeRegistry() {
        return Collections.unmodifiableMap(_types);
    }
}

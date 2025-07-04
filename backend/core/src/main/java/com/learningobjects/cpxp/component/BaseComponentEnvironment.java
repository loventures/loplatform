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
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.learningobjects.cpxp.component.registry.BaseComponentRegistry;
import com.learningobjects.cpxp.component.registry.ComponentRegistry;
import com.learningobjects.cpxp.component.util.ComponentUtils;
import com.learningobjects.cpxp.component.util.ConfigUtils;
import com.learningobjects.cpxp.component.web.util.JacksonUtils;
import com.learningobjects.cpxp.util.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BaseComponentEnvironment implements ComponentEnvironment {
    private static final Logger logger = Logger.getLogger(BaseComponentEnvironment.class.getName());

    private final ComponentCollection _collection;
    private ComponentEnvironment _prior;
    private final ComponentEnvironmentClassLoader _classLoader;
    private final ObjectMapper _objectMapper;
    private final Map<Class<?>, Object> _attributes;

    private ComponentRing _ring;

    public BaseComponentEnvironment(ComponentCollection collection, ComponentEnvironment prior) {
        _collection = collection;
        _prior = prior;
        //TODO: Here we need to be descendant of the WebappClassLoader if in a servlet context to properly get JNDI support from tomcat.
        _classLoader = new ComponentEnvironmentClassLoader(this, Thread.currentThread().getContextClassLoader());
        _objectMapper = JacksonUtils.newEnvironmentMapper(_classLoader);
        _attributes = new ConcurrentHashMap<>(8, 0.9f, 1);
    }

    @Override
    public String getIdentifier() {
        return _collection.getIdentifier();
    }

    @Override
    public ComponentCollection getCollection() {
        return _collection;
    }

    @Override
    public ClassLoader getClassLoader() {
        return _classLoader;
    }

    /**
     * Use this mapper to deserialize in the context of this component environment
     * to prevent classloader leaks from a globally shared object mapper. This is
     * configured to materialize known interfaces.
     */
    @Override
    public ObjectMapper getObjectMapper() {
        return _objectMapper;
    }

    @Override
    public ComponentRing getRing() {
        return _ring;
    }

    @Override
    public Iterable<ComponentArchive> getArchives() {
        return (_ring == null) ? Collections.<ComponentArchive>emptyList() : _ring.getArchives();
    }

    @Override
    public Iterable<ComponentArchive> getAvailableArchives() {
        return Iterables.filter(getArchives(), new Predicate<ComponentArchive>() {
            @Override public boolean apply(ComponentArchive archive) {
                return BooleanUtils.toBooleanDefaultIfNull(_collection.getArchiveEnabled(archive.getIdentifier()), archive.getArchiveAnnotation().available());
            }
        });
    }

    @Override
    public synchronized boolean load() {
        if (_ring != null) {
            return false;
        }
        if (_prior != null) {
            // Wait for the previous environment to finish loading before loading this;
            // otherwise both environments can bootstrap concurrently if the server is
            // hit with a new component install while still bootstrapping.
            _prior.load();
            _prior = null;
        }
        logger.info("Building script environment: " + _collection.getIdentifier());
        _ring  = new BaseComponentRing(ComponentManager.getComponentRing());
        for (ComponentSource src : _collection.getSources()) {
            _ring.addArchive(new BaseComponentArchive(src));
        }
        _ring.load();
        Set<String> suppressed = new HashSet<>();
        Set<String> dependents = new HashSet<>();
        for (ComponentArchive archive : getAvailableArchives()) {
            for (Class<?> clas : archive.getArchiveAnnotation().suppresses()) {
                suppressed.add(clas.getName());
            }
            // I have to do this early because later components often suppress early components
            // but this obviously means that a suppressed component's suppressions still apply
            // unless it is explicitly turned off at the domain level.
            for (ComponentDescriptor component : archive.getComponents()) {
                if (BooleanUtils.toBooleanDefaultIfNull(_collection.getComponentEnabled(component.getIdentifier()), component.getComponentAnnotation().enabled())) {
                    for (Class<?> clas : component.getComponentAnnotation().suppresses()) {
                        suppressed.add(clas.getName());
                    }
                    if (!component.getComponentAnnotation().implementation().isEmpty()) {
                        /* virtual components implicitly suppress their implementation.
                         * this is okay because the implementation class will still get
                         * a DelegateDescriptor owned by the virtual component instead. */
                        suppressed.add(component.getComponentAnnotation().implementation());
                    }
                    for (Class<?> clas : component.getComponentAnnotation().dependencies()) {
                        dependents.add(clas.getName());
                    }
                }
            }
        }
        for (ComponentArchive archive : getAvailableArchives()) {
            for (Class<?> clas : archive.getBoundClasses()) {
                _registry.registerClass(clas);
            }
            for (final Pair<Annotation, Class<?>> pair : archive.getResourceBoundClasses()) {
                _registry.registerResource(pair.getLeft(), pair.getRight());
            }
        }
        for (ComponentArchive archive : getAvailableArchives()) {
            for (ComponentDescriptor component : archive.getComponents()) {
                if (!suppressed.contains(component.getIdentifier()) &&
                  (dependents.contains(component.getIdentifier())
                    || BooleanUtils.toBooleanDefaultIfNull(_collection.getComponentEnabled(component.getIdentifier()), component.getComponentAnnotation().enabled()))) {
                    loadComponent(component);
                }
            }
        }
        return true;
    }

    @Override
    public void loadComponent(ComponentDescriptor component) {
        try {
            for (String alias : component.getComponentAnnotation().alias()) {
                _aliases.put(alias, component.getIdentifier());
            }
            if (!component.getComponentAnnotation().implementation().isEmpty()) {
                /* serving up an extra helping of gruefulness for the ring 0 virtual components */
                _aliases.put(component.getComponentAnnotation().implementation(), component.getIdentifier());
            }
            _components.put(component.getIdentifier(), component);
            _registry.register(component);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error loading: " + component.getIdentifier(), ex);
            throw Throwables.propagate(ex);
        }
    }

    private final Map<String, ComponentDescriptor> _components = new HashMap<>();
    private final Map<String, String> _aliases = new HashMap<>();
    private final Map<String, List<ComponentDescriptor>> _componentsByInterface = new HashMap<>();
    private final ComponentRegistry _registry = new BaseComponentRegistry();
    private final SingletonCache _singletonCache = new BaseSingletonCache(this);

    @Override
    public Collection<ComponentDescriptor> getComponents() {
        return _components.values();
    }

    @Override
    public Set<String> getIdentifiers() {
        return _components.keySet();
    }

    @Override
    public ComponentRegistry getRegistry() {
        return _registry;
    }

    @Override
    public <T> T getComponentConfiguration(String identifier, String name, Class<T> type) {
        return type.cast(ConfigUtils.decodeValues(getComponentConfiguration(identifier).get(name), type));
    }

    @Override
    public Map<String, String[]> getComponentConfiguration(String identifier) {
        return ConfigUtils.decodeConfiguration(getCollection().getComponentConfiguration(identifier));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getJsonConfiguration(String identifier) {
        return (Map<String, Object>) getJsonConfiguration(identifier, Map.class).orElse(Collections.emptyMap());
    }

    @Override
    public <T> Optional<T> getJsonConfiguration(String identifier, Class<T> type) {
        try {
            String config = getCollection().getComponentConfiguration(identifier);
            return StringUtils.isEmpty(config) ? Optional.empty() : Optional.of(ComponentUtils.fromJson(config, type));
        } catch (Exception ex) {
            throw new RuntimeException("Json config error: " + identifier + " / " + type, ex);
        }
    }

    @Override
    public boolean hasComponent(String identifier) {
        String key = StringUtils.defaultString(_aliases.get(identifier), identifier);
        return _components.containsKey(key);
    }

    @Override
    public ComponentDescriptor getComponent(String identifier) {
        String key = StringUtils.defaultString(_aliases.get(identifier), identifier);
        return _components.get(key);
    }

    @Override
    public List<ComponentDescriptor> getComponents(Class<? extends ComponentInterface> iface) {
        List<ComponentDescriptor> components;
        synchronized (_componentsByInterface) {
            // I use a Map instead of a Multimap because it lets me lazily populate it
            components = _componentsByInterface.get(iface.getName());
            if (components == null) { // This is somewhat horrendous...
                components = new ArrayList<>();
                for (ComponentDescriptor component : getComponents()) {
                    if (component.isSupported(iface)) {
                        components.add(component);
                    }
                }
                _componentsByInterface.put(iface.getName(), components);
            }
        }
        return components;
    }

    @Override
    public <T> void setAttribute(Class<T> key, T value) {
        _attributes.put(key, value);
    }

    @Override
    public <T> T getAttribute(Class<T> key) {
        return key.cast(_attributes.get(key));
    }

    @Override
    public <T> T getAttribute(Class<T> key, Supplier<T> supplier) {
        return key.cast(_attributes.computeIfAbsent(key, k -> supplier.get()));
    }

    @Override
    public SingletonCache getSingletonCache() {
        return _singletonCache;
    }

}

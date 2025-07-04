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
import com.learningobjects.cpxp.component.registry.ComponentRegistry;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ComponentEnvironment {
    String getIdentifier();

    ComponentCollection getCollection();

    ClassLoader getClassLoader();

    /**
     * Use this mapper to deserialize in the context of this component environment
     * to prevent classloader leaks from a globally shared object mapper. This is
     * configured to materialize known interfaces.
     */
    ObjectMapper getObjectMapper();

    ComponentRing getRing();

    Iterable<ComponentArchive> getArchives();

    Iterable<ComponentArchive> getAvailableArchives();

    boolean load();

    void loadComponent(ComponentDescriptor component);

    Collection<ComponentDescriptor> getComponents();

    Set<String> getIdentifiers();

    ComponentRegistry getRegistry();

    <T> T getComponentConfiguration(String identifier, String name, Class<T> type);

    Map<String, String[]> getComponentConfiguration(String identifier);

    Map<String, Object> getJsonConfiguration(String identifier);

    <T> Optional<T> getJsonConfiguration(String identifier, Class<T> type);

    boolean hasComponent(String identifier);

    ComponentDescriptor getComponent(String identifier);

    List<ComponentDescriptor> getComponents(Class<? extends ComponentInterface> iface);

    <T> void setAttribute(Class<T> key, T value);

    <T> T getAttribute(Class<T> key);

    <T> T getAttribute(Class<T> key, Supplier<T> supplier);

    /* private[component] */
    SingletonCache getSingletonCache();
}

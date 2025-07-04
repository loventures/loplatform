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

import com.learningobjects.cpxp.component.ComponentDescriptor;
import com.learningobjects.cpxp.component.ComponentInterface;
import com.learningobjects.cpxp.component.function.FunctionInstance;
import com.learningobjects.cpxp.component.internal.DelegateDescriptor;
import com.learningobjects.cpxp.component.internal.FunctionDescriptor;
import com.learningobjects.de.web.Queryable;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface ComponentRegistry {
    void register(ComponentDescriptor component);

    Map<String, Queryable> getPojoDataMappings(Class<?> clazz);

    Set<String> getItemTypes();

    Class<? extends ComponentInterface> getItemComponent(String type);

    DelegateDescriptor lookup(Class<?> clas, Object... keys);

    Iterable<DelegateDescriptor> lookupAll(Class<?> clas);

    FunctionDescriptor lookupFunction(Class<? extends FunctionInstance> type, String name);

    FunctionDescriptor getFunction(ComponentDescriptor component, Class<? extends FunctionInstance> type, Object... keys);

    Collection<FunctionDescriptor> getFunctions(ComponentDescriptor component, Class<? extends FunctionInstance> type);

    void registerClass(Class<?> clas);

    void registerClass(Class<?> base, Annotation annotation, Class<?> impl);

    <T> Class<? extends T> lookupClass(Class<T> clas, Object... keys);

    <T> Collection<Class<? extends T>> lookupAllClasses(Class<T> clas);

    ComponentDescriptor getInstanceDescriptor(Class<?> clas);

    void registerResource(final Annotation a, final Class<?> clas);

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
    <T> T lookupResource(Class<? extends Annotation> annotationType, Class<T> resourceType,
                         Object... keys);

    <T> Iterable<T> lookupAllResources(Class<? extends Annotation> annotationType, Class<T> resourceType);
}

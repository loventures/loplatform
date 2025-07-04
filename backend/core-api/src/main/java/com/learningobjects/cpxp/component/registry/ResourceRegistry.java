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

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * Registers artifacts expressed by classes in the component environment. Where a
 * {@link Registry} can only register either classes or delegate descriptors, a
 * {@link ResourceRegistry} can register objects of any type.
 *
 * @see ResourceBinding
 * @param <T> the type of object that this registry holds
 */
public interface ResourceRegistry<T> {

    /**
     * Register an artifact from the annotation and class. The given annotation is what
     * told the component framework to register {@code clazz}. The annotation is itself
     * marked with {@link ResourceBinding}
     *
     * @param annotation an annotation that describes a resource to make available as
     * type {@code T}.
     * @param clazz the host of the annotation, can be used to help
     * produce and/or later retrieve the artifact of type T
     */
    void register(final Annotation annotation, final Class<?> clazz);


    /**
     * @param keys input to find the artifact
     * @return an artifact
     */
    T lookup(final Object... keys);

    /**
     * @return all artifacts in this registry
     */
    Collection<T> lookupAll();

    /**
     * Combine this registry with another one, usually one from another
     * {@link ComponentDescriptor}.
     *
     * @param registry the registry to merge into this one.
     */
    void merge(final ResourceRegistry<T> registry);

}

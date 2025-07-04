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
import com.learningobjects.cpxp.component.BaseComponentDescriptor;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * FIXME this class is completely messed up, ResourceRegistries are higher order types of a resource type (such as a
 * schema registration), not a higher order type of the annotation type
 *
 *
 * A typesafe heterogeneous container of all the {@link ResourceRegistry}-ies expressed by a {@link
 * BaseComponentDescriptor}, keyed by the
 * annotation that describes the resources. See {@link ResourceRegistry}.
 */
public class BaseResourceRegistryContainer implements ResourceRegistryContainer {

    @Nonnull
    final Map<Class<? extends Annotation>, ResourceRegistry<?>> registryMap = new HashMap<>();

    @Override
    @Nonnull
    public <T extends Annotation> ResourceRegistry<T> getRegistry(@Nonnull final Class<T> annotationType) {

        final ResourceRegistry<T> registry;
        if (registryMap.containsKey(annotationType)) {
            registry = get(annotationType);
        } else {
            registry = instantiateRegistry(annotationType);
            registryMap.put(annotationType, registry);
        }

        return registry;
    }

    @Nonnull
    private <T extends Annotation> ResourceRegistry<T> instantiateRegistry(@Nonnull final Class<T> annotationType) {
        try {
            final ResourceBinding resourceBinding = annotationType.getAnnotation(ResourceBinding.class);

            if (resourceBinding == null) {
                throw new IllegalArgumentException("Cannot instantiate a ResourceRegistry for '" + annotationType + "' because it is not bound to a resource type with @ResourceBinding");
            }

            final Class<? extends ResourceRegistry<?>> registryClass = resourceBinding.registry();

            // This cast is safe as long as the registry class type argument matches the annotation type annotated by
            // resourceBinding.
            @SuppressWarnings("unchecked")
            final ResourceRegistry<T> registry = (ResourceRegistry<T>) registryClass.newInstance();
            return registry;
        } catch (InstantiationException | IllegalAccessException ex) {
            throw Throwables.propagate(ex);
        }
    }

    private <T extends Annotation> void put(@Nonnull final Class<T> annotationType,
            @Nonnull final ResourceRegistry<T> registry) {
        registryMap.put(annotationType, registry);
    }

    private <T extends Annotation> ResourceRegistry<T> get(@Nonnull final Class<T> annotationType) {

        // This cast is safe because {@link #put(Class, ResourceRegistry)} ensures the annotationType type argument
        // matches the registry type argument.
        @SuppressWarnings("unchecked")
        final ResourceRegistry<T> registry = (ResourceRegistry<T>) registryMap.get(annotationType);

        return registry;

    }

    public void merge(@Nonnull final BaseResourceRegistryContainer other) {
        for (final Map.Entry<Class<? extends Annotation>, ResourceRegistry<?>> entry : other.registryMap.entrySet()) {
            mergeHelper(entry.getKey(), other);
        }
    }

    /*
     * Helper for the {@link #merge} method that uses type inference to capture the wildcard annotation type in type
     * variable T for each entry of {@code other}'s registry map.
     */
    private <T extends Annotation> void mergeHelper(final Class<T> annotationType,
            final BaseResourceRegistryContainer other) {
        final ResourceRegistry<T> otherRegistry = other.get(annotationType);
        if (registryMap.containsKey(annotationType)) {
            get(annotationType).merge(otherRegistry);
        } else {
            put(annotationType, otherRegistry);
        }
    }

    public void register(final Annotation a, final Class<?> clas) {

        final Class<? extends Annotation> annotationType = a.annotationType();

        final ResourceRegistry<?> registry = getRegistry(annotationType);

        registry.register(a, clas);
    }
}

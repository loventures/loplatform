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
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.learningobjects.cpxp.component.annotation.RequestMapping;
import com.learningobjects.cpxp.util.collection.BreadthFirstSupertypeIterable;
import com.learningobjects.de.web.Queryable;
import com.learningobjects.de.web.QueryableProperties;
import com.learningobjects.de.web.jackson.JacksonReflectionUtils;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.learningobjects.cpxp.util.ClassUtils.hasAnnotation;

/**
 * Introspects a class for {@link Queryable} properties. The result of introspection is a
 * map of properties that maps their names in the component type to their data model type
 * (the Constants type).
 *
 * @see Queryable
 */
public class QueryableIntrospector extends CacheLoader<Class<?>, Map<String, Queryable>> {

    /**
     * For if you want to use this outside the context of a {@link LoadingCache}
     * (propagates the checked exception)
     */
    public Map<String, Queryable> introspect(@Nonnull final Class<?> clazz) {
        try {
            return load(clazz);
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    @Override
    public Map<String, Queryable> load(@Nonnull final Class<?> clazz) {

        final Map<String, Queryable> queryables = new HashMap<>();

        BreadthFirstSupertypeIterable.from(clazz).filter
                (hasAnnotation(QueryableProperties.class)).forEach(clas -> {

            final QueryableProperties extraQueryables =
                    clas.getAnnotation(QueryableProperties.class);
            for (Queryable queryable : extraQueryables.value()) {
                if (Queryable.USE_JACKSON_NAME.equals(queryable.name())) {
                    throw UnnamedQueryableException.forClass(clas);
                } else {
                    queryables.put(queryable.name(), queryable);
                }
            }
        });

        for (final Method method : clazz.getMethods()) {

            final Queryable queryable = method.getAnnotation(Queryable.class);

            if (queryable != null) {

                final Optional<String> propertyName = getPropertyName(queryable, method);
                if (propertyName.isPresent()) {
                    queryables.put(propertyName.get(), queryable);
                } else {
                    throw UnnamedQueryableException.forMethod(method);
                }
            }
        }

        // support finders
        for (final var field : clazz.getDeclaredFields()) {
            final Queryable queryable = field.getAnnotation(Queryable.class);
            if (queryable != null) {
                queryables.put(field.getName(), queryable);
            }
        }

        // support case classes
        for (final var ctor : clazz.getConstructors()) {
            for (final var param : ctor.getParameters()) {
                final Queryable queryable = param.getAnnotation(Queryable.class);
                if (queryable != null) {
                    queryables.put(param.getName(), queryable);
                }
            }
        }

        return Collections.unmodifiableMap(queryables);
    }

    private Optional<String> getPropertyName(final Queryable queryable,
            final Method method) {
        if (Queryable.USE_JACKSON_NAME.equals(queryable.name())) {
            final Optional<String> propertyName = JacksonReflectionUtils.getPropertyName(method);
            if (!propertyName.isPresent()) {
                // fallback to request mapping path in case this is a join component
                RequestMapping rm = method.getAnnotation(RequestMapping.class);
                if (rm != null) {
                    return Optional.of(rm.path());
                }
            }
            return propertyName;
        } else {
            return Optional.of(queryable.name());
        }
    }

}

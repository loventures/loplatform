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

package com.learningobjects.de.web.jackson;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition;
import com.learningobjects.cpxp.component.web.util.JacksonUtils;
import com.learningobjects.cpxp.util.ClassUtils;
import com.learningobjects.cpxp.util.PropertyDescriptorUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.*;

import static com.learningobjects.cpxp.component.web.util.JacksonUtils.getMapper;

public class JacksonReflectionUtils {

    /**
     * Gets the Jackson property name of the given method. Returns none if method is not a
     * Jackson property. This uses Jackson and the {@link JacksonUtils#getMapper()}
     * serialization configuration under the hood.
     *
     * @param method the method whose Jackson property name is found
     * @return the Jackson property name of the given method
     */
    public static Optional<String> getPropertyName(final Method method) {

        final Optional<BeanPropertyDefinition> prop = getPropertyDefinition(method);

        return prop.map(BeanPropertyDefinition::getName);
    }

    /**
     * Determines if the Jackson property of the given property descriptor is in the given
     * view. Returns false if the given property descriptor is not a Jackson property.
     * This uses Jackson and the {@link JacksonUtils#getMapper()} serialization
     * configuration under the hood.
     *
     * @param propertyDescriptor the property descriptor whose Jackson views are queried
     * @param view the view in which the given method's membership is checked
     * @return true if the given method is a Jackson property in the given view, false
     * otherwise
     */
    public static boolean isInView(
            final PropertyDescriptor propertyDescriptor, final Class<?> view) {

        final Method m = PropertyDescriptorUtils.getMethod(propertyDescriptor);
        return isInView(m, view);
    }

    /**
     * Determines if the Jackson property of the given method is in the given view.
     * Returns false if the given method is not a Jackson property. This uses Jackson and
     * the {@link JacksonUtils#getMapper()} serialization configuration under the hood.
     *
     * @param method the method whose Jackson views are queried
     * @param view the view in which the given method's membership is checked
     * @return true if the given method is a Jackson property in the given view, false
     * otherwise
     */
    public static boolean isInView(final Method method, final Class<?> view) {

        final Optional<BeanPropertyDefinition> prop = getPropertyDefinition(method);

        final List<Class<?>> views;
        if (prop.isPresent()) {
            final Class<?>[] explicitViews = prop.get().findViews();
            if (explicitViews == null) {
                return JacksonUtils.getMapper()
                        .isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION);
            }
            views = Arrays.asList(explicitViews);
        } else {
            views = List.of();
        }

        return views.stream().anyMatch(ClassUtils.isAssignableTo(view));

    }

    private static Optional<BeanPropertyDefinition> getPropertyDefinition(
            final Method method) {

        final Class<?> declaringClass = method.getDeclaringClass();
        final ObjectMapper mapper = JacksonUtils.getMapper();
        final JavaType type = mapper.getTypeFactory().constructType(declaringClass);

        final SerializationConfig cfg = mapper.getSerializationConfig();
        final BasicBeanDescription desc =
                new BasicClassIntrospector().forSerialization(cfg, type, cfg);

        final Map<Method, BeanPropertyDefinition> indexed = indexPropertiesByMethod(desc);

        final BeanPropertyDefinition found = indexed.get(method);
        return Optional.ofNullable(found);
    }

    private static Map<Method, BeanPropertyDefinition> indexPropertiesByMethod(
            final BasicBeanDescription desc) {

        final Map<Method, BeanPropertyDefinition> map = new HashMap<>();

        for (BeanPropertyDefinition prop : desc.findProperties()) {
            if (prop.hasGetter()) {
                final Method method = prop.getGetter().getAnnotated();
                map.put(method, prop);

                /* jackson-module-scala is buggy and will tell us that an explicitly-named
                 * property "foo" on a Java bean has implicit name `getFoo`, so it doesn't
                 * find the non-existent `setGetFoo` method and causes us to fail. Instead
                 * we have to go find the setter, link it to `prop`, and pretend like it's
                 * all okay and we're not using a crap library which introduces awful bugs
                 * with each version and refactors pointlessly rather than fixing them. */
                if (method.getName().startsWith("get") && !prop.hasSetter()) {
                    AnnotatedMethod hackSetter =
                      desc.getClassInfo().findMethod("set" + method.getName().substring(3), new Class<?>[] {method.getReturnType()});
                    if (hackSetter != null) {
                        POJOPropertyBuilder newProp =
                          copyProperty(prop);
                        newProp.addSetter(hackSetter, newProp.getFullName(), newProp.isExplicitlyNamed(), newProp.anyVisible(), newProp.anyIgnorals());
                        prop = newProp;
                    }
                }
            }

            if (prop.hasSetter()) {
                final Method method = prop.getSetter().getAnnotated();
                map.put(method, prop);
            }
        }

        return map;

    }

    private static POJOPropertyBuilder copyProperty(BeanPropertyDefinition prop) {
        if (prop instanceof POJOPropertyBuilder) {
            /* I suspect we always get here but I can't prove it. */
            return ((POJOPropertyBuilder) prop).withName(prop.getFullName());
        } else if (prop instanceof SimpleBeanPropertyDefinition) {
            /* this case seems unlikely. */
            AnnotatedMember mem = prop.getPrimaryMember();
            PropertyName name = prop.getFullName();
            AnnotationIntrospector ai = getMapper().getSerializationConfig().getAnnotationIntrospector();
            boolean
                explicit = prop.isExplicitlyNamed(),
                visible  = mem.isPublic(),
                ignored  = ai.hasIgnoreMarker(mem);
            POJOPropertyBuilder newProp = new POJOPropertyBuilder(
              getMapper().getSerializationConfig(), ai, true, prop.getFullName());
            if (mem instanceof AnnotatedField) {
                newProp.addField((AnnotatedField) mem, name, explicit, visible, ignored);
            } else if (mem instanceof AnnotatedMethod) {
                if (prop.hasGetter()) {
                    newProp.addGetter((AnnotatedMethod) mem, name, explicit, visible, ignored);
                } else {
                    newProp.addSetter((AnnotatedMethod) mem, name, explicit, visible, ignored);
                }
            } else {
                throw new RuntimeException("Don't know what to do with " + mem + " while copying " + prop + "!");
            }
            return newProp;
        } else {
            /* we should never get here. */
            throw new RuntimeException("Don't know how to copy " + prop + "!");
        }
    }
}

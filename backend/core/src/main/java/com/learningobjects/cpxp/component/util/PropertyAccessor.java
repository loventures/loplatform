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

package com.learningobjects.cpxp.component.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.MapMaker;
import com.learningobjects.cpxp.util.StringUtils;
import scala.Tuple2;

import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

abstract class PropertyAccessor {
    public abstract Object get(Object bean, String property) throws
            PropertyAccessorException;

    // Cache the property accessors by class and property. Use weak references to allow
    // class GC.
    private static final ConcurrentMap<Class<?>, Map<String, PropertyAccessor>> __methodMaps = new MapMaker().weakKeys().makeMap();
    private static final ConcurrentMap<Class<?>, PropertyAccessor> __classMap = new MapMaker().weakKeys().makeMap();

    public static PropertyAccessor getInstance(Class<?> clas, String property) throws PropertyAccessorException {
        Map<String, PropertyAccessor> methodMap = __methodMaps.get(clas);
        PropertyAccessor accessor;
        if (methodMap != null) {
            accessor = methodMap.get(property);
        } else {
            accessor = __classMap.get(clas);
        }
        if (accessor == null) {
            // In Java 17 we don't get access to private classes that implement interfaces so we have to instead
            // look to the interface. Case in point is `UnmodifiableEntry` in `Collections`.
            var publicClass = !Modifier.isPublic(clas.getModifiers()) && clas.getSuperclass() == Object.class && clas.getInterfaces().length > 0
              ? clas.getInterfaces()[0] : clas;
            try {
                accessor = new MethodAccessor(publicClass.getMethod("get" + StringUtils.capitalize(property)));
            } catch (Exception e) {
                try {
                    accessor = new MethodAccessor(publicClass.getMethod("is" + StringUtils.capitalize(property)));
                } catch (Exception e2) {
                    try {
                        accessor = new MethodAccessor(publicClass.getMethod(property));
                    } catch (Exception e3) {
                        try {
                            accessor = new FieldAccessor(publicClass.getField(property));
                        } catch (Exception e4) {
                            for (Class cl = clas; cl != null; cl = cl.getSuperclass()) { // TODO: Do this for all accessors?
                                try {
                                    accessor = new FieldAccessor(cl.getDeclaredField("_" + property));
                                } catch (Exception ignored) {
                                }
                            }
                            if (accessor == null) {
                                if (Proxy.isProxyClass(clas)) {
                                    clas = clas.getInterfaces()[0];
                                } else if (Map.class.isAssignableFrom(clas)) {
                                    accessor = new MapAccessor();
                                    __classMap.put(clas, accessor);
                                } else if (scala.collection.Map.class.isAssignableFrom(clas)) {
                                    accessor = new ScalaMapAccessor();
                                    __classMap.put(clas, accessor);
                                } else if (JsonNode.class.isAssignableFrom(clas)) {
                                    accessor = new JsonNodeAccessor();
                                    __classMap.put(clas, accessor);
                                } else if (Tuple2.class.equals(clas)) {
                                    accessor = new TupleAccessor();
                                    __classMap.put(clas, accessor);
                                } else if (clas.isArray()) {
                                    accessor = new ArrayAccessor();
                                    __classMap.put(clas, accessor);
                                } else {
                                    throw new PropertyAccessorException("Class " + clas.getName() + " does not have property: " + property);
                                }
                            }
                        }
                    }
                }
            }
            if (methodMap == null) {
                methodMap = new MapMaker().makeMap();
                __methodMaps.put(clas, methodMap);
            }
            methodMap.put(property, accessor);
        }
        return accessor;
    }
}

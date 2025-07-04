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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;
import com.learningobjects.cpxp.util.StringUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class ConfigUtils {
    public static <T> T decodeConfigurationValue(String configuration, String property, Class<T> type) {
        String[] values = decodeConfiguration(configuration).get(property);
        return type.cast(decodeValues(values, type));
    }

    public static Map<String, String[]> decodeConfiguration(String configuration) {
        // This requires that multiple values for a config field are sequential.
        Map<String, String[]> configMap = new HashMap<String, String[]>();
        String configName = null;
        List<String> configValues = new ArrayList<String>();
        for (String config : StringUtils.defaultString(configuration).split("\n")) {
            int index = config.indexOf('=');
            if (index > 0) {
                String name = config.substring(0, index);
                String value = unescapeConfig(config.substring(1 + index));
                if (!name.equals(configName) && (configName != null)) {
                    configMap.put(configName, configValues.toArray(new String[configValues.size()]));
                    configValues.clear();
                }
                configName = name;
                configValues.add(value);
            }
        }
        if (configName != null) {
            configMap.put(configName, configValues.toArray(new String[configValues.size()]));
        }
        return configMap;
    }

    public static String encodeConfiguration(Map<String, ?> configurationMap) {
        StringBuilder configuration = new StringBuilder();
        for (Map.Entry<String, ?> entry : configurationMap.entrySet()) {
            if (entry.getValue() instanceof Object[]) {
                for (Object value : (Object[]) entry.getValue()) {
                    if (value != null) {
                        append(configuration, entry.getKey(), value.toString());
                    }
                }
            } else if (entry.getValue() != null) {
                append(configuration, entry.getKey(), entry.getValue().toString());
            }
        }
        return configuration.toString();
    }

    public static Collection<Object> getCollection(Class<?> clas) {
        try {
            if (clas == Set.class) {
                return new HashSet<Object>();
            } else if (clas == List.class) {
                return new ArrayList<Object>();
            } else {
                return (Collection<Object>) clas.newInstance();
            }
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    public static Object[] getArray(Class<?> clas, int n) {
        return (Object[]) Array.newInstance(clas, n);
    }

    public static Object decodeValues(String[] values, Type type) {
        try {
            if (type instanceof ParameterizedType) { // List<String> or Set<String>
                ParameterizedType parameterized = (ParameterizedType) type;
                Class<?> clas = (Class<?>) parameterized.getActualTypeArguments()[0];
                Collection<Object> collection = getCollection((Class<?>) parameterized.getRawType());
                if (values != null) {
                    for (String value : values) {
                        collection.add(decodeValue(value, clas));
                    }
                }
                return collection;
            } else {
                Class<?> clas = (Class<?>) type;
                boolean isArray = clas.isArray();
                if (isArray) {
                    clas = clas.getComponentType();
                    int n = (values == null) ? 0 : values.length;
                    Object[] array = getArray(clas, n);
                    for (int i = 0; i < n; ++ i) {
                        array[i] = decodeValue(values[i], clas);
                    }
                    return array;
                } else if ((values != null) && (values.length > 0)) {
                    return decodeValue(values[0], clas);
                } else {
                    return null;
                }
            }
        } catch (Exception ex) {
            throw Throwables.propagate(ex);
        }
    }

    public static Object decodeValue(String value, Class<?> type) {
        if (type == String.class) {
            return value;
        } else if ((type == Boolean.class) || (type == Boolean.TYPE)) {
            return StringUtils.isNotEmpty(value) && !"false".equals(value);
        } else if ((type == Long.class) || (type == Long.TYPE)) {
            try {
                return Long.valueOf(value);
            } catch (Exception ex) {
                return null; // silently discard parse errors.
            }
        } else if ((type == Integer.class) || (type == Integer.TYPE)) {
            try {
                return Integer.valueOf(value);
            } catch (Exception ex) {
                return null; // silently discard parse errors.
            }
        } else if ((type == Double.class) || (type == Double.TYPE)) {
            try {
                return Double.valueOf(value);
            } catch (Exception ex) {
                return null; // silently discard parse errors.
            }
        } else if (Enum.class.isAssignableFrom(type)) {
            try {
                return Enum.valueOf((Class<? extends Enum>) type, value);
            } catch (Exception ex) {
                return null; // silently discard parse errors.
            }
        } else {
            throw new RuntimeException("Unknown type: " + type);
        }
    }

    private static String unescapeConfig(String value) {
        StringBuilder builder = new StringBuilder();
        int o = 0, i;
        while ((i = value.indexOf('\\', o)) >= 0) {
            builder.append(value, o, i);
            builder.append((value.charAt(i + 1)) == 'n' ? '\n' : '\\');
            o = i + 2;
        }
        if (o == 0) {
            return value;
        }
        builder.append(value, o, value.length());
        return builder.toString();
    }

    private static void append(StringBuilder configuration, String name, String value) {
        configuration.append(name).append('=').append(escapeConfig(value)).append('\n');
    }

    private static String escapeConfig(String value) {
        return StringUtils.replace(StringUtils.replace(value, "\\", "\\\\"), "\n", "\\n");
    }

    /**
     * Returns a copy of a document with any unspecified properties copied from a default document.
     * This process is recursive. In order to suppress a property from the default document, the initial
     * document needs to explicitly give it the value null.
     * @param document the initial document
     * @param defaults the default values
     * @return the merged documents
     */
    @Nullable
    public static JsonNode applyDefaults(@Nullable JsonNode document, @Nullable JsonNode defaults) {
        return (document != null) ? merge(document.deepCopy(), defaults) : merge(null, defaults);
    }

    private static JsonNode merge(JsonNode dst, JsonNode src) {
        if (dst == null) {
            return (src == null) ? null : src.deepCopy();
        } else {
            if ((src != null) && src.isObject() && dst.isObject()) {
                src.fields().forEachRemaining(entry ->
                  ((ObjectNode) dst).set(entry.getKey(), merge(dst.get(entry.getKey()), entry.getValue())));
            }
            return dst;
        }
    }
}

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

package com.learningobjects.de.web.util;

import com.fasterxml.jackson.databind.JavaType;
import com.learningobjects.cpxp.component.RestfulComponent;
import com.learningobjects.cpxp.component.annotation.Schema;
import com.learningobjects.cpxp.util.lang.OptionLike;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utility methods about the DE Web API model.
 */
public final class ModelUtils {

    private static final Logger logger = Logger.getLogger(ModelUtils.class.getName());

    /**
     * A prefix put on a schema type name when the type is used in a collection
     */
    private static final String COLLECTION_SCHEMA_TYPE_PREFIX = "collection-of-";

    private ModelUtils() {
    }

    public static String getSchemaName(final Type type, final Map<TypeVariable<?>, Type> typeArguments) {

        final String schemaName;

        if (type instanceof Class) {

            final Class<?> clazz = (Class<?>) type;
            final Schema schema = clazz.getAnnotation(Schema.class);
            if (schema != null) {
                schemaName = schema.value();
            } else {
                handleUnsupportedModelType("Unsupported model type: " + type + ". Missing @Schema annotation");
                schemaName = null;
            }

        } else if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;

            // generic types generally are not supported, only these three are:
            final Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            final String rawSchemaName = getSchemaName(rawType, typeArguments);
            if (rawSchemaName != null) {
                schemaName = rawSchemaName;
            } else if (OptionLike.isOptionLike(rawType) || RestfulComponent.class.isAssignableFrom(rawType)) {
                schemaName = getSchemaName(parameterizedType.getActualTypeArguments()[0], typeArguments);
            } else if (Collection.class.isAssignableFrom(rawType)) {

                final String argumentSchemaName = getSchemaName(parameterizedType.getActualTypeArguments()[0], typeArguments);
                schemaName = argumentSchemaName == null ? null : COLLECTION_SCHEMA_TYPE_PREFIX + argumentSchemaName;
            } else {
                handleUnsupportedModelType("Unsupported model type: " + type + ".");
                schemaName = null;
            }

        } else if (type instanceof TypeVariable) {
            final TypeVariable<?> typeVariable = (TypeVariable<?>) type;
            Type[] bounds = typeVariable.getBounds();
            final Type typeArg = typeArguments.get(typeVariable);
            if (typeArg != null) {
                schemaName = getSchemaName(typeArg, typeArguments);
            } else if (bounds.length > 1) {
                handleUnsupportedModelType("Unsupported model type: " + type + ". Multiple bounds are not supported.");
                schemaName = null;
            } else {
                Type bound = bounds[0];
                if ((bound instanceof ParameterizedType) && type.equals(((ParameterizedType) bound).getActualTypeArguments()[0])) {
                    handleUnsupportedModelType("Undefined type variable: " + type);
                    schemaName = null;
                } else {
                    // the first bound is java.lang.Object if the variable is unbounded
                    schemaName = getSchemaName(bounds[0], typeArguments);
                }
            }

        } else if (type instanceof WildcardType) {

            final WildcardType wildcardType = (WildcardType) type;
            final Type[] lowerBounds = wildcardType.getLowerBounds();
            final Type[] upperBounds = wildcardType.getUpperBounds();
            if (lowerBounds.length != 0) {
                handleUnsupportedModelType("Unsupported model type: " + type + ". Lower bounds are not supported");
                schemaName = null;
            } else if (upperBounds.length > 1) {
                handleUnsupportedModelType("Unsupported model type: " + type + ". Multiple bounds are not supported.");
                schemaName = null;
            } else {
                // the first bound is java.lang.Object if the wildcard is unbounded
                schemaName = getSchemaName(upperBounds[0], typeArguments);
            }

        } else {
            handleUnsupportedModelType("Unsupported model type: " + type);
            schemaName = null;
        }

        return schemaName;
    }

    /**
     * Gets the most contained type argument of the given type. The most contained type argument is the first type
     * argument of the given type that is not a container type itself. Examples are the best way to explain this:
     * <pre>{@code
     *     getMostContainedType(Foo)                  Foo
     *     getMostContainedType(List<Foo>)            Foo
     *     getMostContainedType(List<List<Foo>>>)     Foo
     *     getMostContainedType(List<Option<Foo>>     Foo
     *     getMostContainedType(Map<Foo, Bar>)        Map<Foo, Bar>
     *     getMostContainedType(Comparable<Foo>)      Comparable<Foo>
     * }
     * </pre>
     *
     * @param type the type to find the most contained type argument for
     * @return the most contained type argument of the given type
     */
    public static JavaType getMostContainedType(final JavaType type) {
        if (type.isCollectionLikeType() || OptionLike.isOptionLike(type.getRawClass())) {
            return getMostContainedType(type.containedType(0));
        } else {
            return type;
        }
    }


    private static void handleUnsupportedModelType(final String msg) { logger.fine(msg); }
}

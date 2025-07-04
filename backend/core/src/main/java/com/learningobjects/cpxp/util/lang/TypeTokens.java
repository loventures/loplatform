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

package com.learningobjects.cpxp.util.lang;

import com.google.common.base.Throwables;
import com.google.common.reflect.TypeToken;
import scala.collection.Seq;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class TypeTokens {

    public static Optional<TypeToken<?>> resolveParametricType1(TypeToken<?> type, Class<?> rawType) {
        return resolveParametricTypeN(type, rawType, 0);
    }

    public static Optional<TypeToken<?>> resolveParametricType2(TypeToken<?> type, Class<?> rawType) {
        return resolveParametricTypeN(type, rawType, 1);
    }

    public static Optional<TypeToken<?>> resolveParametricTypeN(TypeToken<?> type, Class<?> rawType, int parametricTypeIndex) {
        checkNotNull(type);
        checkNotNull(rawType);
        checkArgument(parametricTypeIndex >= 0, "index cannot be negative");

        if (rawType.getTypeParameters().length >= parametricTypeIndex + 1) {
            final TypeToken<?> resolveType = type.resolveType(rawType.getTypeParameters()[parametricTypeIndex]);
            checkNotNull(resolveType);
            return Optional.of(resolveType);
        } else {
            return Optional.empty();
        }
    }

    public static Optional<TypeToken<?>> resolveIterableElementType(@Nullable final TypeToken<?> iterableType, final TypeToken declaringType) {

        if (iterableType != null && Iterable.class.isAssignableFrom(iterableType.getRawType())) {
            try {
                final TypeToken<?> iteratorType = iterableType.resolveType(Iterable.class.getMethod("iterator")
                        .getGenericReturnType());
                final TypeToken<?> elementType = iteratorType.resolveType(Iterator.class.getMethod
                        ("next").getGenericReturnType());
                final TypeToken<?> resolvedType = declaringType.resolveType(elementType.getType());
                return Optional.of(resolvedType);
            } catch (NoSuchMethodException ex) {
                /*
                 * can only happen if #getMethod above is called with bad args (because iterableType's type is checked)
                 */
                throw Throwables.propagate(ex);
            }
        }
        return Optional.empty();

    }

    public static Optional<TypeToken<?>> resolveSeqElementType(@Nullable final TypeToken<?> seqType, final TypeToken declaringType) {

        if (seqType != null && Seq.class.isAssignableFrom(seqType.getRawType())) {
            try {
                final TypeToken<?> outerType =
                  seqType.resolveType(Seq.class.getMethod("seq").getGenericReturnType());

                final TypeToken<?> elementType =
                  outerType.resolveType(Seq.class.getMethod("head").getGenericReturnType());

                final TypeToken<?> resolvedType = declaringType.resolveType(elementType.getType());
                return Optional.of(resolvedType);
            } catch (NoSuchMethodException ex) {
                throw Throwables.propagate(ex);
            }
        }

        return Optional.empty();
    }

    public static Optional<TypeToken<?>> resolveMapValueType(@Nullable final TypeToken<?> mapType) {

        if (mapType != null && Map.class.isAssignableFrom(mapType.getRawType())) {
            try {
                final TypeToken<?> valueType = mapType.resolveType(Map.class.getMethod("get", Object.class)
                        .getGenericReturnType());
                return Optional.of(valueType);
            } catch (NoSuchMethodException ex) {
                /*
                 * can only happen if #getMethod above is called with bad args (because mapType's type is checked)
                 */
                throw Throwables.propagate(ex);
            }
        }
        return Optional.empty();

    }

    public static Optional<TypeToken<?>> resolveOptionLikeElementType(@Nullable final TypeToken<?> optionalType) {

        if (optionalType != null && OptionLike.isOptionLike(optionalType.getRawType())) {
            try {
                final TypeToken<?> elementType = optionalType.resolveType(optionalType.getRawType().getMethod("get")
                        .getGenericReturnType());
                return Optional.of(elementType);
            } catch (NoSuchMethodException ex) {
                /*
                 * can only happen if #getMethod above is called with bad args (because optionalType's type is checked)
                 */
                throw Throwables.propagate(ex);
            }
        }
        return Optional.empty();

    }

}

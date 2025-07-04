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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Provides support for the different option-like classes, {@link scala.Option}
 * and {@link java.util.Optional}.
 */
public class OptionLike {
    /**
     * Return whether a particular class is an option-like class.
     *
     * @param clas the class in question
     * @return whether the class is an option-like class.
     */
    public static boolean isOptionLike(@Nonnull Class<?> clas) {
        return scala.Option.class.isAssignableFrom(clas) ||
          java.util.Optional.class.isAssignableFrom(clas);
    }

    /**
     * Return an option-wrapped nullable value.
     *
     * @param optionClass the option-like class
     * @param value the nullable value
     * @return the option-wrapped value
     */
    @Nonnull
    public static <S, T> S ofNullable(@Nonnull Class<S> optionClass, @Nullable T value) {
        // How can I declare that S is parameterized and return S<T>?
        if (scala.Option.class.equals(optionClass)) {
            return optionClass.cast(scala.Option.apply(value));
        } else if (java.util.Optional.class.equals(optionClass)) {
            return optionClass.cast(java.util.Optional.ofNullable(value));
        } else {
            throw new IllegalArgumentException("Not an option-like class: " + optionClass);
        }
    }

    /**
     * Get the empty value of the provided option-like type.
     *
     * @param optionClass the class of the option-like type
     * @param <S> the option-like type
     * @return the empty value of the option-like type
     */
    @Nonnull
    public static <S> S empty(@Nonnull Class<S> optionClass) {
        return ofNullable(optionClass, null);
    }

    /**
     * Extract a nullable value from an option-wrapped value.
     *
     * @param value the option-wrapped value
     * @return the nullable value
     */
    @Nullable
    public static Object getOrNull(@Nonnull Object value) {
        if (scala.Option.class.isInstance(value)) {
            scala.Option scopt = (scala.Option) value;
            return scopt.isEmpty() ? null : scopt.get();
        } else if (java.util.Optional.class.isInstance(value)) {
            return ((java.util.Optional<?>) value).orElse(null);
        } else {
            throw new IllegalArgumentException("Not an option-like value: " + value);
        }
    }
}

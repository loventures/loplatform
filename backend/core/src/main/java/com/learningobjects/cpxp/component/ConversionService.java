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

package com.learningobjects.cpxp.component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.reflect.TypeToken;
import com.learningobjects.cpxp.component.annotation.StringConvert;
import com.learningobjects.cpxp.component.web.converter.StringConverter;
import com.learningobjects.cpxp.component.web.converter.StringConverterComponent;
import scala.Option;
import scala.Some;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * TODO: I wish this did not have a static instance...
 */
public final class ConversionService {

    private static final long CACHE_TIMEOUT_MINUTES = 30;

    private static final class Lazy {
        private static final ConversionService INSTANCE = new ConversionService();
    }

    public static final ConversionService getInstance() {
        return Lazy.INSTANCE;
    }

    private final Cache<TypeToken<?>, Optional<StringConverter<?>>> cache;
    private final Map<TypeToken<?>, StringConverter<?>> internal;

    private ConversionService() {
        cache = CacheBuilder.newBuilder().expireAfterAccess(CACHE_TIMEOUT_MINUTES, TimeUnit.MINUTES).build();

        internal = Map.of(
          TypeToken.of(String.class), input -> Option.apply(input.value()),
          TypeToken.of(Long.class), primitiveConverter(Long::valueOf),
          TypeToken.of(Integer.class), primitiveConverter(Integer::valueOf),
          TypeToken.of(Boolean.class), primitiveConverter(Boolean::valueOf),
          TypeToken.of(Long.TYPE), primitiveConverter(Long::valueOf),
          TypeToken.of(Integer.TYPE), primitiveConverter(Integer::valueOf),
          TypeToken.of(Boolean.TYPE), primitiveConverter(Boolean::valueOf)
        );
    }

    private static <T> StringConverter<T> primitiveConverter(Function<String, T> valueOf) {
        return input -> {
            try {
                return Some.apply(valueOf.apply(input.value()));
            } catch (NumberFormatException e) {
                return scala.Option.empty();
            }
        };
    }

    // TODO: I wish this could return StringConverter of "T" instead of "?"!!!!!!!! How could Cache guarentee it??
    public <T> Optional<StringConverter<?>> get(final TypeToken<T> typeToken) {

        class ConverterLoader implements Callable<Optional<StringConverter<?>>> {
            @Override
            public Optional<StringConverter<?>> call() {
                // bind custom to internal

                // this is tedious without real binding...thanks guava ;_;
                Optional<StringConverter<?>> custom = getCustom(typeToken);

                // no orElse(Supplier<Optional<...>>) here...
                if (custom.isPresent()) {
                    return custom;
                } else {
                    return getInternal(typeToken);
                }
            }
        }

        try {
            return cache.get(typeToken, new ConverterLoader());
        } catch (ExecutionException e) {
            return Optional.empty();
        }
    }

    private Optional<StringConverter<?>> getInternal(TypeToken<?> typeToken) {

        for (TypeToken<?> key : internal.keySet()) {
            if (key.isSupertypeOf(typeToken)) {
                return Optional.ofNullable(internal.get(key));
            }
        }

        return Optional.empty();
    }

    private Optional<StringConverter<?>> getCustom(TypeToken<?> typeToken) {

        if (typeToken.getRawType().isAnnotationPresent(StringConvert.class)) {
            Class<? extends StringConverter<?>> using =
              typeToken.getRawType().getAnnotation(StringConvert.class).using();
            if (resolve(TypeToken.of(using)).isSupertypeOf(typeToken)) {
                // dependency injection probably pointless here
                return Optional.of(ComponentSupport.newInstance(using));
            }
        }

        for (StringConverterComponent<?> converter : ComponentSupport.getComponents(StringConverterComponent.class)) {
            if (resolve(TypeToken.of(converter.getClass())).isSupertypeOf(typeToken)) {
                return Optional.of(converter);
            }
        }

        return Optional.empty();
    }

    @SuppressWarnings("rawtypes")
    private TypeToken<?> resolve(TypeToken<? extends StringConverter> converterType) {
        return converterType.resolveType(StringConverter.class.getTypeParameters()[0]);
    }
}
